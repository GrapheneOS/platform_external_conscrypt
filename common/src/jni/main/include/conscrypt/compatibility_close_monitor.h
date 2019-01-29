/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#ifndef CONSCRYPT_COMPATIBILITY_CLOSE_MONITOR_H_
#define CONSCRYPT_COMPATIBILITY_CLOSE_MONITOR_H_

#include <conscrypt/macros.h>

namespace conscrypt {

/*
 * This is a big hack; don't learn from this. Basically what happened is we do
 * not have an API way to insert ourselves into the AsynchronousCloseMonitor
 * that's compiled into the native libraries for libcore when we're unbundled.
 * So we try to look up the symbol from the main library to find it.
 *
 * On non-Android platforms, this class becomes a no-op as the function pointers
 * to create and destroy AsynchronousCloseMonitor instances will be null.
 */
class CompatibilityCloseMonitor {
 public:
    explicit CompatibilityCloseMonitor(int fd) {
        if (asyncCloseMonitorConstructor != nullptr) {
            asyncCloseMonitorConstructor(objBuffer, fd);
        }
    }

    ~CompatibilityCloseMonitor() {
        if (asyncCloseMonitorDestructor != nullptr) {
            asyncCloseMonitorDestructor(objBuffer);
        }
    }

    static void init();

 private:
    typedef void (*acm_ctor_func)(void*, int);
    typedef void (*acm_dtor_func)(void*);

    // Pointer to constructor for AsynchronousCloseMonitor. This will be null on
    // platforms other than Android.
    static acm_ctor_func asyncCloseMonitorConstructor;

    // Pointer to destructor for AsynchronousCloseMonitor. This will be null on
    // platforms other than Android.
    static acm_dtor_func asyncCloseMonitorDestructor;

    // TODO: remove this when moving to use C API function in libnativehelper.
    char objBuffer[256];
#if 0
    static_assert(sizeof(objBuffer) > 2*sizeof(AsynchronousCloseMonitor),
                  "CompatibilityCloseMonitor must be larger than the actual object");
#endif
};

}  // namespace conscrypt

#endif  // CONSCRYPT_COMPATIBILITY_CLOSE_MONITOR_H_
