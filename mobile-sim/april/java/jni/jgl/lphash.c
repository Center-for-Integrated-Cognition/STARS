#include <stdlib.h>
#include <string.h>
#include <assert.h>

#include "lphash.h"

#define INITIAL_SIZE 16

// when the ratio of allocations to actual size drops below this
// ratio, we rehash. (Reciprocal of more typical load factor.)
#define REHASH_RATIO 2

lphash_t *lphash_create()
{
    lphash_t *vh = (lphash_t*) calloc(1, sizeof(lphash_t));

    vh->alloc = INITIAL_SIZE;
    vh->elements = (struct lphash_element**) calloc(vh->alloc, sizeof(struct lphash_element*));
    return vh;
}

// free all lphash_element structs. (does not free keys or values).
static void free_elements(struct lphash_element **elements, int alloc)
{
    for (int i = 0; i < alloc; i++) {
        struct lphash_element *el = elements[i];
        while (el != NULL) {
            struct lphash_element *nextel = el->next;
            free(el);
            el = nextel;
        }
    }
}

void lphash_destroy(lphash_t *vh)
{
    free_elements(vh->elements, vh->alloc);
    free(vh->elements);
    free(vh);
}

void *lphash_get(lphash_t *vh, uint64_t key)
{
    uint64_t hash = key;
    int idx = hash % vh->alloc;

    struct lphash_element *el = vh->elements[idx];
    while (el != NULL) {
        if (el->key == key) {
            return el->value;
        }
        el = el->next;
    }

    return NULL;
}

// returns one if a new element was added, 0 else. This is abstracted
// so that we can use it when put-ing and resizing.
static inline int lphash_put_real(lphash_t *vh, struct lphash_element **elements, int alloc, uint64_t key, void *value)
{
    uint64_t hash = key;
    int idx = hash % alloc;

    // replace an existing key if it exists.
    struct lphash_element *el = elements[idx];
    while (el != NULL) {
        if (el->key == key) {
            el->value = value;
            return 0;
        }
        el = el->next;
    }

    // create a new key and prepend it to our linked list.
    el = (struct lphash_element*) calloc(1, sizeof(struct lphash_element));
    el->key = key;
    el->value = value;
    el->next = elements[idx];

    elements[idx] = el;
    return 1;
}

// returns number of elements removed
lphash_pair_t lphash_remove(lphash_t *vh, uint64_t key)
{
    uint64_t hash = key;
    int idx = hash % vh->alloc;

    struct lphash_element **out = &vh->elements[idx];
    struct lphash_element *in = vh->elements[idx];

    lphash_pair_t pair;
    pair.key = 0;
    pair.value = NULL;

    while (in != NULL) {
        if (in->key == key) {
            // remove this element.
            pair.key = in->key;
            pair.value = in->value;

            struct lphash_element *tmp = in->next;
            free(in);
            in = tmp;
        } else {
            // keep this element (copy it back out)
            *out = in;
            out = &in->next;
            in = in->next;
        }
    }

    *out = NULL;
    return pair;
}

void lphash_put(lphash_t *vh, uint64_t key, void *value)
{
    int added = lphash_put_real(vh, vh->elements, vh->alloc, key, value);
    vh->size += added;

    int ratio = vh->alloc / vh->size;

    if (ratio < REHASH_RATIO) {
        // resize
        int newalloc = vh->alloc*2;
        struct lphash_element **newelements = (struct lphash_element**) calloc(newalloc, sizeof(struct lphash_element*));

        // put all our existing elements into the new hash table
        for (int i = 0; i < vh->alloc; i++) {
            struct lphash_element *el = vh->elements[i];
            while (el != NULL) {
                lphash_put_real(vh, newelements, newalloc, el->key, el->value);
                el = el->next;
            }
        }

        // free the old elements
        free_elements(vh->elements, vh->alloc);

        // switch to the new elements
        vh->alloc = newalloc;
        vh->elements = newelements;
    }
}

static void lphash_iterator_find_next(lphash_t *vh, lphash_iterator_t *vit)
{
    // fetch the next one.

    // any more left in this bucket?
    if (vit->el != NULL)
        vit->el = vit->el->next;

    // search for the next non-empty bucket.
    while (vit->el == NULL) {
        if (vit->bucket + 1 == vh->alloc) {
            vit->el = NULL; // the end
            return;
        }

        vit->bucket++;
        vit->el = vh->elements[vit->bucket];
    }
}

void lphash_iterator_init(lphash_t *vh, lphash_iterator_t *vit)
{
    vit->bucket = -1;
    vit->el = NULL;
    lphash_iterator_find_next(vh, vit);
}

uint64_t lphash_iterator_next_key(lphash_t *vh, lphash_iterator_t *vit)
{
    if (vit->el == NULL) {
        // has_next would have returned false.
        assert(0);
        return 0;
    }

    uint64_t key = vit->el->key;

    lphash_iterator_find_next(vh, vit);

    return key;
}

int lphash_iterator_has_next(lphash_t *vh, lphash_iterator_t *vit)
{
    return (vit->el != NULL);
}

