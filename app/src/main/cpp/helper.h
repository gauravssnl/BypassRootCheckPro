//
// Created by gauravssnl.
//

#pragma once

#include <string>
#include <jni.h>


jclass (*original_FindClass)(JNIEnv *env, const char *name);

FILE *(*original_fopen)(const char *filename, const char *mode);

FILE *fake_fopen(const char *filename, const char *mode);

jint (*RootBeerNative_setLogDebugMessages)(JNIEnv *env, jobject thiz, jboolean debug);

jint (*RootBeerNative_checkForMagiskUDS)(JNIEnv *env, jobject thiz);

jint (*RootBeerNative_checkForRoot)(JNIEnv *env, jobject thiz, jobjectArray pathsArray);

jint RootBeerNative_checkForMagiskUDS_Fake(JNIEnv *env, jobject thiz);

jint *RootBeerNative_checkForRoot_Fake(JNIEnv *env, jobject thiz, jobjectArray pathsArray);

jint RootBeerNative_setLogDebugMessages_Fake(JNIEnv *env, jobject thiz, jboolean);
