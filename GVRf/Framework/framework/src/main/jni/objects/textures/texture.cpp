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

/***************************************************************************
 * Textures.
 ***************************************************************************/

#include "texture.h"

namespace gvr {

Texture::~Texture() {
    //RunOnGlThread* deleter = getInstance();
//        deleter->cancelRunnable(this);
    delete gl_texture_;
}

// Should be called in GL context.
GLuint Texture::getId() {
    if (gl_texture_ == 0) {
        // must be recycled already. The caller will handle error.
        return 0;
    }

    // Before returning the ID makes sure nothing is pending
    runOnGlThread();

    return gl_texture_->id();
}

void Texture::updateTextureParameters(int* texture_parameters) {
    // Sets the new MIN FILTER
    GLenum min_filter_type_ = texture_parameters[0];

    // Sets the MAG FILTER
    GLenum mag_filter_type_ = texture_parameters[1];

    // Sets the wrap parameter for texture coordinate S
    GLenum wrap_s_type_ = texture_parameters[3];

    // Sets the wrap parameter for texture coordinate S
    GLenum wrap_t_type_ = texture_parameters[4];

    glBindTexture(target, getId());

    // Sets the anisotropic filtering if the value provided is greater than 1 because 1 is the default value
    if (texture_parameters[2] > 1.0f) {
        glTexParameterf(target, GL_TEXTURE_MAX_ANISOTROPY_EXT, texture_parameters[2]);
    }

    glTexParameteri(target, GL_TEXTURE_WRAP_S, wrap_s_type_);
    glTexParameteri(target, GL_TEXTURE_WRAP_T, wrap_t_type_);
    glTexParameteri(target, GL_TEXTURE_MIN_FILTER, min_filter_type_);
    glTexParameteri(target, GL_TEXTURE_MAG_FILTER, mag_filter_type_);
    glBindTexture(target, 0);
}

void Texture::runOnGlThread() {
    if (gl_texture_) {
        gl_texture_->runOnGlThread();
    }
}

bool Texture::isReady() {
    return ready;
}

void Texture::setReady(bool ready) {
    this->ready = ready;
}

Texture::Texture(GLTexture* gl_texture) : HybridObject() {
    gl_texture_ = gl_texture;
}

}

