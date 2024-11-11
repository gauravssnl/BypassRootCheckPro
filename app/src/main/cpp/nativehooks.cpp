#include "helper.h"
#include "hooker.h"
#include "logger.h"

#include <dlfcn.h>
#include <sys/stat.h>

static HookFunType hook_func = nullptr;

void on_library_loaded(const char *name, void *handle) {
    LOGI("Library name : %s", name);
    auto patterns1 = new std::string[]{"libtool-checker.so", "libtool_checker.so",
                                       "libtoolChecker.so"};
    if (string_equals_any(std::string(name), patterns1)) {
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

    auto patterns2  = new std::string[]{"libapplist_detector.so", "libapplist-detector.so",
                "libapplistDetector.so"};
    if (string_equals_any(std::string(name),patterns2)) {
        LOGI("Trying to hook lib app list detector library :: %s", name);
        void *target1 = dlsym(handle,"Java_krypton_tbsafetychecker_appdetector_AbnormalEnvironment_detectDual");
        if (target1)
            hook_func(target1, (void *) tbsafetychecker_appdetector_AbnormalEnvironment_detectDual,
                      (void **) &tbsafetychecker_appdetector_AbnormalEnvironment_detectDual_Fake);
        void *target2 = dlsym(handle,"Java_krypton_tbsafetychecker_appdetector_AbnormalEnvironment_detectXposed");
        if (target2)
            hook_func(target2, (void *) tbsafetychecker_appdetector_AbnormalEnvironment_detectXposed,
                      (void **) &tbsafetychecker_appdetector_AbnormalEnvironment_detectXposed_Fake);
        void *target3 = dlsym(handle,"Java_krypton_tbsafetychecker_appdetector_FileDetection_nativeDetect");
        if (target3)
            hook_func(target3, (void *) tbsafetychecker_appdetector_FileDetection_nativeDetect,
                      (void **) &tbsafetychecker_appdetector_FileDetection_nativeDetect_Fake);
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
        LOGI("App tried to check root related files, so bypass it");
        return nullptr;
    }
    return original_fopen(filename, mode);
}

int fake_stat(const char *filename, struct stat *file_info) {
    LOGD("Inside fake_stat. Filename :: %s ", filename);
    if (is_file_related_to_root(filename)) {
        LOGI("App tried to check root related files, so bypass it");
        return -1;
    }
    return original_stat(filename, file_info);
}

bool is_file_related_to_root(const char *filepath) {
    for (const std::string &item: root_related_dirs) {
        if (std::string(filepath).starts_with(item)) {
            return true;
        }
    }

    for (const std::string &item: root_related_files) {
        if (filepath_equals_or_ends_with(std::string(filepath), item)) {
            return true;
        }
    }
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

jboolean tbsafetychecker_appdetector_AbnormalEnvironment_detectDual_Fake(JNIEnv *env, jobject thiz) {
        LOGD("Inside tbsafetychecker_appdetector_AbnormalEnvironment_detectDual_Fake");
    return 0;
}

jboolean tbsafetychecker_appdetector_AbnormalEnvironment_detectXposed_Fake(JNIEnv *env, jobject thiz) {
    LOGD("Inside tbsafetychecker_appdetector_AbnormalEnvironment_detectXposed_Fake");
    return 0;
}

jint tbsafetychecker_appdetector_FileDetection_nativeDetect_Fake(JNIEnv *env, jclass clazz, jstring str, jboolean flag) {
    LOGD("Inside tbsafetychecker_appdetector_FileDetection_nativeDetect_Fake");
    LOGD("Params passed are :: %s and  % d ", env->GetStringUTFChars(str, NULL), flag);
    return -1;
}

bool filepath_equals_or_ends_with(std::string filepath, std::string pattern) {
    return filepath == pattern || filepath.ends_with(pattern);
}

bool string_equals_any(std::string name, std::string patterns[]) {
    bool  result = false;
    for(auto index = 0; index < patterns->length(); index++) {
        if (name == patterns[index]) {
            result = true;
            break;
        }
    }
    return result;
}
