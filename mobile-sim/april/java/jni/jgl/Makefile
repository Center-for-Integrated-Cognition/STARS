include ../common.mk

LIB=libjgl.$(SHARED_EXT)
DESTLIB=../../../lib/$(LIB)

all:  $(DESTLIB)


$(DESTLIB) : $(LIB)
	@echo "    [$(LIB)]"
	cp $(LIB) $(DESTLIB)

LIBJGL_OBJS += glcontext.o glcontext-x11.o gl_jni.o lphash.o parray.o
$(LIB): april_vis_GL.h $(LIBJGL_OBJS)
	$(LD) $(LDFLAGS) $(LIBJGL_OBJS) -o $@ -lGL -lX11

#############################################################
%.o: %.c
	$(CC) $(CCFLAGS) $(JNI_INCLUDES) $<

april_vis_GL.h: ../../src/april/vis/GL.java
	@echo "NOTICE: Rebuilding JNI headers. Ensure java file has been recently built."
	javah -classpath ../../april.jar  april.vis.GL

clean:
	rm -rf $(LIBJGL_OBJS) *~ april_vis_GL.h $(LIB) $(DESTLIB)
