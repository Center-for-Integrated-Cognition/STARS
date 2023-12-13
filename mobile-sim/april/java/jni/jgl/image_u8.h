#ifndef _IMAGE_U8_H
#define _IMAGE_U8_H

#include <stdint.h>

typedef struct image_u8 image_u8_t;
struct image_u8
{
    int width, height;
    int stride;

    uint8_t *buf;
};

#include "image_f32.h"

image_u8_t *image_u8_create(int width, int height);
image_u8_t *image_u8_create_from_rgb3(int width, int height, uint8_t *rgb, int stride);
image_u8_t *image_u8_create_from_f32(image_f32_t *fim);
image_u8_t *image_u8_create_from_pnm(const char *path);

void image_u8_destroy(image_u8_t *im);

int image_u8_write_pgm(image_u8_t *im, const char *path);

#endif
