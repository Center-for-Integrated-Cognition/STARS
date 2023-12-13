#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/time.h>
#include <assert.h>

#define GL_GLEXT_PROTOTYPES

#include <GL/gl.h>
#include <GL/glx.h>

#include "glcontext.h"
#include "april_vis_GL.h"
#include "lphash.h"
#include "parray.h"

static glcontext_t *glc;

#define MAX_FBOS 128

static gl_fbo_t *fbos[MAX_FBOS];

static lphash_t *vbo_info_map, *texture_info_map;
static parray_t *vbo_info_array, *texture_info_array;
static int64_t current_canvas_id;

typedef struct vbo_info vbo_info_t;
struct vbo_info
{
    int64_t id; // java's id

    GLuint vbo_id;

    int64_t canvas_id; // which canvas was this object most recently drawn for?
    int32_t size; // size of allocated memory in bytes

    uint32_t frames_since_referenced;
};

typedef struct texture_info texture_info_t;
struct texture_info
{
    int64_t id; // java's id

    GLuint  texture_id;

    int64_t canvas_id; // which canvas was this object most recently drawn for?
    int32_t size; // size of allocated memory in bytes

    uint32_t frames_since_referenced;
};

/*
static int64_t timestamp_now()
{
    struct timeval tv;
    gettimeofday (&tv, NULL);
    return (int64_t) tv.tv_sec * 1000000 + tv.tv_usec;
}
*/

JNIEXPORT jint JNICALL Java_april_vis_GL_gl_1initialize
  (JNIEnv *jenv, jclass jcls)
{
    glc = glcontext_X11_create();

    return 0;
}

JNIEXPORT jint JNICALL Java_april_vis_GL_gl_1fbo_1create
  (JNIEnv *jenv, jclass jcls, jint width, jint height)
{
    gl_fbo_t *fbo = gl_fbo_create(glc, width, height);

    if (fbo == NULL)
        return -1;

    // skip id 0...
    for (int i = 1; i < MAX_FBOS; i++) {
        if (fbos[i] == NULL) {
            fbos[i] = fbo;
            return i;
        }
    }

    return -2;
}

JNIEXPORT jint JNICALL Java_april_vis_GL_gl_1fbo_1bind
  (JNIEnv *jenv, jclass jcls, jint fboid)
{
    gl_fbo_t *fbo = fbos[fboid];

    gl_fbo_bind(fbo);

    return 0;
}

JNIEXPORT jint JNICALL Java_april_vis_GL_gl_1fbo_1destroy
  (JNIEnv *jenv, jclass jcls, jint fboid)
{
    gl_fbo_destroy(fbos[fboid]);
    fbos[fboid] = 0;

    return 0;
}

/*
JNIEXPORT jintArray JNICALL Java_april_vis_GL_gl_1read_1pixels
  (JNIEnv *jenv, jclass jcls, jint width, jint height)
{
    int sz = width*height;

    jintArray jdata = (*jenv)->NewIntArray(jenv, sz);

    int32_t *tmp = malloc(sz * sizeof(int32_t));

//    glFlush();
    printf("*1*\n");
    glReadPixels(0, 0, width, height, GL_RGB, GL_UNSIGNED_INT_8_8_8_8, tmp);

    (*jenv)->SetIntArrayRegion(jenv, jdata, 0, sz, (jint*) tmp);

    free(tmp);

    return jdata;
}
*/
static int warned_is_copy = 0;

JNIEXPORT jint JNICALL Java_april_vis_GL_gl_1read_1pixels2__II_3I
  (JNIEnv *jenv, jclass jcls, jint width, jint height, jintArray jdata)
{
    glFlush();

    jboolean is_copy = 0;
    jint *data = (*jenv)->GetPrimitiveArrayCritical(jenv, jdata, &is_copy);

    if (is_copy && !warned_is_copy) {
        warned_is_copy = 1;
        printf("gl_jni.c: got a copy when accessing image buffer; probably slower.\n");
    }

    printf("*2*\n");
    glPixelStorei(GL_PACK_ALIGNMENT, 1);
//    glReadPixels(0, 0, width, height, GL_BGR, GL_UNSIGNED_INT_8_8_8_8_REV, data);
    glReadPixels(0, 0, width, height, GL_BGR, GL_UNSIGNED_BYTE, data);

    (*jenv)->ReleasePrimitiveArrayCritical(jenv, jdata, data, 0);
    return 0;
}

JNIEXPORT jint JNICALL Java_april_vis_GL_gl_1read_1pixels2__II_3B
  (JNIEnv *jenv, jclass jcls, jint width, jint height, jbyteArray jdata)
{
    glFlush();

    jboolean is_copy = 0;
    jbyte *data = (*jenv)->GetPrimitiveArrayCritical(jenv, jdata, &is_copy);

    if (is_copy && !warned_is_copy) {
        warned_is_copy = 1;
        printf("gl_jni.c: got a copy when accessing image buffer; probably slower.\n");
    }

//    glReadPixels(0, 0, width, height, GL_BGR, GL_UNSIGNED_INT_8_8_8_8_REV, data);
    glPixelStorei(GL_PACK_ALIGNMENT, 1);
    glReadPixels(0, 0, width, height, GL_BGR, GL_UNSIGNED_BYTE, data);

    (*jenv)->ReleasePrimitiveArrayCritical(jenv, jdata, data, 0);
    return 0;
}

JNIEXPORT jint JNICALL Java_april_vis_GL_gl_1ops
  (JNIEnv *jenv, jclass jcls, jdoubleArray darray, jint dlen)
{
    double *buf = (double*) malloc(sizeof(double) * dlen);

    (*jenv)->GetDoubleArrayRegion(jenv, darray, 0, dlen, buf);

    int pos = 0;

    while (pos < dlen) {
        int cmd = (int) buf[pos];
        pos++;

        switch(cmd)
        {
        case april_vis_GL_OP_TRANSLATED:
            glTranslated(buf[pos], buf[pos+1], buf[pos+2]);
            pos+=3;
            break;
        case april_vis_GL_OP_PUSHMATRIX:
            glPushMatrix();
            break;
        case april_vis_GL_OP_POPMATRIX:
            glPopMatrix();
            break;
        case april_vis_GL_OP_MULTMATRIXD:
            glMultMatrixd(&buf[pos]);
            pos+=16;
            break;
        case april_vis_GL_OP_LOADIDENTITY:
            glLoadIdentity();
            break;
        case april_vis_GL_OP_MATRIXMODE:
            glMatrixMode((int) buf[pos]);
            pos++;
            break;
        case april_vis_GL_OP_ENABLE:
            glEnable((int) buf[pos]);
            pos++;
            break;
        case april_vis_GL_OP_DISABLE:
            glDisable((int) buf[pos]);
            pos++;
            break;
        case april_vis_GL_OP_SCALED:
            glScaled(buf[pos], buf[pos+1], buf[pos+2]);
            pos+=3;
            break;
        case april_vis_GL_OP_CLEAR:
            glClear(buf[pos]);
            pos++;
            break;
        case april_vis_GL_OP_COLOR:
        {
            uint32_t rgba = buf[pos];
            pos++;

            glColor4ub((rgba >> 24) & 0xff,
                       (rgba >> 16) & 0xff,
                       (rgba >> 8) & 0xff,
                       rgba & 0xff);
            break;
        }
        case april_vis_GL_OP_CLEARCOLOR:
            glClearColor(buf[pos], buf[pos+1], buf[pos+2], buf[pos+3]);
            pos+=4;
            break;
        case april_vis_GL_OP_CLEARDEPTH:
            glClearDepth(buf[pos]);
            pos++;
            break;
        case april_vis_GL_OP_SCISSOR:
            glScissor((int) buf[pos], (int) buf[pos+1], (int) buf[pos+2], (int) buf[pos+3]);
            pos += 4;
            break;
        case april_vis_GL_OP_LIGHTFV:
        {
            GLenum light = (int) buf[pos];
            GLenum pname = (int) buf[pos+1];
            int flen = (int) buf[pos+2];
            float params[flen];
            for (int i = 0; i < flen; i++)
                params[i] = buf[pos+3+i];
            glLightfv(light, pname, params);
            pos += 3+flen;
            break;
        }
        case april_vis_GL_OP_LIGHTMODELI:
            glLightModeli((int) buf[pos], (int) buf[pos+1]);
            pos += 2;
            break;
        case april_vis_GL_OP_COLORMATERIAL:
            glColorMaterial((int) buf[pos], (int) buf[pos+1]);
            pos += 2;
            break;
        case april_vis_GL_OP_MATERIALF:
            glMaterialf((int) buf[pos], (int) buf[pos+1], (float) buf[pos+2]);
            pos += 3;
            break;
        case april_vis_GL_OP_MATERIALFV:
        {
            GLenum face = (int) buf[pos];
            GLenum pname = (int) buf[pos+1];
            int flen = (int) buf[pos+2];
            float params[flen];
            for (int i = 0; i < flen; i++)
                params[i] = buf[pos+3+i];
            glMaterialfv(face, pname, params);
            pos += 3+flen;
            break;
        }
        case april_vis_GL_OP_DEPTHFUNC:
            glDepthFunc((int) buf[pos]);
            pos++;
            break;
        case april_vis_GL_OP_BLENDFUNC:
            glBlendFunc((int) buf[pos], (int) buf[pos+1]);
            pos += 2;
            break;
        case april_vis_GL_OP_POLYGONMODE:
            glPolygonMode((int) buf[pos], (int) buf[pos+1]);
            pos += 2;
            break;
        case april_vis_GL_OP_HINT:
            glHint((int) buf[pos], (int) buf[pos+1]);
            pos += 2;
            break;
        case april_vis_GL_OP_SHADEMODEL:
            glShadeModel((int) buf[pos]);
            pos += 1;
            break;
        case april_vis_GL_OP_BEGIN:
            glBegin((int) buf[pos]);
            pos += 1;
            break;
        case april_vis_GL_OP_END:
            glEnd();
            break;
        case april_vis_GL_OP_VERTEX2D:
            glVertex2d(buf[pos], buf[pos+1]);
            pos += 2;
            break;
        case april_vis_GL_OP_VERTEX3D:
            glVertex3d(buf[pos], buf[pos+1], buf[pos+2]);
            pos += 3;
            break;
        case april_vis_GL_OP_VIEWPORT:
            glViewport((int) buf[pos], (int) buf[pos+1], (int) buf[pos+2], (int) buf[pos+3]);
            pos += 4;
            break;
        case april_vis_GL_OP_DRAWARRAYS:
            glDrawArrays((int) buf[pos], (int) buf[pos+1], (int) buf[pos+2]);
            pos += 3;
            break;
        case april_vis_GL_OP_POLYGONOFFSET:
            glPolygonOffset((float) buf[pos], (float) buf[pos+1]);
            pos += 2;
            break;
        case april_vis_GL_OP_LINEWIDTH:
            glLineWidth((float) buf[pos]);
            pos ++;
            break;
        case april_vis_GL_OP_PUSHATTRIB:
            glPushAttrib((GLbitfield) buf[pos]);
            pos ++;
            break;
        case april_vis_GL_OP_POPATTRIB:
            glPopAttrib();
            break;
        case april_vis_GL_OP_POINTSIZE:
            glPointSize((float) buf[pos]);
            pos++;
            break;
        case april_vis_GL_OP_TEXCOORD2D:
            glTexCoord2d(buf[pos], buf[pos+1]);
            pos+=2;
            break;
        case april_vis_GL_OP_NORMAL3F:
            glNormal3f((float) buf[pos], (float) buf[pos+1], (float) buf[pos+2]);
            pos+=3;
            break;
        case april_vis_GL_OP_ALPHA_FUNC:
            glAlphaFunc((GLenum) buf[pos], (GLclampf) buf[pos+1]);
            pos+=2;
            break;
        case april_vis_GL_OP_ROTATED:
            glRotated(buf[pos], buf[pos+1], buf[pos+2], buf[pos+3]);
            pos+=4;
            break;
        case april_vis_GL_OP_DRAWRANGEELEMENTS:
            glDrawRangeElements((int) buf[pos], (int) buf[pos+1], (int) buf[pos+2], (int) buf[pos+3], GL_UNSIGNED_INT, (GLvoid*) ((int) buf[pos+4]));
            pos += 5;
            break;
        default:
            printf("Warning: unknown opcode %d (0x%04x)... giving up.\n", cmd, cmd);
            pos = dlen;
            break;
        }
    }

    free(buf);
    return 0;
}

static void gldata_check_initialized()
{
    if (vbo_info_map == NULL) {
        vbo_info_map = lphash_create();
        vbo_info_array = parray_create();
        texture_info_map = lphash_create();
        texture_info_array = parray_create();
    }
}

JNIEXPORT jint JNICALL Java_april_vis_GL_gldata_1begin_1frame
  (JNIEnv *jenv, jclass jcls, jlong canvasid)
{
    gldata_check_initialized();

    current_canvas_id = canvasid;
    return 0;
}

JNIEXPORT jint JNICALL Java_april_vis_GL_gldata_1end_1frame_1and_1collect
(JNIEnv *jenv, jclass jcls, jlong canvasid, jint memsize)
{
    gldata_check_initialized();

    // scan the objects that belong to this canvasid.
    for (int i = 0; i < parray_size(vbo_info_array); i++) {
        vbo_info_t *vbo_info = parray_get(vbo_info_array, i);

        if (vbo_info->canvas_id != canvasid)
            continue;

        vbo_info->frames_since_referenced ++;

        if (vbo_info->frames_since_referenced == 2) {
            glDeleteBuffers(1, &vbo_info->vbo_id);
            lphash_remove(vbo_info_map, vbo_info->id);
            parray_remove_shuffle(vbo_info_array, i);
            free(vbo_info);
        }
    }

    for (int i = 0; i < parray_size(texture_info_array); i++) {
        texture_info_t *texture_info = parray_get(texture_info_array, i);

        if (texture_info->canvas_id != canvasid)
            continue;

        texture_info->frames_since_referenced ++;

        if (texture_info->frames_since_referenced == 2) {
            glDeleteTextures(1, &texture_info->texture_id);
            lphash_remove(texture_info_map, texture_info->id);
            parray_remove_shuffle(texture_info_array, i);
            free(texture_info);
        }
    }

    return 0;
}

static int gldata_bind(JNIEnv *jenv, jclass jcls, jint vbo_type, jlong id, jint nverts, jint vertdim, jarray jdata, int gltype)
{
    GLuint vbo_id;

    gldata_check_initialized();

    int gltype_size;

    switch (gltype)
    {
    case GL_SHORT:
        gltype_size = 2;
        break;
    case GL_INT:
    case GL_FLOAT:
        gltype_size = 4;
        break;
    case GL_DOUBLE:
        gltype_size = 8;
        break;
    default:
        assert(0);
        break;
    }

    GLenum target = GL_ARRAY_BUFFER;
    if (vbo_type == april_vis_GL_VBO_TYPE_ELEMENT_ARRAY)
        target = GL_ELEMENT_ARRAY_BUFFER;

    vbo_info_t *vbo_info = lphash_get(vbo_info_map, id);

    if (vbo_info == NULL) {
        glGenBuffers(1, &vbo_id);

        glBindBuffer(target, vbo_id);
        float *data = (*jenv)->GetPrimitiveArrayCritical(jenv, jdata, 0);
        uint32_t sz = vertdim*gltype_size*nverts;

        glBufferData(target, sz, data, GL_STATIC_DRAW);
        (*jenv)->ReleasePrimitiveArrayCritical(jenv, jdata, data, 0);

        vbo_info = (vbo_info_t*) calloc(1, sizeof(vbo_info_t));
        vbo_info->id = id;
        vbo_info->vbo_id = vbo_id;
        vbo_info->size = sz;

        lphash_put(vbo_info_map, id, vbo_info);
        parray_add(vbo_info_array, vbo_info);
    } else {
        glBindBuffer(target, vbo_info->vbo_id);
    }

    switch (vbo_type)
    {
    case april_vis_GL_VBO_TYPE_VERTEX:
        glEnableClientState(GL_VERTEX_ARRAY);
        glVertexPointer(vertdim, gltype, vertdim*gltype_size, 0);
        break;
    case april_vis_GL_VBO_TYPE_NORMAL:
        glEnableClientState(GL_NORMAL_ARRAY);
        assert(vertdim == 3);
        glNormalPointer(gltype, vertdim*gltype_size, 0);
        break;
    case april_vis_GL_VBO_TYPE_COLOR:
        gltype = GL_UNSIGNED_BYTE;
        gltype_size = 1;
        glEnableClientState(GL_COLOR_ARRAY);
        glColorPointer(vertdim, gltype, vertdim*gltype_size, 0);
        break;
    case april_vis_GL_VBO_TYPE_TEX_COORD:
        glEnableClientState(GL_TEXTURE_COORD_ARRAY);
        glTexCoordPointer(vertdim, gltype, vertdim*gltype_size, 0);
        break;
    case april_vis_GL_VBO_TYPE_ELEMENT_ARRAY:
        // nothing to do for element arrays; the bind is enough.
        assert(vertdim == 1);
        break;
    }

    vbo_info->frames_since_referenced = 0;
    vbo_info->canvas_id = current_canvas_id;

    return 0;
}

JNIEXPORT jint JNICALL Java_april_vis_GL_gldata_1unbind
  (JNIEnv *jenv, jclass jcls, jint vbo_type, jlong id)
{
    switch (vbo_type)
    {
    case april_vis_GL_VBO_TYPE_VERTEX:
        glDisableClientState(GL_VERTEX_ARRAY);
        break;
    case april_vis_GL_VBO_TYPE_NORMAL:
        glDisableClientState(GL_NORMAL_ARRAY);
        break;
    case april_vis_GL_VBO_TYPE_COLOR:
        glDisableClientState(GL_COLOR_ARRAY);
        break;
    case april_vis_GL_VBO_TYPE_TEX_COORD:
        glDisableClientState(GL_TEXTURE_COORD_ARRAY);
        break;
    case april_vis_GL_VBO_TYPE_ELEMENT_ARRAY:
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0);
        break;
    }

    return 0;
}

JNIEXPORT jint JNICALL Java_april_vis_GL_gldata_1bind__IJII_3F
  (JNIEnv *jenv, jclass jcls, jint vbo_type, jlong id, jint nverts, jint vertdim, jfloatArray jdata)
{
    return gldata_bind(jenv, jcls, vbo_type, id, nverts, vertdim, jdata, GL_FLOAT);
}

JNIEXPORT jint JNICALL Java_april_vis_GL_gldata_1bind__IJII_3D
  (JNIEnv *jenv, jclass jcls, jint vbo_type, jlong id, jint nverts, jint vertdim, jdoubleArray jdata)
{
    return gldata_bind(jenv, jcls, vbo_type, id, nverts, vertdim, jdata, GL_DOUBLE);
}

JNIEXPORT jint JNICALL Java_april_vis_GL_gldata_1bind__IJII_3I
  (JNIEnv *jenv, jclass jcls, jint vbo_type, jlong id, jint nverts, jint vertdim, jintArray jdata)
{
    return gldata_bind(jenv, jcls, vbo_type, id, nverts, vertdim, jdata, GL_INT);
}

JNIEXPORT jint JNICALL Java_april_vis_GL_gldata_1bind__IJII_3S
  (JNIEnv *jenv, jclass jcls, jint vbo_type, jlong id, jint nverts, jint vertdim, jshortArray jdata)
{
    return gldata_bind(jenv, jcls, vbo_type, id, nverts, vertdim, jdata, GL_SHORT);
}

////////////////////////////////////////////////////////////////////

static int gldata_tex_bind(JNIEnv *jenv, jclass jcls, jlong id, jint internalfmt, jint width, jint height,
                           jint fmt, jint type, jarray jdata, int bytes_per_element, int flags)
{
    GLuint texture_id;

    gldata_check_initialized();

    texture_info_t *texture_info = lphash_get(texture_info_map, id);

    glEnable(GL_TEXTURE_2D);

    if (texture_info == NULL) {
        int min_filter = (flags & april_vis_GL_TEX_FLAG_MIN_FILTER) ? 1 : 0;
        int mag_filter = (flags & april_vis_GL_TEX_FLAG_MAG_FILTER) ? 1 : 0; // aka mipmap
        int repeat     = (flags & april_vis_GL_TEX_FLAG_REPEAT) ? 1 : 0;     // or clamp?

        glGenTextures(1, &texture_id);
        glBindTexture(GL_TEXTURE_2D, texture_id);

        int *data = (*jenv)->GetPrimitiveArrayCritical(jenv, jdata, 0);
        uint32_t sz = bytes_per_element*(*jenv)->GetArrayLength(jenv, jdata);

        if (repeat) {
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);
        } else {
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP);
        }

        if (min_filter)
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        else
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);

        if (mag_filter) {
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR_MIPMAP_LINEAR);
//        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_LOD, 0);
//        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAX_LOD, 10);
        } else {
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        }

        glTexImage2D(GL_TEXTURE_2D, 0, internalfmt, width, height, 0, fmt, type, data);
        (*jenv)->ReleasePrimitiveArrayCritical(jenv, jdata, data, 0);

        texture_info = (texture_info_t*) calloc(1, sizeof(texture_info_t));
        texture_info->id = id;
        texture_info->texture_id = texture_id;
        texture_info->size = sz;

        if (mag_filter)
            glGenerateMipmap(GL_TEXTURE_2D);

        lphash_put(texture_info_map, id, texture_info);
        parray_add(texture_info_array, texture_info);
    } else {
        glBindTexture(GL_TEXTURE_2D, texture_info->texture_id);
    }

    texture_info->frames_since_referenced = 0;
    texture_info->canvas_id = current_canvas_id;

    return 0;
}

// Textures
JNIEXPORT jint JNICALL Java_april_vis_GL_gldata_1tex_1bind__JIIIII_3II
(JNIEnv *jenv, jclass jcls, jlong id, jint internalfmt, jint width, jint height, jint fmt, jint type, jintArray jdata, jint flags)
{
    return gldata_tex_bind(jenv, jcls, id, internalfmt, width, height, fmt, type, jdata, 4, flags);
}

JNIEXPORT jint JNICALL Java_april_vis_GL_gldata_1tex_1bind__JIIIII_3BI
(JNIEnv *jenv, jclass jcls, jlong id, jint internalfmt, jint width, jint height, jint fmt, jint type, jbyteArray jdata, jint flags)
{
    return gldata_tex_bind(jenv, jcls, id, internalfmt, width, height, fmt, type, jdata, 1, flags);
}

JNIEXPORT jint JNICALL Java_april_vis_GL_gldata_1tex_1unbind
  (JNIEnv *jenv, jclass jcls, jlong id)
{
    glBindTexture(GL_TEXTURE_2D, 0);
    return 0;
}


////////////////////////////////////////////////////////////////////
JNIEXPORT jint JNICALL Java_april_vis_GL_gl_1get_1error
  (JNIEnv *jenv, jclass jcls)
{
    return glGetError();
}


JNIEXPORT jint JNICALL Java_april_vis_GL_gl_1get_1double_1v
  (JNIEnv *jenv, jclass jcls, jint param, jdoubleArray jdata)
{
    jboolean is_copy = 0;
    jdouble *data = (*jenv)->GetPrimitiveArrayCritical(jenv, jdata, &is_copy);

    glGetDoublev(param, data);

    (*jenv)->ReleasePrimitiveArrayCritical(jenv, jdata, data, 0);
    return 0;
}


