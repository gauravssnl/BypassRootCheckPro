//
// Created by gauravssnl.
//

#pragma once

#include <string>
#include <jni.h>

const std::string root_related_files[] = {"magisk", "su", "busybox", "Superuser", "SuperSU",
                                          "daemonsu", "/data/adb/magisk.db",
                                          "/data/adb/.boot_count", "/proc/net/unix"};

const std::string root_related_dirs[] = {"/data/adb/modules", "/data/adb/lspd", "/data/adb/shamiko",
                                         "/data/adb/sui", "/data/adb/storage-isolation",
                                         "/data/adb/appops", "/data/adb/ksud", "data/system/xlua"};

FILE *(*original_fopen)(const char *filename, const char *mode);

FILE *fake_fopen(const char *filename, const char *mode);

int (*original_stat)(const char *, struct stat *);

int fake_stat(const char *, struct stat *);

int (*original_lstat)(const char *path, struct stat *buf);

int fake_lstat(const char *path, struct stat *buf);

jint (*RootBeerNative_setLogDebugMessages)(JNIEnv *env, jobject thiz, jboolean debug);

jint (*RootBeerNative_checkForMagiskUDS)(JNIEnv *env, jobject thiz);

jint (*RootBeerNative_checkForRoot)(JNIEnv *env, jobject thiz, jobjectArray pathsArray);

jint RootBeerNative_checkForMagiskUDS_Fake(JNIEnv *env, jobject thiz);

jint *RootBeerNative_checkForRoot_Fake(JNIEnv *env, jobject thiz, jobjectArray pathsArray);

jint RootBeerNative_setLogDebugMessages_Fake(JNIEnv *env, jobject thiz, jboolean);

jboolean (*tbsafetychecker_appdetector_AbnormalEnvironment_detectDual)(JNIEnv *env, jobject thiz);

jboolean (*tbsafetychecker_appdetector_AbnormalEnvironment_detectXposed)(JNIEnv *env, jobject thiz);

jint (*tbsafetychecker_appdetector_FileDetection_nativeDetect)(JNIEnv *, jclass, jstring, jboolean);

jboolean tbsafetychecker_appdetector_AbnormalEnvironment_detectDual_Fake (JNIEnv *env, jobject thiz);

jboolean tbsafetychecker_appdetector_AbnormalEnvironment_detectXposed_Fake(JNIEnv *env, jobject thiz);

jint tbsafetychecker_appdetector_FileDetection_nativeDetect_Fake(JNIEnv *, jclass, jstring, jboolean);

bool is_file_related_to_root(const char *filename);

bool filepath_equals_or_ends_with(std::string filepath, std::string pattern);

bool string_equals_any(std::string name, std::string patterns[]);
