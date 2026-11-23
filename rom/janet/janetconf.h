#ifndef JANETCONF_H
#define JANETCONF_H
#define JANET_VERSION_MAJOR 1
#define JANET_VERSION_MINOR 41
#define JANET_VERSION_PATCH 2
#define JANET_VERSION_EXTRA ""
#define JANET_VERSION "1.41.2"
#define JANET_NO_DYNAMIC_MODULES
#define JANET_NO_THREADS
#define JANET_NO_EV
#define JANET_NO_NET
#define JANET_NO_PROCESSES
#define JANET_NO_FFI
#define JANET_NO_SYMLINKS
#define JANET_NO_UMASK
#define JANET_NO_REALPATH
#define JANET_SINGLE_THREADED
#define JANET_NO_RETURN
#define JANET_REDUCED_OS
/* On the computer (not in the host build that makes the boot image): running out of memory says more (syscalls.c) */
#ifdef EA_ROM
void ea_out_of_memory(const char *file, int line) __attribute__((noreturn));
#define JANET_OUT_OF_MEMORY do { ea_out_of_memory(__FILE__, __LINE__); } while (0)
#endif
#endif
