/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: 2026 The halogenOS Project
 */

#include "PopupCamera.h"

#include <android-base/file.h>
#include <android-base/logging.h>
#include <android-base/strings.h>

using ::android::base::ReadFileToString;
using ::android::base::Trim;
using ::android::base::WriteStringToFile;

namespace aidl::vendor::oplus::hardware::popupcamera {

PopupCamera::PopupCamera() {
    std::string calibration;
    if (!ReadFileToString(kPersistCalibration, &calibration) || Trim(calibration).empty()) {
        calibration = kCalibrationDefault;
    }
    if (!WriteStringToFile(Trim(calibration), kMotorCalibration)) {
        LOG(ERROR) << "Failed to write motor calibration";
    }
    LOG(INFO) << "Popup camera HAL initialized";
}

ndk::ScopedAStatus PopupCamera::raise() {
    if (!WriteStringToFile("1", kMotorDirection)) {
        LOG(ERROR) << "Failed to set motor direction up";
        return ndk::ScopedAStatus::fromExceptionCode(EX_SERVICE_SPECIFIC);
    }
    if (!WriteStringToFile("1", kMotorEnable)) {
        LOG(ERROR) << "Failed to enable motor";
        return ndk::ScopedAStatus::fromExceptionCode(EX_SERVICE_SPECIFIC);
    }
    return ndk::ScopedAStatus::ok();
}

ndk::ScopedAStatus PopupCamera::lower() {
    if (!WriteStringToFile("0", kMotorDirection)) {
        LOG(ERROR) << "Failed to set motor direction down";
        return ndk::ScopedAStatus::fromExceptionCode(EX_SERVICE_SPECIFIC);
    }
    if (!WriteStringToFile("1", kMotorEnable)) {
        LOG(ERROR) << "Failed to enable motor";
        return ndk::ScopedAStatus::fromExceptionCode(EX_SERVICE_SPECIFIC);
    }
    return ndk::ScopedAStatus::ok();
}

ndk::ScopedAStatus PopupCamera::getPosition(MotorPosition* _aidl_return) {
    std::string value;
    if (!ReadFileToString(kMotorPosition, &value)) {
        LOG(ERROR) << "Failed to read motor position";
        return ndk::ScopedAStatus::fromExceptionCode(EX_SERVICE_SPECIFIC);
    }
    *_aidl_return = Trim(value) == "0" ? MotorPosition::UP : MotorPosition::DOWN;
    return ndk::ScopedAStatus::ok();
}

}  // namespace aidl::vendor::oplus::hardware::popupcamera
