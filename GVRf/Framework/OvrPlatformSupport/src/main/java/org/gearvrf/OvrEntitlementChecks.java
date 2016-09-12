package org.gearvrf;

import org.gearvrf.utility.Log;

public final class OvrEntitlementChecks {

    public OvrEntitlementChecks(final GVRContext activity, final String appId) {
        mAppId = appId;
        mActivity = activity;

        create(appId);
    }

    /**
     * Called from native code
     *
     * @param error
     */
    private void onPlatformInitializeError(int error) {
        mPlatformInitializeError = error;
    }

    /**
     * Called from native code
     */
    private void onUserIsNotEntitled() {
        final Runnable r = new Runnable() {
            @Override
            public void run() {
                final GVRViewManager viewManager = mActivity.getViewManager();
                if (null == viewManager) {
                    throw new IllegalStateException("Fatal error; viewManager not available!");
                }
                try {
                    viewManager.getEventManager().sendEvent(
                            mActivity,
                            IPlatformSDKEvents.class,
                            "onUserIsNotEntitled");

                } catch (final Exception exc) {
                    Log.e(TAG, "Exception in user event handler (onUserIsNotEntitled): %s", exc);
                }

            }

        };
        mActivity.runOnUiThread(r);
    }

    private final String mAppId;
    private final GVRActivity mActivity;
    private int mPlatformInitializeError;

    private static final String TAG = "OvrEntitlementChecks";

    private native long create(String appId);
}
