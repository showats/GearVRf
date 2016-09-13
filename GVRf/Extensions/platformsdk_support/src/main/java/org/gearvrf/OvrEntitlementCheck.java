package org.gearvrf;

import android.app.Activity;

import org.gearvrf.utility.Log;

public final class OvrEntitlementCheck {

    /**
     * @throws IllegalStateException in case the platform sdk cannot be initialized
     */
    public static void start(final GVRContext context, final String appId) {
        final GVRActivity activity = context.getActivity();
        final long result = create(context.getActivity(), appId);
        if (0 != result) {
            throw new IllegalStateException("Could not initialize the platform sdk; error code: " + result);
        }

        context.registerDrawFrameListener(new GVRDrawFrameListener() {
            @Override
            public void onDrawFrame(float frameTime) {
                final int result = processEntitlementCheckResponse();
                if (0 != result) {
                    context.unregisterDrawFrameListener(this);

                    if (-1 == result) {
                        final Runnable r = new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    activity.getViewManager().getEventManager().sendEvent(
                                            activity,
                                            IPlatformSDKEvents.class,
                                            "onUserIsNotEntitled");
                                } catch (final Exception exc) {
                                    Log.e(TAG, "Exception in user event handler (onUserIsNotEntitled)", exc);
                                }
                            }
                        };
                        activity.runOnUiThread(r);
                    }
                }
            }
        });
    }

    private static native int create(Activity activity, String appId);
    private static native int processEntitlementCheckResponse();

    {
        System.loadLibrary("gvrf-platformsdk");
    }

    private static final String TAG = "OvrEntitlementChecks";
}
