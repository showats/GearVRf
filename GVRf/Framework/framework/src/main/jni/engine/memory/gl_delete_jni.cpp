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

#include "util/gvr_jni.h"
#include "run_on_gl_thread.h"

namespace gvr {

extern "C" {

JNIEXPORT void JNICALL
Java_org_gearvrf_NativeGLDelete_processQueues(JNIEnv * env, jobject obj, jlong deleterPtr) {
    RunOnGlThread* deleter = reinterpret_cast<RunOnGlThread*>(deleterPtr);
    deleter->processQueues();
}

JNIEXPORT jlong JNICALL
Java_org_gearvrf_NativeGLDelete_ctor(JNIEnv * env, jobject obj) {
    return reinterpret_cast<long>(new RunOnGlThread());
}

JNIEXPORT void JNICALL
Java_org_gearvrf_NativeGLDelete_dtor(JNIEnv * env, jobject obj, jlong deleterPtr) {
    RunOnGlThread* deleter = reinterpret_cast<RunOnGlThread*>(deleterPtr);
    delete deleter;
}

JNIEXPORT void JNICALL
Java_org_gearvrf_NativeGLDelete_createTlsKey(JNIEnv * env, jobject obj) {
    RunOnGlThread::createTlsKey();
}

}   //extern "C"

}   //namespace gvr
