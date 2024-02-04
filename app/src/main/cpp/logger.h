//
// Created by gauravssnl on 05/02/24.
//

#pragma once

#include <android/log.h>

#define LOGI(...) (__android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__))
#define LOGW(...) (__android_log_print(ANDROID_LOG_WARN, TAG, __VA_ARGS__))
#define LOGE(...) (__android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__))

#ifndef NDEBUG
#define LOGV(...) (__android_log_print(ANDROID_LOG_VERBOSE, TAG, __VA_ARGS__))
#define LOGD(...) (__android_log_print(ANDROID_LOG_DEBUG, TAG, __VA_ARGS__))
#else
#define LOGV(...) ((void)0)
#define LOGD(...) ((void)0)
#endif

#define TAG "NativeHookerByGauravssnl"
