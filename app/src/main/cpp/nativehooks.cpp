#include "helper.h"
#include "hooker.h"
#include "logger.h"

#include <dlfcn.h>

static HookFunType hook_func = nullptr;

void on_library_loaded(const char *name, void *handle) {
    if (strstr(name, "libtool-checker.so") || strstr(name, "libtoolChecker.so")) {
        LOGI("Trying to hook libtool Checker library :: %s", name);
        // Root Beer Fresh hooks
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

        LOGI("All native hooks for root bypass done. Enjoy :)");
    }
}

// Note : native_init is mandatory; do not change signature
extern "C" [[gnu::visibility("default")]] [[gnu::used]]
NativeOnModuleLoaded native_init(const NativeAPIEntries *entries) {
    hook_func = entries->hook_func;
    return on_library_loaded;
}

jint RootBeerNative_checkForMagiskUDS_Fake(JNIEnv *env, jobject thiz) {
    LOGI("Inside checkForMagiskUDS_Fake");
    return 0;
}

jint *RootBeerNative_checkForRoot_Fake(JNIEnv *env, jobject thiz, jobjectArray pathsArray) {
    LOGI("Inside RootBeerNative_checkForRoot_Fake");
    return 0;
}

jint RootBeerNative_setLogDebugMessages_Fake(JNIEnv *env, jobject thiz, jboolean) {
    LOGI("Inside RootBeerNative_setLogDebugMessages_Fake");
    // we want to enable DEBUG flag to see our hooks
    auto res = RootBeerNative_setLogDebugMessages(env, thiz, 1);
    LOGI("Debug flag set for RootBeerNative_setLogDebugMessages");
    return res;
}