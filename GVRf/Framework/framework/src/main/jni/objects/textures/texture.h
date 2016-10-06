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

#ifndef TEXTURE_H_
#define TEXTURE_H_

#include "gl/gl_texture.h"
#include "objects/hybrid_object.h"

namespace gvr {

class Texture: public HybridObject, RunnableOnGlThread {
public:
    virtual ~Texture();

    // Should be called in GL context.
    virtual GLuint getId();
    virtual void updateTextureParameters(int* texture_parameters);
    virtual GLenum getTarget() const = 0;
    virtual void runOnGlThread();

    bool isReady();
    void setReady(bool ready);

protected:
    Texture(GLTexture* gl_texture);

    GLTexture* gl_texture_;

private:
    Texture(const Texture& texture) = delete;
    Texture(Texture&& texture) = delete;
    Texture& operator=(const Texture& texture) = delete;
    Texture& operator=(Texture&& texture) = delete;

private:
    static const GLenum target = GL_TEXTURE_2D;
    bool ready = false;
};

}

#endif
