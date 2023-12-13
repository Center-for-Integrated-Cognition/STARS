/*
 *  parray.h
 *  lcmgl
 *
 *  Created by Edwin Olson on 4/20/10.
 *  Copyright 2010 ebolson. All rights reserved.
 *
 */
#ifndef _PARRAY_H
#define _PARRAY_H

typedef struct parray parray_t;

parray_t *parray_create();
void parray_destroy(parray_t *va);
int parray_size(parray_t *va);
void parray_add(parray_t *va, void *p);
void *parray_get(parray_t *va, int idx);
void parray_remove(parray_t *va, void *d);
void parray_remove_shuffle(parray_t *va, int idx);
#endif
