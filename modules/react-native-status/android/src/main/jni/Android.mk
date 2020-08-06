LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE := status
LOCAL_EXPORT_C_INCLUDES := $(LOCAL_PATH)

LOCAL_SRC_FILES := status.c libstatus.a libnim_status.a
include $(PREBUILT_STATIC_LIBRARY)
