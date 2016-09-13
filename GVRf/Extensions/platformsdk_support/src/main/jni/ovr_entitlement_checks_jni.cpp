#include <jni.h>

#include "OVR_Platform.h"

#include "util/gvr_log.h"


extern "C" {

JNIEXPORT jint JNICALL
Java_org_gearvrf_OvrEntitlementChecks_create(JNIEnv *jni, jclass clazz, jobject activity, jstring appId) {

    const ovrPlatformInitializeResult result = ovr_PlatformInitializeAndroid(
            jni->GetStringUTFChars(appId, 0), activity, jni);

    if (ovrPlatformInitialize_Success != result) {
        LOGE("ovr_PlatformInitializeAndroid failed with error %d", result);
    } else {
        LOGI("entitlement check enabled!");
        ovr_Entitlement_GetIsViewerEntitled();
    }

    return result;
}

/**
 * @return 0 if indeterminate (keep on checking), -1 on failure, 1 on success
 */
JNIEXPORT jint JNICALL
Java_org_gearvrf_OvrEntitlementChecks_processEntitlementCheckResponse(JNIEnv *jni, jclass clazz) {

    ovrMessage *response = ovr_PopMessage();
    if (response) {
        int messageType = ovr_Message_GetType(response);
        if (messageType == ovrMessage_Entitlement_GetIsViewerEntitled) {
            if (ovr_Message_IsError(response) != 0) {
                LOGI("entitlement check for user failed");
                return -1;
            } else {
                return 1;
            }
        }
    }

    return 0;
}

}

