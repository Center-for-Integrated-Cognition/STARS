#include <errno.h>
#include <string.h>
#include <stdarg.h>
#include <stdlib.h>
#include <stdio.h>

#include "string_util.h"
#include "varray.h"

struct string_buffer
{
    char *s;
    int alloc;
    int size; // as if strlen() was called; not counting terminating \0
};

struct string_feeder
{
    char *s;
    int len;
    int pos;

    int line, col;
    char lastc;
};

#define MIN_PRINTF_ALLOC 16

/** Allocate a string and call sprintf, resizing if necessary and calling
 *  sprintf again.  Returns a char* that must be freed by the caller.
 */
char *sprintf_alloc(const char *fmt, ...)
{
    int size = MIN_PRINTF_ALLOC;
    char *buf = malloc(size * sizeof(char));

    int returnsize;
    va_list args;

    va_start(args,fmt);
    returnsize = vsnprintf(buf, size, fmt, args);
    va_end(args);

    // it was successful
    if (returnsize < size) {
        return buf;
    }

    // otherwise, we should try again
    free(buf);
    size = returnsize + 1;
    buf = malloc(size * sizeof(char));

    va_start(args,fmt);
    returnsize = vsnprintf(buf, size, fmt, args);
    va_end(args);

    return buf;
}

varray_t *str_split(const char *str, const char *delim)
{
    varray_t *parts = varray_create();
    string_buffer_t *sb = string_buffer_create();

    int delim_len = strlen(delim);
    int len = strlen(str);
    int pos = 0;

    while (pos < len) {
        if (str_starts_with(&str[pos], delim)) {
            pos += delim_len;
            varray_add(parts, string_buffer_to_string(sb));
            string_buffer_reset(sb);
        } else {
            string_buffer_append(sb, str[pos]);
            pos++;
        }
    }

    if (string_buffer_size(sb) > 0)
        varray_add(parts, string_buffer_to_string(sb));

    string_buffer_destroy(sb);
    return parts;
}

string_buffer_t* string_buffer_create()
{
    string_buffer_t *sb = (string_buffer_t*) calloc(1, sizeof(string_buffer_t));
    sb->alloc = 32;
    sb->s = calloc(sb->alloc, 1);
    return sb;
}

void string_buffer_destroy(string_buffer_t *sb)
{
    if (sb->s)
        free(sb->s);
    free(sb);
}

void string_buffer_append(string_buffer_t *sb, char c)
{
    if (sb->size+2 >= sb->alloc) {
        sb->alloc *= 2;
        sb->s = realloc(sb->s, sb->alloc);
    }

    sb->s[sb->size++] = c;
    sb->s[sb->size] = 0;
}

void string_buffer_appendf(string_buffer_t *sb, const char *fmt, ...)
{
    int size = MIN_PRINTF_ALLOC;
    char *buf = malloc(size * sizeof(char));

    int returnsize;
    va_list args;

    va_start(args,fmt);
    returnsize = vsnprintf(buf, size, fmt, args);
    va_end(args);

    if (returnsize >= size) {
        // otherwise, we should try again
        free(buf);
        size = returnsize + 1;
        buf = malloc(size * sizeof(char));

        va_start(args, fmt);
        returnsize = vsnprintf(buf, size, fmt, args);
        va_end(args);
    }

    string_buffer_append_string(sb, buf);
    free(buf);
}

void string_buffer_append_string(string_buffer_t *sb, const char *s)
{
    int len = strlen(s);

    while (sb->size+len + 1 >= sb->alloc) {
        sb->alloc *= 2;
        sb->s = realloc(sb->s, sb->alloc);
    }

    memcpy(&sb->s[sb->size], s, len);
    sb->size += len;
    sb->s[sb->size] = 0;
}

int string_buffer_ends_with(string_buffer_t *sb, const char *s)
{
    return str_ends_with(sb->s, s);
}

char *string_buffer_to_string(string_buffer_t *sb)
{
    return strdup(sb->s);
}

// returns length of string (not counting \0)
int string_buffer_size(string_buffer_t *sb)
{
    return sb->size;
}

void string_buffer_reset(string_buffer_t *sb)
{
    sb->s[0] = 0;
    sb->size = 0;
}

string_feeder_t *string_feeder_create(const char *s)
{
    string_feeder_t *sf = (string_feeder_t*) calloc(1, sizeof(string_feeder_t));
    sf->s = strdup(s);
    sf->len = strlen(sf->s);
    sf->line = 0;
    sf->col = 0;
    sf->pos = 0;
    sf->lastc = '\n';
    return sf;
}

int string_feeder_get_line(string_feeder_t *sf)
{
    return sf->line;
}

int string_feeder_get_column(string_feeder_t *sf)
{
    return sf->col;
}

void string_feeder_destroy(string_feeder_t *sf)
{
    free(sf->s);
    free(sf);
}

int string_feeder_has_next(string_feeder_t *sf)
{
    return sf->s[sf->pos] != 0;
}

char string_feeder_next(string_feeder_t *sf)
{
    if (sf->lastc == '\n') {
        sf->line++;
        sf->col = 1;
    } else {
        sf->col ++;
    }

    sf->lastc = sf->s[sf->pos++];
    return sf->lastc;
}

char string_feeder_peek(string_feeder_t *sf)
{
    return sf->s[sf->pos];
}

////////////////////////////////////////////
int str_ends_with(const char *s, const char *needle)
{
    int lens = strlen(s);
    int lenneedle = strlen(needle);

    if (lenneedle > lens)
        return 0;

    return !strncmp(&s[lens - lenneedle], needle, lenneedle);
}

int str_starts_with(const char *s, const char *needle)
{
    int lenneedle = strlen(needle);

    return !strncmp(s, needle, lenneedle);
}

char *str_substring(const char *s, int startidx, int endidx)
{
    if (endidx < 0)
        endidx = strlen(s);

    int blen = endidx - startidx; // not counting \0
    char *b = malloc(blen + 1);
    memcpy(b, &s[startidx], blen);
    b[blen] = 0;
    return b;
}

char *str_replace(const char *haystack, const char *needle, const char *replacement)
{
    string_buffer_t *sb = string_buffer_create();
    int haystack_len = strlen(haystack);
    int needle_len = strlen(needle);

    int pos = 0;
    while (pos < haystack_len) {
        if (str_starts_with(&haystack[pos], needle)) {
            string_buffer_append_string(sb, replacement);
            pos += needle_len;
        } else {
            string_buffer_append(sb, haystack[pos]);
            pos++;
        }
    }

    char *res = string_buffer_to_string(sb);
    string_buffer_destroy(sb);
    return res;
}
