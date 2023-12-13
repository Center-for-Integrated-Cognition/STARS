#include <stdlib.h>
#include <string.h>
#include <GL/gl.h>
#include <GL/glx.h>
#include <assert.h>

#include "glcontext.h"

glcontext_t *glcontext_X11_create()
{
    glcontext_t *glc = (glcontext_t*) calloc(1, sizeof(glcontext_t));
    glcontext_init(glc);

    Display *dpy = XOpenDisplay(NULL);
    assert(dpy != NULL);

    Window root = DefaultRootWindow(dpy);
    // These GL settings are unimportant; see glcontext.c for the allocation of the FBO.
    GLint att[] = { GLX_RGBA, GLX_DEPTH_SIZE, 16, GLX_DOUBLEBUFFER, None };
    XVisualInfo *vi = glXChooseVisual(dpy, 0, att);
    Colormap cmap = XCreateColormap(dpy, root, vi->visual, AllocNone);

    XSetWindowAttributes    swa;
    memset(&swa, 0, sizeof(XSetWindowAttributes));
    swa.colormap = cmap;
    swa.event_mask = ExposureMask | KeyPressMask;

    Window win = XCreateWindow(dpy, root, 0, 0, 100, 100, 0, vi->depth, InputOutput, vi->visual, CWColormap | CWEventMask, &swa);

    GLXContext _glc = glXCreateContext(dpy, vi, NULL, GL_TRUE);
    glXMakeCurrent(dpy, win, _glc);

    return glc;
}
