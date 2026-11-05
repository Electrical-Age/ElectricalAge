/* Files on the floppies: newlib's file system calls over FatFs (A: and B:, FAT12), and the disk/... natives. */
#include <errno.h>
#include <fcntl.h>
#include <stdint.h>
#include <stdlib.h>
#include <string.h>
#include <sys/stat.h>
#include "ff.h"
#include "janet.h"
#include "../common/devices.h"

#define MAX_FILES 16
static FATFS fs[2];
static int mounted[2];
static FIL *files[MAX_FILES];      /* fd 3 + i */
static FRESULT last;

static const char *const vol[2] = {"A:", "B:"};

/* The drive a path is on: an "a:" or "b:" prefix, else A. */
static int drive_of(const char *path) {
    if (path[0] && path[1] == ':') {
        char c = path[0] | 0x20;
        if (c == 'b') return 1;
    }
    return 0;
}

/* Mount the drive's disk if it changed or isn't mounted yet. */
static FRESULT ready(int d) {
    if (FD_CHANGED & (1u << d)) {
        FD_CHANGED = 1u << d;
        if (mounted[d]) f_mount(NULL, vol[d], 0);
        mounted[d] = 0;
    }
    if (mounted[d]) return FR_OK;
    FRESULT r = f_mount(&fs[d], vol[d], 1);
    if (r == FR_OK) mounted[d] = 1;
    return r;
}

static int fail(FRESULT r) {
    last = r;
    switch (r) {
        case FR_NO_FILE: case FR_NO_PATH: case FR_INVALID_NAME: errno = ENOENT; break;
        case FR_DENIED: case FR_EXIST: errno = EEXIST; break;
        case FR_WRITE_PROTECTED: errno = EROFS; break;
        case FR_NOT_READY: errno = ENODEV; break;
        case FR_NO_FILESYSTEM: errno = ENODEV; break;
        case FR_TOO_MANY_OPEN_FILES: errno = EMFILE; break;
        default: errno = EIO; break;
    }
    return -1;
}

static FRESULT prepare(const char *path) {
    FRESULT r = ready(drive_of(path));
    last = r;
    return r;
}

int _open(const char *path, int flags, int mode) {
    (void) mode;
    FRESULT r = prepare(path);
    if (r != FR_OK) return fail(r);
    int i = 0;
    while (i < MAX_FILES && files[i]) i++;
    if (i == MAX_FILES) { errno = EMFILE; return -1; }
    BYTE m = 0;
    int acc = flags & O_ACCMODE;
    if (acc == O_RDONLY || acc == O_RDWR) m |= FA_READ;
    if (acc == O_WRONLY || acc == O_RDWR) m |= FA_WRITE;
    if (flags & O_APPEND) m |= FA_OPEN_APPEND;
    else if ((flags & O_CREAT) && (flags & O_TRUNC)) m |= FA_CREATE_ALWAYS;
    else if (flags & O_CREAT) m |= FA_OPEN_ALWAYS;
    else m |= FA_OPEN_EXISTING;
    FIL *f = malloc(sizeof(FIL));
    if (!f) { errno = ENOMEM; return -1; }
    r = f_open(f, path, m);
    if (r != FR_OK) { free(f); return fail(r); }
    files[i] = f;
    return 3 + i;
}

static FIL *file(int fd) {
    return fd >= 3 && fd < 3 + MAX_FILES ? files[fd - 3] : NULL;
}

int file_read(int fd, char *buf, int n) {
    FIL *f = file(fd);
    if (!f) { errno = EBADF; return -1; }
    UINT got;
    FRESULT r = f_read(f, buf, (UINT) n, &got);
    return r == FR_OK ? (int) got : fail(r);
}

int file_write(int fd, const char *buf, int n) {
    FIL *f = file(fd);
    if (!f) { errno = EBADF; return -1; }
    UINT put;
    FRESULT r = f_write(f, buf, (UINT) n, &put);
    return r == FR_OK ? (int) put : fail(r);
}

int _close(int fd) {
    FIL *f = file(fd);
    if (!f) return fd >= 0 && fd <= 2 ? 0 : (errno = EBADF, -1);
    FRESULT r = f_close(f);
    free(f);
    files[fd - 3] = NULL;
    return r == FR_OK ? 0 : fail(r);
}

int _lseek(int fd, int off, int whence) {
    FIL *f = file(fd);
    if (!f) { errno = ESPIPE; return -1; }
    FSIZE_t to = whence == SEEK_SET ? (FSIZE_t) off : whence == SEEK_CUR ? f_tell(f) + off : f_size(f) + off;
    FRESULT r = f_lseek(f, to);
    return r == FR_OK ? (int) f_tell(f) : fail(r);
}

int _fstat(int fd, struct stat *st) {
    memset(st, 0, sizeof *st);
    if (fd >= 0 && fd <= 2) { st->st_mode = S_IFCHR; return 0; }
    FIL *f = file(fd);
    if (!f) { errno = EBADF; return -1; }
    st->st_mode = S_IFREG | 0666;
    st->st_size = (off_t) f_size(f);
    return 0;
}

int _isatty(int fd) { return fd >= 0 && fd <= 2; }

int _stat(const char *path, struct stat *st) {
    FRESULT r = prepare(path);
    if (r != FR_OK) return fail(r);
    FILINFO fi;
    memset(st, 0, sizeof *st);
    /* the root has no directory entry */
    size_t n = strlen(path);
    if ((n == 2 && path[1] == ':') || (n == 3 && path[1] == ':' && path[2] == '/') || (n == 1 && path[0] == '/')) {
        st->st_mode = S_IFDIR | 0777;
        return 0;
    }
    r = f_stat(path, &fi);
    if (r != FR_OK) return fail(r);
    st->st_mode = (fi.fattrib & AM_DIR) ? S_IFDIR | 0777 : S_IFREG | 0666;
    st->st_size = (off_t) fi.fsize;
    return 0;
}

int _unlink(const char *path) {
    FRESULT r = prepare(path);
    if (r == FR_OK) r = f_unlink(path);
    return r == FR_OK ? 0 : fail(r);
}

int _rename(const char *a, const char *b) {
    FRESULT r = prepare(a);
    if (r == FR_OK) r = f_rename(a, b);
    return r == FR_OK ? 0 : fail(r);
}

int mkdir(const char *path, mode_t mode) {
    (void) mode;
    FRESULT r = prepare(path);
    if (r == FR_OK) r = f_mkdir(path);
    return r == FR_OK ? 0 : fail(r);
}

/* ---- disk/... natives */

static const char *message(FRESULT r) {
    switch (r) {
        case FR_OK: return "ok";
        case FR_NOT_READY: return "no disk in the drive";
        case FR_NO_FILESYSTEM: return "the disk isn't formatted (disk/format it)";
        case FR_WRITE_PROTECTED: return "the disk is write-protected";
        case FR_NO_FILE: return "no such file";
        case FR_NO_PATH: return "no such directory";
        case FR_EXIST: return "it already exists";
        case FR_DENIED: return "denied (the disk is full, or a directory isn't empty)";
        case FR_INVALID_NAME: return "not a valid name";
        default: return "disk error";
    }
}

static int getdrive(const Janet *argv, int32_t n) {
    JanetKeyword k = janet_getkeyword(argv, n);
    if ((k[0] | 0x20) == 'a' && k[1] == 0) return 0;
    if ((k[0] | 0x20) == 'b' && k[1] == 0) return 1;
    janet_panic("drives are :a and :b");
    return 0;
}

#define CHECK(r) do { FRESULT r_ = (r); if (r_ != FR_OK) janet_panic(message(r_)); } while (0)

JANET_FN(cfun_disk_format, "(ea/disk-format drive &opt label)", "Format the disk in drive (:a or :b), FAT12; erases it.") {
    janet_arity(argc, 1, 2);
    int d = getdrive(argv, 0);
    if (mounted[d]) f_mount(NULL, vol[d], 0);
    mounted[d] = 0;
    static BYTE work[FF_MAX_SS];
    MKFS_PARM opt = {FM_FAT | FM_SFD, 0, 0, 0, 0};
    CHECK(f_mkfs(vol[d], &opt, work, sizeof work));
    CHECK(ready(d));
    if (argc > 1) {
        const char *l = janet_getcstring(argv, 1);
        char buf[16];
        snprintf(buf, sizeof buf, "%s%.11s", vol[d], l);
        CHECK(f_setlabel(buf));
    }
    return janet_wrap_nil();
}

JANET_FN(cfun_disk_label, "(ea/disk-label drive)", "The disk's label, or nil.") {
    janet_fixarity(argc, 1);
    int d = getdrive(argv, 0);
    CHECK(ready(d));
    char l[24];
    CHECK(f_getlabel(vol[d], l, NULL));
    return l[0] ? janet_cstringv(l) : janet_wrap_nil();
}

JANET_FN(cfun_disk_free, "(ea/disk-free drive)", "Bytes free on the disk.") {
    janet_fixarity(argc, 1);
    int d = getdrive(argv, 0);
    CHECK(ready(d));
    DWORD clusters;
    FATFS *f;
    CHECK(f_getfree(vol[d], &clusters, &f));
    return janet_wrap_number((double) clusters * f->csize * 512);
}

JANET_FN(cfun_disk_list, "(ea/disk-list path)", "The names in a directory (directories end in /).") {
    janet_fixarity(argc, 1);
    const char *path = janet_getcstring(argv, 0);
    CHECK(prepare(path));
    DIR dir;
    FILINFO fi;
    CHECK(f_opendir(&dir, path));
    JanetArray *a = janet_array(8);
    for (;;) {
        FRESULT r = f_readdir(&dir, &fi);
        if (r != FR_OK) { f_closedir(&dir); janet_panic(message(r)); }
        if (!fi.fname[0]) break;
        if (fi.fattrib & (AM_HID | AM_SYS)) continue;
        char name[FF_LFN_BUF + 2];
        snprintf(name, sizeof name, "%s%s", fi.fname, (fi.fattrib & AM_DIR) ? "/" : "");
        janet_array_push(a, janet_cstringv(name));
    }
    f_closedir(&dir);
    return janet_wrap_array(a);
}

JANET_FN(cfun_disk_delete, "(ea/disk-delete path)", "Delete a file (or an empty directory).") {
    janet_fixarity(argc, 1);
    const char *path = janet_getcstring(argv, 0);
    CHECK(prepare(path));
    CHECK(f_unlink(path));
    return janet_wrap_nil();
}

JANET_FN(cfun_disk_rename, "(ea/disk-rename from to)", "Rename or move a file on its disk.") {
    janet_fixarity(argc, 2);
    const char *a = janet_getcstring(argv, 0), *b = janet_getcstring(argv, 1);
    CHECK(prepare(a));
    CHECK(f_rename(a, b));
    return janet_wrap_nil();
}

JANET_FN(cfun_disk_mkdir, "(ea/disk-mkdir path)", "Make a directory.") {
    janet_fixarity(argc, 1);
    const char *path = janet_getcstring(argv, 0);
    CHECK(prepare(path));
    CHECK(f_mkdir(path));
    return janet_wrap_nil();
}

JANET_FN(cfun_disk_why, "(ea/disk-why)", "What the last file call that failed ran into, in words.") {
    janet_fixarity(argc, 0);
    (void) argv;
    return janet_cstringv(message(last));
}

void ea_register_disk(JanetTable *env) {
    JanetRegExt fns[] = {
        JANET_REG("ea/disk-format", cfun_disk_format), JANET_REG("ea/disk-label", cfun_disk_label),
        JANET_REG("ea/disk-free", cfun_disk_free), JANET_REG("ea/disk-list", cfun_disk_list),
        JANET_REG("ea/disk-delete", cfun_disk_delete), JANET_REG("ea/disk-rename", cfun_disk_rename),
        JANET_REG("ea/disk-mkdir", cfun_disk_mkdir), JANET_REG("ea/disk-why", cfun_disk_why), JANET_REG_END};
    janet_cfuns_ext(env, "ea", fns);
}
