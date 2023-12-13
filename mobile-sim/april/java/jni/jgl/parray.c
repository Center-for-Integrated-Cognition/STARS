/*
 *  parray.c
 *  lcmgl
 *
 *  Created by Edwin Olson on 4/20/10.
 *  Copyright 2010 ebolson. All rights reserved.
 *
 */

#include "parray.h"
#include <math.h>
#include <stdlib.h>
#include <assert.h>

#define MIN_ALLOC 16

struct parray
{
	int size;
	int alloc;
	void **data;
};

parray_t *parray_create()
{
	parray_t *va = calloc(1, sizeof(parray_t));
	return va;
}

void parray_destroy(parray_t *va)
{
	if (va->data != NULL)
		free(va->data);
	free(va);
}

int parray_size(parray_t *va)
{
	return va->size;
}

void parray_add(parray_t *va, void *p)
{
	if (va->size == va->alloc) {
		int newalloc = va->alloc*2;
		if (newalloc < MIN_ALLOC)
			newalloc = MIN_ALLOC;
		va->data = realloc(va->data, sizeof(void*)*newalloc);
		va->alloc = newalloc;
	}

	va->data[va->size] = p;
	va->size++;
}

void *parray_get(parray_t *va, int idx)
{
	assert(idx < va->size);
	return va->data[idx];
}

void parray_remove(parray_t *va, void *d)
{
    int outpos = 0;
    for (int inpos = 0; inpos < va->size; inpos++) {
        if (va->data[inpos] == d)
            continue;
        va->data[outpos] = va->data[inpos];
        outpos++;
    }
    va->size = outpos;
}

void parray_remove_shuffle(parray_t *va, int idx)
{
    va->data[idx] = va->data[va->size-1];
    va->size --;
}
