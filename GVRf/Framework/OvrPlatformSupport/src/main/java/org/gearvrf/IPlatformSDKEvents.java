package org.gearvrf;

/**
 * Oculus Platform SDK entitlement check events
 */
public interface IPlatformSDKEvents extends IEvents {
    /**
     * Called in case ovr_PlatformInitializeAndroid did not succeed. Will be called on the app's
     * main thread.
     * @param errorCode return value of ovr_PlatformInitializeAndroid;
     *                  one of the values listed in ovrPlatformInitializeResult
     */
    void onPlatformInitializeError(int errorCode);

    /**
     * Called when ovr_Entitlement_GetIsViewerEntitled() did not succeed. Will be called on the
     * app's main thread.
     */
    void onUserIsNotEntitled();
}