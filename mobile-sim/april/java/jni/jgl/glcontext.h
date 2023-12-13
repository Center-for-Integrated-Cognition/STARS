#ifndef _GLCONTEXT_H
#define _GLCONTEXT_H

#include <GL/gl.h>

typedef struct glcontext glcontext_t;
struct glcontext
{
    void *impl;
};

typedef struct gl_fbo gl_fbo_t;
struct gl_fbo
{
    glcontext_t *glc;

    GLuint fbo_id; // frame buffer object id
    GLuint depth_id;
    GLuint color_id;
};

glcontext_t *glcontext_X11_create();

// glcontext_init() not to be called by anyone except platform constructors
void glcontext_init();

gl_fbo_t *gl_fbo_create(glcontext_t *glc, int width, int height);
void gl_fbo_bind(gl_fbo_t *fbo);
void gl_fbo_destroy(gl_fbo_t *fbo);

#endif
