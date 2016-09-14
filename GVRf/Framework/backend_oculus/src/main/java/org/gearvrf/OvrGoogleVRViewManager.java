
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

import android.content.Context;

import com.google.vr.sdk.base.Eye;
import com.google.vr.sdk.base.GvrView;
import com.google.vr.sdk.base.HeadTransform;
import com.google.vr.sdk.base.Viewport;

import javax.microedition.khronos.egl.EGLConfig;

class OvrGoogleVRViewManager extends GVRViewManager {
    private static final String TAG = OvrGoogleVRViewManager.class.getSimpleName();
    private final float[] headTransform;

    OvrGoogleVRViewManager(final GVRActivity gvrActivity, GVRScript gvrScript) {
        super(gvrActivity, gvrScript);
        GvrView gvrView = new GoogleVRView(gvrActivity, this, null);
        gvrActivity.setContentView(gvrView);
        headTransform = new float[4];
    }

    private void onNewFrame(HeadTransform headTransform) {
        headTransform.getQuaternion(this.headTransform, 0);
        mMainScene.getMainCameraRig().getHeadTransform().setRotation(this.headTransform[3],
                this.headTransform[0], this.headTransform[1], this.headTransform[2]);
    }

    private void onDrawEye(Eye eye) {
        if (eye.getType() == Eye.Type.LEFT) {
            OvrMonoscopicRenderer.cull(mMainScene, mMainScene.getMainCameraRig().getCenterCamera
                    (), mRenderBundle);
            renderCamera(mMainScene, mMainScene
                    .getMainCameraRig().getLeftCamera(), mRenderBundle);
        } else if (eye.getType() == Eye.Type.RIGHT) {
            renderCamera(mMainScene, mMainScene
                    .getMainCameraRig().getRightCamera(), mRenderBundle);
        }
    }

    private static class GoogleVRViewRenderer implements GvrView.StereoRenderer {
        private OvrGoogleVRViewManager mViewManager = null;

        public GoogleVRViewRenderer(OvrGoogleVRViewManager viewManager) {
            mViewManager = viewManager;
        }

        public void setViewManager(OvrGoogleVRViewManager viewManager) {
            mViewManager = viewManager;
        }

        @Override
        public void onSurfaceChanged(int width, int height) {
        }

        @Override
        public void onSurfaceCreated(EGLConfig config) {
            mViewManager.onSurfaceCreated();
        }

        @Override
        public void onDrawEye(Eye eye) {
            mViewManager.onDrawEye(eye);
        }

        @Override
        public void onNewFrame(HeadTransform headTransform) {
            mViewManager.beforeDrawEyes();
            mViewManager.onNewFrame(headTransform);
            mViewManager.onDrawFrame();
        }

        @Override
        public void onFinishFrame(Viewport viewport) {
            mViewManager.afterDrawEyes();
        }

        @Override
        public void onRendererShutdown() {
        }
    }

    private static class GoogleVRView extends GvrView {
        private OvrGoogleVRViewManager mViewManager = null;

        public GoogleVRView(Context context) {
            super(context);
        }

        public GoogleVRView(Context context, final OvrGoogleVRViewManager viewManager,
                            GoogleVRViewRenderer renderer) {
            super(context);
            mViewManager = viewManager;
            setEGLConfigChooser(8, 8, 8, 8, 16, 8);

            if (renderer != null) {
                renderer.setViewManager(viewManager);
                setRenderer(renderer);
            } else {
                setRenderer(new GoogleVRViewRenderer(viewManager));
            }
            setTransitionViewEnabled(true);
            setOnCardboardBackButtonListener(
                    new Runnable() {
                        @Override
                        public void run() {
                            viewManager.getActivity().onBackPressed();
                        }
                    });
        }
    }
}
