#ifndef _LPHASH_H
#define _LPHASH_H

#include <stdint.h>

/** lphash is for the special case where the key is a long. The value
 * is a pointer to an opaque blob.
 **/

struct lphash_element;

struct lphash_element
{
    uint64_t key;
    void *value;

    struct lphash_element *next;
};

typedef struct
{
    int alloc;
    int size;

    struct lphash_element **elements;

} lphash_t;

typedef struct
{
    int bucket;
    struct lphash_element *el; // the next one to be returned.

    int has_next;

} lphash_iterator_t;

typedef struct
{
    uint64_t key;
    void *value;
} lphash_pair_t;

lphash_t *lphash_create();
void lphash_destroy(lphash_t *vh);
void *lphash_get(lphash_t *vh, uint64_t key);

// the old key will be retained in preference to the new key.
void lphash_put(lphash_t *vh, uint64_t key, void *value);

void lphash_iterator_init(lphash_t *vh, lphash_iterator_t *vit);
uint64_t lphash_iterator_next_key(lphash_t *vh, lphash_iterator_t *vit);
int lphash_iterator_has_next(lphash_t *vh, lphash_iterator_t *vit);

// returns the removed element pair, which can be used for deallocation.
lphash_pair_t lphash_remove(lphash_t *vh, uint64_t key);

#endif
