#ifndef _STRING_UTIL_H
#define _STRING_UTIL_H

#include "varray.h"

#ifdef __cplusplus
extern "C" {
#endif

    typedef struct string_buffer string_buffer_t;

    typedef struct string_feeder string_feeder_t;

    char *sprintf_alloc(const char *fmt, ...) __attribute__ ((format (printf, 1, 2)));

    // you must free the varray and the strings contained within.
    // e.g.:
    // varray_t *va = str_split("this is a haystack", " ");
    // varray_map(va, free);
    // varray_destroy(va);
    varray_t *str_split(const char *str, const char *delim);

    //////////////////////////////////////////////////////
    int str_ends_with(const char *s, const char *needle);
    int str_starts_with(const char *s, const char *needle);

    // allocated a new string.  The new string contains characters
    // from 'startidx' through (inclusive) 'endidx-1'. If endidx is
    // -1, it is equivalent to strlen(s).
    char *str_substring(const char *s, int startidx, int endidx);

    char *str_replace(const char *haystack, const char *needle, const char *replacement);

    //////////////////////////////////////////////////////
    // String Buffer
    string_buffer_t *string_buffer_create();
    void string_buffer_destroy(string_buffer_t *sb);
    void string_buffer_append(string_buffer_t *sb, char c);
    void string_buffer_append_string(string_buffer_t *sb, const char *s);
    void string_buffer_appendf(string_buffer_t *sb, const char *fmt, ...) __attribute__ ((format (printf, 2, 3)));
    int string_buffer_ends_with(string_buffer_t *sb, const char *s);
    // returns length of string (not counting \0)
    int string_buffer_size(string_buffer_t *sb);

    // caller's responsibility to free the returned string.
    char *string_buffer_to_string(string_buffer_t *sb);

    void string_buffer_reset(string_buffer_t *sb);

    //////////////////////////////////////////////////////
    // String Feeder

    // makes an internal copy of s (which will be freed with _destroy)
    string_feeder_t *string_feeder_create(const char *s);
    void string_feeder_destroy(string_feeder_t *sf);
    int string_feeder_has_next(string_feeder_t *sf);
    char string_feeder_next(string_feeder_t *sf);
    char string_feeder_peek(string_feeder_t *sf);
    int string_feeder_get_line(string_feeder_t *sf);
    int string_feeder_get_column(string_feeder_t *sf);

#ifdef __cplusplus
}
#endif

#endif
