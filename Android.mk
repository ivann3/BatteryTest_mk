LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_SRC_FILES := $(call all-subdir-java-files)

LOCAL_PACKAGE_NAME := BatteryTest

LOCAL_CERTIFICATE := platform

CAL_RESOURCE_DIR += $(LOCAL_PATH)/res

#输入第三方jar包的别名
LOCAL_STATIC_JAVA_LIBRARIES := \
        android-support-v4 \
        android-support-v7-appcompat \
		achartengine \
        
include $(BUILD_PACKAGE)

###################################################
include $(CLEAR_VARS)
#冒号前面为jar别名，后面为jar文件的实际路径
LOCAL_PREBUILT_STATIC_JAVA_LIBRARIES := achartengine:/libs/achartengine-1.1.0.jar \

include $(BUILD_MULTI_PREBUILT)
###################################################

include $(call all-makefiles-under,$(LOCAL_PATH))

