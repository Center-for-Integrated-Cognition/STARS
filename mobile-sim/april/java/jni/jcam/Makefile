include ../common.mk

LIBIMAGESOURCE_OBJS = image_source.o image_source_v4l2.o image_source_jni.o image_source_dc1394.o url_parser.o image_source_islog.o image_source_pgusb.o image_source_filedir.o

LIB=libimagesource.so
DESTLIB=../../../lib/$(LIB)

ALL = $(DESTLIB) istest tcpstream

#############################################################
# MacOS X
ifeq "$(shell uname)" "Darwin"

.phony:
	@echo "JCam is not supported on Darwin/MacOS."

ALL = .phony

endif

#############################################################


all: $(ALL)


istest: main.o $(LIBIMAGESOURCE_OBJS)
	@echo "    [$@]"
	gcc -L../../../lib main.o $(LIBIMAGESOURCE_OBJS) -o istest -ldc1394 -lc -lcommon -lpng -lpthread -lusb-1.0 -lm

tcpstream: tcp_image_streamer.c
	@echo "    [$@]"
	gcc -o tcpstream -std=gnu99 tcp_image_streamer.c -lc

$(DESTLIB): $(LIB)
	@echo "    [$@]"
	cp $(LIB) $(DESTLIB)

libimagesource.so: april_jcam_ImageSourceNative.h $(LIBIMAGESOURCE_OBJS)
	$(LD) $(LDFLAGS) $(LIBIMAGESOURCE_OBJS) -o libimagesource.so -ldc1394 -lc -lcommon -lpng

april_jcam_ImageSourceNative.h:
	echo "Rebuilding JNI headers. Ensure java file has been recently built."
	javah -classpath ../../april.jar  april.jcam.ImageSourceNative

clean:
	rm -f *~ april_jcam_ImageSourceNative.h april_jcam_ImageSourceFormat.h $(LIB) $(DESTLIB) $(LIBIMAGESOURCE_OBJS) istest main.o tcpstream tcp_image_streamer.o

# If you get an error message saying that you're missing jni.h or
# jni_md.h, locate those files and add the directories using the -I
# flag. See example below

DC1394_INCLUDES = -I/usr/local/include/

%.o: %.c
	$(CC) $(CCFLAGS) $(JNI_INCLUDES) $(DC1394_INCLUDES) $<
