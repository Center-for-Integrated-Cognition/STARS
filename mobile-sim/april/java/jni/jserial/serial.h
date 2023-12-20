#ifndef _SERIAL_H
#define _SERIAL_H

/** Creates a basic fd, setting baud to 9600, raw data i/o (no flow
    control, no fancy character handling. Configures it for blocking
    reads.  8 data bits, 1 stop bit, no parity.

    Returns the fd, -1 on error
**/
int serial_open(const char *port, int baud, int blocking);

/** Set the baud rate, where the baudrate is just the integer value
    desired.

    Returns non-zero on error.
**/
int serial_set_baud(int fd, int baudrate);

/** Enable cts/rts flow control. 
    Returns non-zero on error. 
**/
int serial_set_ctsrts(int fd, int enable);
/** Enable xon/xoff flow control. 
    Returns non-zero on error. 
**/
int serial_set_xon(int fd, int enable);

/**
    databits = [5, 8]
    parity = 0: none, 1: odd, 2: even
    stopbits = [1, 2]
 **/
int serial_set_mode(int fd, int databits, int parity, int stopbits);

/** Set the port to 8 data bits, 2 stop bits, no parity.
    Returns non-zero on error.
 **/
int serial_set_N82(int fd);

int serial_close(int fd);

int serial_set_dtr(int fd, int v);
int serial_set_rts(int fd, int v);

#endif
