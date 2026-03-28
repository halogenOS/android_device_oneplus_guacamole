/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: 2026 The halogenOS Project
 */

#pragma once

#include <aidl/vendor/oplus/hardware/popupcamera/BnPopupCamera.h>

namespace aidl::vendor::oplus::hardware::popupcamera {

class PopupCamera : public BnPopupCamera {
  public:
    PopupCamera();

    ndk::ScopedAStatus raise() override;
    ndk::ScopedAStatus lower() override;
    ndk::ScopedAStatus getPosition(MotorPosition* _aidl_return) override;

  private:
    static constexpr const char* kMotorEnable = "/sys/class/motor/enable";
    static constexpr const char* kMotorDirection = "/sys/class/motor/direction";
    static constexpr const char* kMotorPosition = "/sys/class/motor/position";
    static constexpr const char* kMotorCalibration = "/sys/class/motor/hall_calibration";
    static constexpr const char* kPersistCalibration =
        "/mnt/vendor/persist/engineermode/hall_calibration";
    static constexpr const char* kCalibrationDefault =
        "170,170,480,0,0,480,500,0,0,500,1500";
};

}  // namespace aidl::vendor::oplus::hardware::popupcamera
