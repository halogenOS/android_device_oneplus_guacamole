/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: 2026 The halogenOS Project
 */

#include "PopupCamera.h"

#include <android-base/logging.h>
#include <android/binder_manager.h>
#include <android/binder_process.h>

using aidl::vendor::oplus::hardware::popupcamera::PopupCamera;

int main() {
    ABinderProcess_setThreadPoolMaxThreadCount(0);

    auto svc = ndk::SharedRefBase::make<PopupCamera>();
    const std::string instance = std::string() + PopupCamera::descriptor + "/default";

    auto status = AServiceManager_addService(svc->asBinder().get(), instance.c_str());
    CHECK_EQ(status, STATUS_OK) << "Failed to register " << instance;

    ABinderProcess_joinThreadPool();
    return EXIT_FAILURE;
}
