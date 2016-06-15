/* Copyright 2015 Samsung Electronics Co., LTD
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gearvrf;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.gearvrf.scene_objects.GVRViewSceneObject;
import org.gearvrf.scene_objects.view.GVRView;
import org.gearvrf.script.IScriptable;
import org.gearvrf.utility.DockEventReceiver;
import org.gearvrf.utility.GrowBeforeQueueThreadPoolExecutor;
import org.gearvrf.utility.Log;
import org.gearvrf.utility.Threads;
import org.gearvrf.utility.VrAppSettings;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;

/**
 * The typical GVRF application will have a single Android {@link Activity},
 * which <em>must</em> descend from {@link GVRActivity}, not directly from
 * {@code Activity}.
 * 
 * {@code GVRActivity} creates and manages the internal classes which use sensor
 * data to manage a viewpoint, and thus present an appropriate stereoscopic view
 * of your scene graph. {@code GVRActivity} also gives GVRF a full-screen window
 * in landscape orientation with no title bar.
 */
public class GVRActivity extends Activity implements IEventReceiver, IScriptable {

    private static final String TAG = Log.tag(GVRActivity.class);

    // these values are copy of enum KeyEventType in VrAppFramework/Native_Source/Input.h
    public static final int KEY_EVENT_NONE = 0;
    public static final int KEY_EVENT_SHORT_PRESS = 1;
    public static final int KEY_EVENT_DOUBLE_TAP = 2;
    public static final int KEY_EVENT_LONG_PRESS = 3;
    public static final int KEY_EVENT_DOWN = 4;
    public static final int KEY_EVENT_UP = 5;
    public static final int KEY_EVENT_MAX = 6;

    private GVRViewManager mViewManager;
    private GVRScript mGVRScript;
    private GVRMain mGVRMain;
    private VrAppSettings mAppSettings;
    private static View mFullScreenView = null;

    // Group of views that are going to be drawn
    // by some GVRViewSceneObject to the scene.
    private ViewGroup mRenderableViewGroup = null;
    private boolean mPaused = true;

    // Send to listeners and scripts but not this object itself
    private static final int SEND_EVENT_MASK =
            GVREventManager.SEND_MASK_ALL & ~GVREventManager.SEND_MASK_OBJECT;

    private GVREventReceiver mEventReceiver = new GVREventReceiver(this);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        android.util.Log.i(TAG, "onCreate " + Integer.toHexString(hashCode()));
        super.onCreate(savedInstanceState);

        if (null != Threads.getThreadPool()) {
            Threads.getThreadPool().shutdownNow();
        }
        Threads.setThreadPool(new GrowBeforeQueueThreadPoolExecutor("gvrf"));

        /*
         * Removes the title bar and the status bar.
         */
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        mAppSettings = new VrAppSettings(this);

        mRenderableViewGroup = (ViewGroup) findViewById(android.R.id.content).getRootView();
        mActivityHandler = ActivityHandlerFactory.makeActivityHandler(this);
    }

    private void startDockEventReceiver() {
        mDockEventReceiver = GVRConfigurationManager.getInstance().makeDockEventReceiver(this,
                new Runnable() {
                    @Override
                    public void run() {
                        handleOnDock();
                    }
                }, new Runnable() {
                    @Override
                    public void run() {
                        handleOnUndock();
                    }
                });
        if (null != mDockEventReceiver) {
            mDockEventReceiver.start();
        }
    }

    private void onConfigure(){
        onInitAppSettings(mAppSettings);

        GVRConfigurationManager.onInitialize(this);
        GVRConfigurationManager.getInstance().invalidate();

        startDockEventReceiver();
    }

    /**
     * <em>Derived classes must call through to the super class's implementation of this method.</em>
     * </p>
     */
    protected void onInitAppSettings(VrAppSettings appSettings) {
    }

    public VrAppSettings getAppSettings() {
        return mAppSettings;
    }

    @Override
    protected void onPause() {
        android.util.Log.i(TAG, "onPause " + Integer.toHexString(hashCode()));

        mPaused = true;
        if (mViewManager != null) {
            mViewManager.onPause();

            mViewManager.getEventManager().sendEventWithMask(
                    SEND_EVENT_MASK,
                    this,
                    IActivityEvents.class,
                    "onPause");
        }

        mActivityHandler.onPause();
        super.onPause();
    }

    @Override
    protected void onResume() {
        android.util.Log.i(TAG, "onResume " + Integer.toHexString(hashCode()));

        mPaused = false;
        super.onResume();
        if (mViewManager != null) {
            mViewManager.onResume();

            mViewManager.getEventManager().sendEventWithMask(
                    SEND_EVENT_MASK,
                    this,
                    IActivityEvents.class,
                    "onResume");
        }

        mActivityHandler.onResume();
    }

    @Override
    protected void onDestroy() {
        android.util.Log.i(TAG, "onDestroy " + Integer.toHexString(hashCode()));

        if (null != mDockEventReceiver) {
            mDockEventReceiver.stop();
        }
        if (mViewManager != null) {
            mViewManager.onDestroy();

            mViewManager.getEventManager().sendEventWithMask(
                    SEND_EVENT_MASK,
                    this,
                    IActivityEvents.class,
                    "onDestroy");
        }

        mActivityHandler.getNative().onDestroy();
        super.onDestroy();
    }

    /**
     * Links {@linkplain GVRScript a script} to the activity; sets the version;
     * 
     * @param gvrScript
     *            An instance of {@link GVRScript} to handle callbacks on the GL
     *            thread.
     * @param dataFileName
     *            Name of the XML file containing the framebuffer parameters. 
     * 
     *            <p>
     *            The XML filename is relative to the application's
     *            {@code assets} directory, and can specify a file in a
     *            directory under the application's {@code assets} directory.
     * @deprecated
     */
    public void setScript(GVRScript gvrScript, String dataFileName) {
        this.mGVRScript = gvrScript;
        if (getRequestedOrientation() == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
            if (Configuration.ORIENTATION_PORTRAIT == getResources().getConfiguration().orientation) {
                Log.w(TAG, "display in portrait, short-circuiting this activity and waiting for the switch to landscape");
                return;
            }

            GVRXMLParser xmlParser = new GVRXMLParser(getAssets(),
                    dataFileName, mAppSettings);
            onConfigure();
            if (!GVRConfigurationManager.getInstance().isVrSupported()) {//@todo isvrsupported
                // fall back to Monoscopic with flag forcing too
                mAppSettings.getMonoscopicModeParams().setMonoscopicMode(true);
            }
            if (!mActivityHandler.isMonoscopic() && !mAppSettings.getMonoscopicModeParams().isMonoscopicMode()) {
                mViewManager = new GVRViewManager(this, gvrScript, xmlParser);
            } else {
                mViewManager = new GVRMonoscopicViewManager(this, gvrScript, xmlParser);
            }

            if (gvrScript instanceof GVRMain) {
                mViewManager.setUseTheFrameworkThread(true);
            }

            mViewManager.getEventManager().sendEventWithMask(
                    SEND_EVENT_MASK,
                    this,
                    IActivityEvents.class,
                    "onSetScript", gvrScript);

            mViewManager.registerDrawFrameListener(new GVRDrawFrameListener() {
                @Override
                public void onDrawFrame(float frameTime) {
                    if (GVRConfigurationManager.getInstance().isHmtConnected()) {
                        handleOnDock();
                        mViewManager.unregisterDrawFrameListener(this);
                    }
                }
            });
            mActivityHandler.onSetScript(mViewManager);
        } else {
            throw new IllegalArgumentException(
                    "You can not set orientation to portrait for GVRF apps.");
        }
    }

    public View getFullScreenView() {
        if (mFullScreenView != null) {
            return mFullScreenView;
        }

        final VrAppSettings.Size screenDimensions = mAppSettings.getScreenDimensions();
        ViewGroup.LayoutParams layout = new ViewGroup.LayoutParams(screenDimensions.width, screenDimensions.height);
        mFullScreenView = new View(this);
        mFullScreenView.setLayoutParams(layout);
        mRenderableViewGroup.addView(mFullScreenView);
        return mFullScreenView;
    }

    /**
     * Gets the {@linkplain GVRScript a script} linked to the activity.
     * @return the {@link GVRScript}.
     * @deprecated
     */
    public GVRScript getScript() {
        return mGVRScript;
    }

    /**
     * Links {@linkplain GVRMain} to the activity; sets the version;
     * 
     * @param gvrMain
     *            An instance of {@link GVRMain} to handle callbacks on the framework
     *            thread.
     * @param dataFileName
     *            Name of the XML file containing the framebuffer parameters. 
     *            <p>
     *            The XML filename is relative to the application's
     *            {@code assets} directory, and can specify a file in a
     *            directory under the application's {@code assets} directory.
     */
    public void setMain(GVRMain gvrMain, String dataFileName) {
        this.mGVRScript = gvrMain;
        this.mGVRMain = gvrMain;
        setScript(gvrMain, dataFileName);
    }

    /**
     * Gets the {@linkplain GVRMain} linked to the activity.
     * @return the {@link GVRMain}.
     */
    public GVRMain getMain() {
        return mGVRMain;
    }

    /**
     * Sets whether to force rendering to be single-eye, monoscopic view.
     * 
     * @param force
     *            If true, will create a GVRMonoscopicViewManager when
     *            {@linkplain setMain setMain()} is called. If false, will
     *            proceed to auto-detect whether the device supports VR
     *            rendering and choose the appropriate ViewManager. This call
     *            will only have an effect if it is called before
     *            {@linkplain #setMain(GVRMain, String) setMain()}.
     * @deprecated
     */
    @Deprecated
    public void setForceMonoscopic(boolean force) {
        mAppSettings.monoscopicModeParams.setMonoscopicMode(force);
//@todo
    /**
     * Sets whether to force rendering to be single-eye, monoscopic view.
     * 
     * @param force
     *            If true, switch the current ViewManager to monoscopic rendering
     *            mode. If false, will switch back to normal stereoscopic rendering
     *            mode. Used by app to switch between monoscopic and stereoscopic
     *            mode on the fly
     */
        mViewManager.switchMonoscopicMode(force);
    }

    /**
     * Returns whether a monoscopic view was asked to be forced during
     * {@linkplain #setMain(GVRMain, String) setMain()}.
     * 
     * @see setForceMonoscopic
     * @deprecated
     */
    @Deprecated
    public boolean getForceMonoscopic() {
        return mAppSettings.monoscopicModeParams.isMonoscopicMode();
    }

    /**
     * @deprecated
     */
    public long getNative() {
        return mActivityHandler.getNative().getPtr();
    }

    /**
     * @deprecated
     * @param camera
     */
    void setCamera(GVRCamera camera) {
        mActivityHandler.getNative().setCamera(camera);
    }

    void setCameraRig(GVRCameraRig cameraRig) {
        mActivityHandler.getNative().setCameraRig(cameraRig);
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        boolean handled = mViewManager.dispatchKeyEvent(event);
        if (handled == false) {
            handled = super.dispatchKeyEvent(event);
        }
        return handled;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (KeyEvent.KEYCODE_BACK == keyCode) {
            event.startTracking();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (!mPaused && KeyEvent.KEYCODE_BACK == keyCode) {
            if (null != mActivityHandler) {
                return mActivityHandler.onBack();
            }
        }
        return super.onKeyUp(keyCode, event);
    }

    @Override
    public boolean onKeyLongPress(int keyCode, KeyEvent event) {
        if (KeyEvent.KEYCODE_BACK == keyCode) {
            if (null != mActivityHandler) {
                return mActivityHandler.onBackLongPress();
            }
        }
        return super.onKeyLongPress(keyCode, event);
    }

    @Override
    public boolean dispatchGenericMotionEvent(MotionEvent event) {
        boolean handled = mViewManager.dispatchMotionEvent(event);
        if (handled == false) {
            handled = super.dispatchGenericMotionEvent(event);
        }
        return handled;
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        boolean handled = mViewManager.dispatchMotionEvent(event);
        if (handled == false) {
            handled = super.dispatchTouchEvent(event);// VrActivity's
        }

        mViewManager.getEventManager().sendEventWithMask(
                SEND_EVENT_MASK,
                this,
                IActivityEvents.class,
                "dispatchTouchEvent", event);

        return handled;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (Configuration.ORIENTATION_LANDSCAPE == newConfig.orientation) {
            Log.w(TAG, "back to landscape orientation, restarting activity");
            finish();
            startActivity(getIntent());
        }

        if (mViewManager != null) {
            mViewManager.getEventManager().sendEventWithMask(
                    SEND_EVENT_MASK,
                    this,
                    IActivityEvents.class,
                    "onConfigurationChanged", newConfig);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (mViewManager != null) {
            mViewManager.getEventManager().sendEventWithMask(
                    SEND_EVENT_MASK,
                    this,
                    IActivityEvents.class,
                    "onTouchEvent", event);
        }

        return super.onTouchEvent(event);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        if (mViewManager != null) {
            mViewManager.getEventManager().sendEventWithMask(
                    SEND_EVENT_MASK,
                    this,
                    IActivityEvents.class,
                    "onWindowFocusChanged", hasFocus);
        }

        super.onWindowFocusChanged(hasFocus);
    }

    boolean updateSensoredScene() {
        return mViewManager.updateSensoredScene();
    }

    /**
     * It is a convenient function to add a {@link GVRView} to Android hierarchy
     * view. UI thread will call {@link GVRView#draw(android.graphics.Canvas)}
     * to refresh the view when necessary.
     *
     * @param view Is a {@link GVRView} that draw itself into some
     *            {@link GVRViewSceneObject}.
     */
    public void registerView(final View view) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                /* The full screen should be updated
                otherwise just the children's bounds may be refreshed. */
                mRenderableViewGroup.setClipChildren(false);

                mRenderableViewGroup.addView(view);
            }
        });
    }

    /**
     * Remove a child view of Android hierarchy view .
     * 
     * @param view View to be removed.
     */
    public void unregisterView(final View view) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mRenderableViewGroup.removeView(view);
            }
        });
    }

    public GVRContext getGVRContext() {
        return mViewManager;
    }

    @Override
    public GVREventReceiver getEventReceiver() {
        return mEventReceiver;
    }

    private boolean mIsDocked = false;

    void handleOnDock() {
        Log.i(TAG, "handleOnDock");
        final Runnable r = new Runnable() {
            @Override
            public void run() {
                if (!mIsDocked) {
                    mIsDocked = true;

                    mActivityHandler.getNative().onDock();
                    if (null != mViewManager) {
                        mViewManager.onDock();
                    }
                    for (final DockListener dl : mDockListeners) {
                        dl.onDock();
                    }
                }
            }
        };
        runOnUiThread(r);
    }

    void handleOnUndock() {
        Log.i(TAG, "handleOnUndock");
        final Runnable r = new Runnable() {
            @Override
            public void run() {
                if (mIsDocked) {
                    mIsDocked = false;

                    mActivityHandler.getNative().onUndock();
                    if (null != mViewManager) {
                        mViewManager.onUndock();
                    }
                    for (final DockListener dl : mDockListeners) {
                        dl.onUndock();
                    }
                }
            }
        };
        runOnUiThread(r);
    }

    interface DockListener {
        void onDock();
        void onUndock();
    }

    private final List<DockListener> mDockListeners = new CopyOnWriteArrayList<DockListener>();

    void addDockListener(final DockListener dl) {
        mDockListeners.add(dl);
    }

    private DockEventReceiver mDockEventReceiver;

    private ActivityHandler mActivityHandler;

}
