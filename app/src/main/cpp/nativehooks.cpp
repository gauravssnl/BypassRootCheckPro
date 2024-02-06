#include "helper.h"
#include "hooker.h"
#include "logger.h"

#include <dlfcn.h>
#include <sys/stat.h>

static HookFunType hook_func = nullptr;

void on_library_loaded(const char *name, void *handle) {
    if (strstr(name, "libtool-checker.so") || strstr(name, "libtoolChecker.so")) {
        LOGI("Trying to hook libtool Checker library :: %s", name);
        // Root Beer Fresh & Root Beer hooks
        void *target1 = dlsym(handle,"Java_com_kimchangyoun_rootbeerFresh_RootBeerNative_checkForMagiskUDS");
        if (target1)
            hook_func(target1, (void *) RootBeerNative_checkForMagiskUDS_Fake,
                      (void **) &RootBeerNative_checkForMagiskUDS);
        void *target2 = dlsym(handle,"Java_com_kimchangyoun_rootbeerFresh_RootBeerNative_checkForRoot");
        if (target2)
            hook_func(target2, (void *) RootBeerNative_checkForRoot_Fake,
                      (void **) &RootBeerNative_checkForRoot);
        void *target3 = dlsym(handle,"Java_com_kimchangyoun_rootbeerFresh_RootBeerNative_setLogDebugMessages");
        if (target1)
            hook_func(target3, (void *) RootBeerNative_setLogDebugMessages_Fake,
                      (void **) &RootBeerNative_setLogDebugMessages);
        // Root Beer hooks
        void *target4 = dlsym(handle,"Java_com_scottyab_rootbeer_RootBeerNative_checkForRoot");
        if (target4)
            hook_func(target4, (void *) RootBeerNative_checkForRoot_Fake,
                      (void **) &RootBeerNative_checkForRoot);
        void *target5 = dlsym(handle,"Java_com_scottyab_rootbeer_RootBeerNative_setLogDebugMessages");
        if (target5)
            hook_func(target1, (void *) RootBeerNative_setLogDebugMessages_Fake,
                      (void **) &RootBeerNative_setLogDebugMessages);
        LOGI("All native hooks applied for RootBeer / RootBeer Fresh :)");
    }
}

// Note : native_init is mandatory; do not change signature
extern "C" [[gnu::visibility("default")]] [[gnu::used]]
NativeOnModuleLoaded native_init(const NativeAPIEntries *entries) {
    hook_func = entries->hook_func;
    // System Hooks
    hook_func((void *) fopen, (void *) fake_fopen, (void **) &original_fopen);
    hook_func((void *) stat, (void *) fake_stat, (void **) &original_stat);
    hook_func((void *) lstat, (void *) fake_lstat, (void **) &original_lstat);
    LOGI("All System hooks applied for bypassing root check.)");
    return on_library_loaded;
}

FILE *fake_fopen(const char *filename, const char *mode) {
    LOGD("Inside fake_fopen. Filename :: %s", filename);
    if (is_file_related_to_root(filename)) {
        LOGD("App tried to check root related files, so bypass it");
        return nullptr;
    }
    return original_fopen(filename, mode);
}

int fake_stat(const char *filename, struct stat *file_info) {
    LOGD("Inside fake_stat. Filename :: %s ", filename);
    if (is_file_related_to_root(filename)) {
        LOGD("App tried to check root related files, so bypass it");
        return -1;
    }
    return original_stat(filename, file_info);
}

bool is_file_related_to_root(const char *filename) {
    if (strstr(filename, "su") || strstr(filename, "/proc/net/unix") ||
        strstr(filename, "magisk") || strstr(filename, "busybox") ||
        strstr(filename, "/data/adb/.boot_count") || strstr(filename, "Superuser") ||
        strstr(filename, "daemonsu") || strstr(filename, "SuperSU"))
        return true;
    return false;
}

int fake_lstat(const char *pathName, struct stat *buf) {
    LOGD("Inside fake_lstat. Filename :: %s ", pathName);
    if (is_file_related_to_root(pathName)) return -1;
    return original_lstat(pathName, buf);
}

jint RootBeerNative_checkForMagiskUDS_Fake(JNIEnv *env, jobject thiz) {
    LOGD("Inside checkForMagiskUDS_Fake");
    return 0;
}

jint *RootBeerNative_checkForRoot_Fake(JNIEnv *env, jobject thiz, jobjectArray pathsArray) {
    LOGD("Inside RootBeerNative_checkForRoot_Fake");
    return 0;
}

jint RootBeerNative_setLogDebugMessages_Fake(JNIEnv *env, jobject thiz, jboolean) {
    LOGD("Inside RootBeerNative_setLogDebugMessages_Fake");
    // we want to enable DEBUG flag to see our hooks
    auto res = RootBeerNative_setLogDebugMessages(env, thiz, 1);
    LOGD("Debug flag set for RootBeerNative_setLogDebugMessages");
    return res;
}
