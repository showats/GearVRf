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

import org.gearvrf.utility.VrAppSettings;

/** A container for various services and pieces of data required for rendering. */
class GVRRenderBundle {
    private final GVRContext mGVRContext;
    private final GVRMaterialShaderManager mMaterialShaderManager;
    private final GVRPostEffectShaderManager mPostEffectShaderManager;
    private GVRRenderTexture mPostEffectRenderTextureA = null;
    private GVRRenderTexture mPostEffectRenderTextureB = null;

    GVRRenderBundle(GVRContext gvrContext) {
        mGVRContext = gvrContext;
        mMaterialShaderManager = new GVRMaterialShaderManager(gvrContext);
        mPostEffectShaderManager = new GVRPostEffectShaderManager(gvrContext);

        update();
    }

    GVRMaterialShaderManager getMaterialShaderManager() {
        return mMaterialShaderManager;
    }

    GVRPostEffectShaderManager getPostEffectShaderManager() {
        return mPostEffectShaderManager;
    }

    GVRRenderTexture getPostEffectRenderTextureA() {
        return mPostEffectRenderTextureA;
    }

    GVRRenderTexture getPostEffectRenderTextureB() {
        return mPostEffectRenderTextureB;
    }

    private void update() {

        final VrAppSettings appSettings = mGVRContext.getActivity().getAppSettings();
        int sampleCount = appSettings.getEyeBufferParams().getMultiSamples() < 0 ? 0
                : appSettings.getEyeBufferParams().getMultiSamples();
        if (sampleCount > 1) {
            int maxSampleCount = GVRMSAA.getMaxSampleCount();
            if (sampleCount > maxSampleCount) {
                sampleCount = maxSampleCount;
            }
        }

        final int width = appSettings.getEyeBufferParams().getResolutionWidth();
        final int height = appSettings.getEyeBufferParams().getResolutionHeight();
        if (sampleCount <= 1) {
            mPostEffectRenderTextureA = new GVRRenderTexture(mGVRContext, width, height);
            mPostEffectRenderTextureB = new GVRRenderTexture(mGVRContext, width, height);
        } else {
            mPostEffectRenderTextureA = new GVRRenderTexture(mGVRContext, width, height, sampleCount);
            mPostEffectRenderTextureB = new GVRRenderTexture(mGVRContext, width, height, sampleCount);
        }
    }
}
