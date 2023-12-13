#include <string.h>
#include <stdint.h>
#include <stdlib.h>
#include <inttypes.h>
#include <stdio.h>

#include "image_source.h"
#include "url_parser.h"

#include "common/varray.h"
#include "common/string_util.h"

void image_source_print_features(image_source_t *isrc);

image_source_t *image_source_open(const char *url)
{
    image_source_t *isrc = NULL;

    // get feature key/value pairs
    url_parser_t *urlp = url_parser_create(url);
    if (urlp == NULL) // bad URL format
        return NULL;

    const char *protocol = url_parser_get_protocol(urlp);
    const char *location = url_parser_get_location(urlp);

    if (!strcmp(protocol, "v4l2://")) {
        isrc = image_source_v4l2_open(location);
    } else if (!strcmp(protocol, "dc1394://")) {
        isrc = image_source_dc1394_open(urlp);
    } else if (!strcmp(protocol, "islog://")) {
        isrc = image_source_islog_open(urlp);
    } else if (!strcmp(protocol, "pgusb://")) {
        isrc = image_source_pgusb_open(urlp);
    }  else if (!strcmp(protocol, "file://")) {
        isrc = image_source_filedir_open(urlp);
    }  else if (!strcmp(protocol, "dir://")) {
        isrc = image_source_filedir_open(urlp);
    }

    // handle parameters
    if (isrc != NULL) {
        int found[url_parser_num_parameters(urlp)];

        for (int param_idx = 0; param_idx < url_parser_num_parameters(urlp); param_idx++) {
            const char *key = url_parser_get_parameter_name(urlp, param_idx);
            const char *value = url_parser_get_parameter_value(urlp, param_idx);

            if (!strcmp(key, "fidx")) {
                printf("image_source.c: set feature %30s = %15s\n", key, value);
                int fidx = atoi(url_parser_get_parameter(urlp, "fidx", "0"));
                printf("SETTING fidx %d\n", fidx);
                isrc->set_format(isrc, fidx);
                found[param_idx] = 1;
                continue;
            }

            if (!strcmp(key, "format")) {
                printf("image_source.c: set feature %30s = %15s\n", key, value);
                isrc->set_named_format(isrc, value);
                found[param_idx] = 1;
                continue;
            }

            if (!strcmp(key, "print")) {
                image_source_print_features(isrc);
                continue;
            }

            // pass through a device-specific parameter.
            for (int feature_idx = 0; feature_idx < isrc->num_features(isrc); feature_idx++) {

                if (!strcmp(isrc->get_feature_name(isrc, feature_idx), key)) {
                    char *endptr = NULL;
                    double dv = strtod(value, &endptr);
                    if (endptr != value + strlen(value)) {
                        printf("Parameter for key '%s' is invalid. Must be a number.\n",
                               isrc->get_feature_name(isrc, feature_idx));
                        goto cleanup;
                    }

                    int res = isrc->set_feature_value(isrc, feature_idx, dv);
                    if (res != 0)
                        printf("Error setting feature: key %s value %s, error code %d\n",
                               key, value, res);

                    double setvalue = isrc->get_feature_value(isrc, feature_idx);
                    printf("image_source.c: set feature %30s = %15s (double %12.6f). Actually set to %8.3f\n", key, value, dv, setvalue);

                    found[param_idx] = 1;
                    break;
                }
            }
        }

        for (int param_idx = 0; param_idx < url_parser_num_parameters(urlp); param_idx++) {
            if (found[param_idx] != 1) {
                const char *key = url_parser_get_parameter_name(urlp, param_idx);
                const char *value = url_parser_get_parameter_value(urlp, param_idx);

                printf("Parameter not found. Key: %s Value: %s\n", key, value);
            }
        }
    }

cleanup:
    url_parser_destroy(urlp);

    return isrc;
}

char** image_source_enumerate()
{
    char **urls = calloc(1, sizeof(char*));

    urls = image_source_enumerate_v4l2(urls);
    urls = image_source_enumerate_dc1394(urls);
    urls = image_source_enumerate_pgusb(urls);

    return urls;
}

void image_source_enumerate_free(char **urls)
{
    if (urls == NULL)
        return;

    for (int i = 0; urls[i] != NULL; i++)
        free(urls[i]);
    free(urls);
}

void image_source_print_features(image_source_t *isrc)
{
    printf("Features:\n");

    int n = isrc->num_features(isrc);
    for (int i = 0; i < n; i++) {

        const char *name = isrc->get_feature_name(isrc, i);
        const char *type = isrc->get_feature_type(isrc, i);
        double value = isrc->get_feature_value(isrc, i);

        printf("    %-30s : ", name);

        if (type[0] == 'b') {
            printf("Boolean %20s", ((int) value) ? "True" : "False");
        } else if (type[0] == 'i') {

            varray_t *tokens = str_split(type, ",");

            if (varray_size(tokens) == 3 || varray_size(tokens) == 4) {
                char *min = varray_get(tokens, 1);
                char *max = varray_get(tokens, 2);
                char *inc = "1";
                if (varray_size(tokens) == 4)
                    inc = varray_get(tokens, 3);

                printf("Int     %20i Min %20s Max %20s Inc %20s", (int) value, min, max, inc);
            }

            varray_map(tokens, free);
            varray_destroy(tokens);

        } else if (type[0] == 'f') {

            varray_t *tokens = str_split(type, ",");

            if (varray_size(tokens) == 3 || varray_size(tokens) == 4) {
                char *min = varray_get(tokens, 1);
                char *max = varray_get(tokens, 2);
                char *inc = NULL;
                if (varray_size(tokens) == 4)
                    inc = varray_get(tokens, 3);

                printf("Float   %20.15f Min %20s Max %20s", value, min, max);
                if (inc) printf("Inc %20s", inc);
            }

            varray_map(tokens, free);
            varray_destroy(tokens);

        } else if (type[0] == 'c') {

            varray_t *tokens = str_split(type, ",");

            printf("Enum    %20i (", (int) value);

            for (int i = 1; i < varray_size(tokens); i++)
                printf("%s%s", (char*) varray_get(tokens, i), (i+1 == varray_size(tokens)) ? "" : ", ");

            printf(")");

            varray_map(tokens, free);
            varray_destroy(tokens);
        }

        printf("\n");
    }
}

