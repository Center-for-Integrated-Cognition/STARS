#include <stdlib.h>
#include <stdio.h>
#include "image_u8.h"

#define ALIGNMENT 16

image_u8_t *image_u8_create(int width, int height)
{
    image_u8_t *im = (image_u8_t*) calloc(1, sizeof(image_u8_t));

    im->width = width;
    im->height = height;
    im->stride = width;

    if ((im->stride % ALIGNMENT) != 0)
        im->stride += ALIGNMENT - (im->stride % ALIGNMENT);

    im->buf = (uint8_t*) calloc(1, im->height*im->stride);

    return im;
}

void image_u8_destroy(image_u8_t *im)
{
    free(im->buf);
    free(im);
}

image_u8_t *image_u8_create_from_pnm(const char *path)
{
    int width, height, format;

    FILE *f = fopen(path, "rb");
    if (f == NULL)
        return NULL;

    if (3 != fscanf(f, "P%d\n%d %d\n255\n", &format, &width, &height))
        goto error;

    image_u8_t *im = image_u8_create(width, height);

    if (format == 6) {
        int stride = width*3;
        int sz = stride*height;

        uint8_t *rgb = malloc(sz);

        if (sz != fread(rgb, 1, sz, f))
            goto error;

        fclose(f);

        for (int y = 0; y < im->height; y++) {
            for (int x = 0; x < im->width; x++) {
                int r = rgb[y*stride + 3*x + 0];
                int g = rgb[y*stride + 3*x + 1];
                int b = rgb[y*stride + 3*x + 2];

                int gray = (int) (0.3*r + 0.59*g + 0.11*b); // XXX not gamma correct
                if (gray > 255)
                    gray = 255;

                im->buf[y*im->stride + x] = gray;
            }
        }

        return im;
    }

error:
    fclose(f);

    if (im != NULL)
        image_u8_destroy(im);

    return NULL;
}

image_u8_t *image_u8_create_from_rgb3(int width, int height, uint8_t *rgb, int stride)
{
    image_u8_t *im = image_u8_create(width, height);

    for (int y = 0; y < im->height; y++) {
        for (int x = 0; x < im->width; x++) {
            int r = rgb[y*stride + 3*x + 0];
            int g = rgb[y*stride + 3*x + 0];
            int b = rgb[y*stride + 3*x + 0];

            int gray = (int) (0.6*g + 0.3*r + 0.1*b);
            if (gray > 255)
                gray = 255;

            im->buf[y*im->stride + x] = gray;
        }
    }

    return im;
}

image_u8_t *image_u8_create_from_f32(image_f32_t *fim)
{
    image_u8_t *im = image_u8_create(fim->width, fim->height);

    for (int y = 0; y < fim->height; y++) {
        for (int x = 0; x < fim->width; x++) {
            float v = fim->buf[y*fim->stride + x];
            im->buf[y*im->stride + x] = (int) (255 * v);
        }
    }

    return im;
}


int image_u8_write_pgm(image_u8_t *im, const char *path)
{
    FILE *f = fopen(path, "wb");
    int res = 0;

    if (f == NULL) {
        res = -1;
        goto finish;
    }

    fprintf(f, "P5\n%d %d\n255\n", im->width, im->height);

    for (int y = 0; y < im->height; y++) {
        if (im->width != fwrite(&im->buf[y*im->stride], 1, im->width, f)) {
            res = -2;
            goto finish;
        }
    }

finish:
    if (f != NULL)
        fclose(f);

    return res;
}
