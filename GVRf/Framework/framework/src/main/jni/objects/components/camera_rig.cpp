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
 * Holds left, right cameras and reacts to the rotation sensor.
 ***************************************************************************/

#include "camera_rig.h"

#include "glm/gtc/quaternion.hpp"

#include "objects/scene_object.h"
#include "objects/components/camera.h"
#include "objects/components/perspective_camera.h"
#include "util/gvr_time.h"

namespace gvr {

CameraRig::CameraRig() :
        CameraRigBase(CameraRig::getComponentType()) {
}

CameraRig::~CameraRig() {
}

void CameraRig::predict(float time) {
    return predict(time, rotation_sensor_data_);
}

void CameraRig::predict(float time, const RotationSensorData& rotationSensorData) {
    long long clock_time = getCurrentTime();
    float time_diff = (clock_time - rotationSensorData.time_stamp())
            / 1000000000.0f;

    glm::vec3 axis = rotationSensorData.gyro();
    //the magnitude of the gyro vector should be the angular velocity, rad/sec
    float angle = glm::length(axis);

    //normalize the axis
    if (angle != 0.0f) {
        axis /= angle;
    }

    setRotation(complementary_rotation_*rotationSensorData.quaternion());
}


void CameraRig::setPosition(const glm::vec3& transform_position) {
	Transform* transform = getHeadTransform();
	transform->set_position(transform_position);
}

Transform* CameraRig::getHeadTransform() const {
	return owner_object()->getChildByIndex(0)->transform();
}

}
