/*
 * Copyright (c) 2016 The CyanogenMod Project
 * Copyright (c) 2018 The LineageOS Project
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

package org.lineageos.pocketmode;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;

public class ProximitySensor implements SensorEventListener {

    private static final boolean DEBUG = false;
    private static final String TAG = "PocketModeProximity";

    private static final String CHEESEBURGER_FILE =
            "/sys/devices/soc/soc:fpc_fpc1020/proximity_state";
    private static final String DUMPLING_FILE =
            "/sys/devices/soc/soc:goodix_fp/proximity_state";
    private final String FPC_FILE;

    private SensorManager mSensorManager;
    private Sensor mSensor;
    private Context mContext;

    public ProximitySensor(Context context) {
        boolean found = false;
        mContext = context;
        mSensorManager = mContext.getSystemService(SensorManager.class);
        mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);

        if (fileExists(CHEESEBURGER_FILE)) {
            FPC_FILE = CHEESEBURGER_FILE;
            found = true;
        } else if (fileExists(DUMPLING_FILE)) {
            FPC_FILE = DUMPLING_FILE;
            found = true;
        } else {
            Log.e(TAG, "No proximity state file found!");
            FPC_FILE = CHEESEBURGER_FILE;
        }

        if (found) {
            if (DEBUG) Log.d(TAG, "Using proximity state from " + FPC_FILE);
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        setFPProximityState(event.values[0] < mSensor.getMaximumRange());
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        /* Empty */
    }

    private void setFPProximityState(boolean isNear) {
        if (isFileWritable(FPC_FILE)) {
            writeLine(FPC_FILE, isNear ? "1" : "0");
        } else {
            Log.e(TAG, "Proximity state file " + FPC_FILE + " is not writable!");
        }
    }

    protected void enable() {
        if (DEBUG) Log.d(TAG, "Enabling");
        mSensorManager.registerListener(this, mSensor,
                SensorManager.SENSOR_DELAY_NORMAL);
    }

    protected void disable() {
        if (DEBUG) Log.d(TAG, "Disabling");
        mSensorManager.unregisterListener(this, mSensor);
        // Ensure FP is left enabled
        setFPProximityState(/* isNear */ false);
    }

    /**
     * Checks whether the given file exists
     *
     * @return true if exists, false if not
     */
    public static boolean fileExists(String fileName) {
        final File file = new File(fileName);
        return file.exists();
    }
     /**
     * Checks whether the given file is writable
     *
      * @return true if writable, false if not
      */
     public static boolean isFileWritable(String fileName) {
         final File file = new File(fileName);
         return file.exists() && file.canWrite();
     }

     /**
      * Writes the given value into the given file
      *
      * @return true on success, false on failure
      */
     public static boolean writeLine(String fileName, String value) {
         BufferedWriter writer = null;

         try {
             writer = new BufferedWriter(new FileWriter(fileName));
             writer.write(value);
         } catch (FileNotFoundException e) {
             Log.w(TAG, "No such file " + fileName + " for writing", e);
             return false;
         } catch (IOException e) {
             Log.e(TAG, "Could not write to file " + fileName, e);
             return false;
         } finally {
             try {
                 if (writer != null) {
                     writer.close();
                 }
             } catch (IOException e) {
                 // Ignored, not much we can do anyway
             }
         }

         return true;
     }
}
