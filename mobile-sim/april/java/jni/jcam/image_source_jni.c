#include <stdlib.h>
#include <string.h>
#include <jni.h>

#include <assert.h>

#include "image_source.h"

#define MAX_DESCRIPTORS 32
static image_source_t *isrcs[MAX_DESCRIPTORS];

/*
 * Class:     jcam_ImageSource
 * Method:    image_source_open_jni
 * Signature: (Ljava/lang/String;)I
 */
JNIEXPORT jint JNICALL Java_april_jcam_ImageSourceNative_image_1source_1open_1jni
(JNIEnv *jenv, jclass jcls, jstring _url)
{
    // allocate a descriptor
    int srcid = -1;
    for (int i = 0; i < MAX_DESCRIPTORS; i++) {
        if (isrcs[i]==NULL) {
            srcid = i;
            break;
        }
    }

    if (srcid < 0)
        return -1;

    // try to open
    const char *url=(*jenv)->GetStringUTFChars(jenv, _url, 0);

    image_source_t *isrc = image_source_open(url);

    (*jenv)->ReleaseStringUTFChars(jenv, _url, url);

    if (isrc == NULL)
        return -1;

    isrcs[srcid] = isrc;
    return srcid;
}

/*
 * Class:     jcam_ImageSource
 * Method:    image_source_open_num_formats
 * Signature: (I)I
 */
JNIEXPORT jint JNICALL Java_april_jcam_ImageSourceNative_image_1source_1num_1formats_1jni
  (JNIEnv *jenv, jclass jcls, jint srcid)
{
    image_source_t *isrc = isrcs[srcid];
    return isrc->num_formats(isrc);
}


// Helper function to convert image_source_format_t to ImageSourceFormat, needed by getFormat() and getFrame()
jobject image_source_format_t_to_ImageSourceFormat(JNIEnv *jenv, image_source_format_t * fmt)
{

    jclass formatClass = (*jenv)->FindClass(jenv, "april/jcam/ImageSourceFormat");
    assert(formatClass != NULL);

    jmethodID methodId = (*jenv)->GetMethodID(jenv, formatClass, "<init>", "()V");
    assert(methodId != NULL);

    jobject ifmt = (*jenv)->NewObject(jenv, formatClass, methodId);

    if (1) {
        jstring fcc = (*jenv)->NewStringUTF(jenv, fmt->format);
        jfieldID fourccFieldId = (*jenv)->GetFieldID(jenv, formatClass, "format", "Ljava/lang/String;");
        assert(fourccFieldId != NULL);
        (*jenv)->SetObjectField(jenv, ifmt, fourccFieldId, fcc);

        jfieldID widthId = (*jenv)->GetFieldID(jenv, formatClass, "width", "I");
        (*jenv)->SetIntField(jenv, ifmt, widthId, fmt->width);

        jfieldID heightId = (*jenv)->GetFieldID(jenv, formatClass, "height", "I");
        (*jenv)->SetIntField(jenv, ifmt, heightId, fmt->height);

        (*jenv)->DeleteLocalRef(jenv, fcc);
    }

    return ifmt;
}

/*
 * Class:     jcam_ImageSource
 * Method:    image_source_get_format
 * Signature: (II)Ljcam/ImageSourceFormat;
 */
JNIEXPORT jobject JNICALL Java_april_jcam_ImageSourceNative_image_1source_1get_1format_1jni
  (JNIEnv *jenv, jclass jcls, jint srcid, jint fmtidx)
{
    image_source_t *isrc = isrcs[srcid];
    image_source_format_t *fmt = isrc->get_format(isrc, fmtidx);

    return image_source_format_t_to_ImageSourceFormat(jenv, fmt);
}

/*
 * Class:     jcam_ImageSource
 * Method:    image_source_enumerate_jni
 * Signature: ()Ljava/util/ArrayList;
 */
JNIEXPORT jobject JNICALL Java_april_jcam_ImageSourceNative_image_1source_1enumerate_1jni
  (JNIEnv *jenv, jclass jcls)
{
    jclass arrayListClass = (*jenv)->FindClass(jenv, "java/util/ArrayList");
    assert(arrayListClass != NULL);

    jmethodID initMethodId = (*jenv)->GetMethodID(jenv, arrayListClass, "<init>", "()V");
    assert(initMethodId != NULL);

    jobject arrayList = (*jenv)->NewObject(jenv, arrayListClass, initMethodId);

    jmethodID addMethodId = (*jenv)->GetMethodID(jenv, arrayListClass, "add", "(Ljava/lang/Object;)Z");
    assert(addMethodId != NULL);

    char **urls = image_source_enumerate();

    for (int i = 0; urls[i] != NULL; i++) {
        jstring sobj = (*jenv)->NewStringUTF(jenv, urls[i]);
        (*jenv)->CallBooleanMethod(jenv, arrayList, addMethodId, sobj);
        (*jenv)->DeleteLocalRef(jenv, sobj);
    }

    image_source_enumerate_free(urls);

    return arrayList;
}

/*
 * Class:     jcam_ImageSource
 * Method:    image_source_get_current_format_jni
 * Signature: (I)I
 */
JNIEXPORT jint JNICALL Java_april_jcam_ImageSourceNative_image_1source_1get_1current_1format_1jni
  (JNIEnv *jenv, jclass cjls, jint srcid)
{
    image_source_t *isrc = isrcs[srcid];
    return isrc->get_current_format(isrc);
}

/*
 * Class:     jcam_ImageSource
 * Method:    image_source_set_format
 * Signature: (II)I
 */
JNIEXPORT jint JNICALL Java_april_jcam_ImageSourceNative_image_1source_1set_1format_1jni
  (JNIEnv *jenv, jclass jcls, jint srcid, jint fmtidx)
{
    image_source_t *isrc = isrcs[srcid];
    return isrc->set_format(isrc, fmtidx);
}

/*
 * Class:     jcam_ImageSource
 * Method:    image_source_start
 * Signature: (I)I
 */
JNIEXPORT jint JNICALL Java_april_jcam_ImageSourceNative_image_1source_1start_1jni
  (JNIEnv *jenv, jclass jcls, jint srcid)
{
    image_source_t *isrc = isrcs[srcid];
    return isrc->start(isrc);
}

/*
 * Class:     april_jcam_ImageSourceNative
 * Method:    image_source_get_frame_jni
 * Signature: (I)Lapril/jcam/FrameData;
 */
JNIEXPORT jobject JNICALL Java_april_jcam_ImageSourceNative_image_1source_1get_1frame_1jni
  (JNIEnv *jenv, jclass jcls, jint srcid)
{
    image_source_t *isrc = isrcs[srcid];

    /* void *imbuf = NULL; */
    /* int imbuflen = 0; */

    frame_data_t * fdat = calloc(1, sizeof(frame_data_t));
    int res = isrc->get_frame(isrc, fdat);

    if (res < 0)
        return NULL;

    jclass frameDataClass = (*jenv)->FindClass(jenv, "april/jcam/FrameData");
    assert(frameDataClass != NULL);

    jmethodID methodId = (*jenv)->GetMethodID(jenv, frameDataClass, "<init>", "()V");
    assert(methodId != NULL);

    jobject frmd = (*jenv)->NewObject(jenv, frameDataClass, methodId);
    if (1) {
        // Get the image source format
        jobject ifmt = image_source_format_t_to_ImageSourceFormat(jenv, fdat->ifmt);
        jfieldID ifmtId = (*jenv)->GetFieldID(jenv, frameDataClass, "ifmt", "Lapril/jcam/ImageSourceFormat;");
        (*jenv)->SetObjectField(jenv, frmd, ifmtId, ifmt);
        (*jenv)->DeleteLocalRef(jenv, ifmt);

        jfieldID utimeId = (*jenv)->GetFieldID(jenv, frameDataClass, "utime", "J");
        (*jenv)->SetLongField(jenv, frmd, utimeId, (jlong) fdat->utime);

        jfieldID dataId = (*jenv)->GetFieldID(jenv, frameDataClass, "data", "[B");
        jbyteArray bytes = NULL;
        bytes = (*jenv)->NewByteArray(jenv, fdat->datalen);
        (*jenv)->SetByteArrayRegion(jenv, bytes, 0, fdat->datalen, (jbyte*) fdat->data);
        (*jenv)->SetObjectField(jenv, frmd, dataId, bytes);
        (*jenv)->DeleteLocalRef(jenv, bytes);
    }
    isrc->release_frame(isrc, fdat);
    free(fdat);

    return frmd;
}

/*
 * Class:     jcam_ImageSource
 * Method:    image_source_stop
 * Signature: (I)I
 */
JNIEXPORT jint JNICALL Java_april_jcam_ImageSourceNative_image_1source_1stop_1jni
  (JNIEnv *jenv, jclass jcls, jint srcid)
{
    image_source_t *isrc = isrcs[srcid];
    return isrc->stop(isrc);
}

/*
 * Class:     jcam_ImageSource
 * Method:    image_source_close_jni
 * Signature: (I)I
 */
JNIEXPORT jint JNICALL Java_april_jcam_ImageSourceNative_image_1source_1close_1jni
  (JNIEnv *jenv, jclass jcls, jint srcid)
{
    image_source_t *isrc = isrcs[srcid];
    return isrc->close(isrc);
}

/*
 * Class:     april_jcam_ImageSourceNative
 * Method:    image_source_get_num_features
 * Signature: (I)I
 */
JNIEXPORT jint JNICALL Java_april_jcam_ImageSourceNative_image_1source_1get_1num_1features
  (JNIEnv *jenv, jclass jcls, jint srcid)
{
    image_source_t *isrc = isrcs[srcid];

    return isrc->num_features(isrc);
}

/*
 * Class:     april_jcam_ImageSourceNative
 * Method:    image_source_get_feature_name
 * Signature: (II)Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_april_jcam_ImageSourceNative_image_1source_1get_1feature_1name
  (JNIEnv *jenv, jclass jcls, jint srcid, jint idx)
{
    image_source_t *isrc = isrcs[srcid];

    jstring sobj = (*jenv)->NewStringUTF(jenv, isrc->get_feature_name(isrc, idx));

    return sobj;
}

/*
 * Class:     april_jcam_ImageSourceNative
 * Method:    image_source_is_feature_available
 * Signature: (II)Z
 */
JNIEXPORT jboolean JNICALL Java_april_jcam_ImageSourceNative_image_1source_1is_1feature_1available
  (JNIEnv *jenv, jclass jcls, jint srcid, jint idx)
{
    image_source_t *isrc = isrcs[srcid];

    return isrc->is_feature_available(isrc, idx);
}

/*
 * Class:     april_jcam_ImageSourceNative
 * Method:    image_source_get_feature_type
 * Signature: (II)Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_april_jcam_ImageSourceNative_image_1source_1get_1feature_1type
  (JNIEnv *jenv, jclass jcls, jint srcid, jint idx)
{
    image_source_t *isrc = isrcs[srcid];

    char *type = isrc->get_feature_type(isrc, idx);
    assert(type != NULL);
    jstring sobj = (*jenv)->NewStringUTF(jenv, type);
    free(type);

    return sobj;
}

/*
 * Class:     april_jcam_ImageSourceNative
 * Method:    image_source_get_feature_min
 * Signature: (II)D
 */
// JNIEXPORT jdouble JNICALL Java_april_jcam_ImageSourceNative_image_1source_1get_1feature_1min
//   (JNIEnv *jenv, jclass jcls, jint srcid, jint idx)
// {
//     image_source_t *isrc = isrcs[srcid];

//     return isrc->get_feature_min(isrc, idx);
// }

/*
 * Class:     april_jcam_ImageSourceNative
 * Method:    image_source_get_feature_max
 * Signature: (II)D
 */
// JNIEXPORT jdouble JNICALL Java_april_jcam_ImageSourceNative_image_1source_1get_1feature_1max
//   (JNIEnv *jenv, jclass jcls, jint srcid, jint idx)
// {
//     image_source_t *isrc = isrcs[srcid];

//     return isrc->get_feature_max(isrc, idx);
// }

/*
 * Class:     april_jcam_ImageSourceNative
 * Method:    image_source_get_feature_value
 * Signature: (II)D
 */
JNIEXPORT jdouble JNICALL Java_april_jcam_ImageSourceNative_image_1source_1get_1feature_1value
  (JNIEnv *jenv, jclass jcls, jint srcid, jint idx)
{
    image_source_t *isrc = isrcs[srcid];

    return isrc->get_feature_value(isrc, idx);
}

/*
 * Class:     april_jcam_ImageSourceNative
 * Method:    image_source_set_feature_value
 * Signature: (IID)I
 */
JNIEXPORT jint JNICALL Java_april_jcam_ImageSourceNative_image_1source_1set_1feature_1value
  (JNIEnv *jenv, jclass jcls, jint srcid, jint idx, jdouble v)
{
    image_source_t *isrc = isrcs[srcid];

    return isrc->set_feature_value(isrc, idx, v);
}

/*
 * Class:     april_jcam_ImageSourceNative
 * Method:    image_source_print_info
 * Signature: (I)V
 */
JNIEXPORT void JNICALL Java_april_jcam_ImageSourceNative_image_1source_1print_1info
  (JNIEnv *jenv, jclass jcls, jint srcid)
{
    image_source_t *isrc = isrcs[srcid];

    isrc->print_info(isrc);
}
