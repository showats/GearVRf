package org.gearvrf;

/**
 * Oculus Platform SDK entitlement check events
 */
public interface IPlatformSDKEvents extends IEvents {
    /**
     * Called when ovr_Entitlement_GetIsViewerEntitled() did not succeed. Will be called on the
     * app's main thread.
     */
    void onUserIsNotEntitled();
}