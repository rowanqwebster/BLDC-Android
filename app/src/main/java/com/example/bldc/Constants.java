/*
 * Copyright (C) 2014 The Android Open Source Project
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
 *
 * FILE MODIFIED TO SUIT THE NEEDS OF THIS PROJECT
 * Added new constants for motor statistics
 *
 */

package com.example.bldc;

public interface Constants {

    int MESSAGE_STATE_CHANGE = 1;
    int MESSAGE_READ = 2;
    int MESSAGE_WRITE = 3;
    int MESSAGE_DEVICE_NAME = 4;
    int MESSAGE_TOAST = 5;

    String DEVICE_NAME = "device_name";
    String TOAST = "toast";

    String SPEED = "sp";
    String POWER = "pw";
    String CURRENT = "cr";
    String CONTROL_TEMP = "ct";
    String BATTERY_VOLT = "bv";
    String BATTERY_REM = "br";
    String PWM_FREQ = "pf";
    String MAX_SPEED = "ms";
    String MAX_POWER_DRAW = "mp";
    String MAX_CURRENT_DRAW = "mc";
    String BATTERY_CHEMISTRY = "bc";
    String DRIVING_MODE = "dm";
    String BATTERY_CELLS = "be";
    String BATTERY_FLAG = "bf";

}
