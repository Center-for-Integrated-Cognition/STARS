#ifndef _SSOCKET_H
#define _SSOCKET_H

#include <sys/types.h>
#include <sys/socket.h>
#include <stdio.h>

typedef struct ssocket ssocket_t;

struct ssocket
{
	int type;
	int socket;

	struct sockaddr addr;
	socklen_t addrlen;

};

ssocket_t *ssocket_create();
void ssocket_destroy(ssocket_t *s);

// returns < 0 on error
int ssocket_connect(ssocket_t *s, const char *hostname, int port);

int ssocket_disable_nagle(ssocket_t *s);
int ssocket_listen(ssocket_t *s, int port, int listenqueue, int localhostOnly);
ssocket_t *ssocket_accept(ssocket_t *s);
void ssocket_get_remote_ip(ssocket_t *s, int *ip);
int ssocket_get_fd(ssocket_t *s);

#endif
