#include <jni.h>
#include "OVR_Platform.h"


bool enableEntitlementCheck(JNIEnv &env, jobject activity, const char *appId) const {
    const ovrPlatformInitializeResult result = ovr_PlatformInitializeAndroid(appId, activity_,
                                                                             &env);
    if (ovrPlatformInitialize_Success != result) {
        LOGE("ovr_PlatformInitializeAndroid failed with error %d", result);
        jmethodID mid = GetMethodId(env, activityClass_, "onPlatformInitializeError", "(I)V");
        env.CallVoidMethod(caller, mid, result);
    }
    return ovrPlatformInitialize_Success == result;
}


extern "C" {

JNIEXPORT void JNICALL
Java_org_gearvrf_OvrEntitlementChecks_onCreate(JNIEnv *jni, jclass clazz,
                                               jobject caller, jstring appId) {
    if (enableEntitlementCheck(env, caller, env.GetStringUTFChars(appId, 0))) {
        LOGI("entitlement check enabled!");
        ovr_Entitlement_GetIsViewerEntitled();
    }

}

}

