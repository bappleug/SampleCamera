/*
 * Copyright (C) 2016 The Android Open Source Project
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

package me.ray.samplecamera.camera;

import android.content.Context;
import android.util.SparseIntArray;
import android.view.Display;
import android.view.OrientationEventListener;
import android.view.Surface;


/**
 * Monitors the value returned from {@link Display#getRotation()}.
 */
abstract class OrientationDetector {

    private final OrientationEventListener mOrientationEventListener;

    /**
     * Mapping from Surface.Rotation_n to degrees.
     */
    static final SparseIntArray DISPLAY_ORIENTATIONS = new SparseIntArray();

    static {
        DISPLAY_ORIENTATIONS.put(Surface.ROTATION_0, 0);
        DISPLAY_ORIENTATIONS.put(Surface.ROTATION_90, 90);
        DISPLAY_ORIENTATIONS.put(Surface.ROTATION_180, 180);
        DISPLAY_ORIENTATIONS.put(Surface.ROTATION_270, 270);
    }

    Display mDisplay;

    private int mLastKnownDisplayOrientation = 0;

    public OrientationDetector(Context context) {
        mOrientationEventListener = new OrientationEventListener(context) {

            private int mOldDisplayRotation = -1;
            private int mOldSensorOrientation = -1;

            @Override
            public void onOrientationChanged(int orientation) {
                if (orientation == OrientationEventListener.ORIENTATION_UNKNOWN ||
                        mDisplay == null) {
                    return;
                }
                orientation = (orientation + 45) / 90 * 90;
                final int rotation = mDisplay.getRotation();
                if (mOldDisplayRotation != rotation || mOldSensorOrientation != orientation) {
                    mOldDisplayRotation = rotation;
                    mOldSensorOrientation = orientation;
                    dispatchOnOrientationChanged(DISPLAY_ORIENTATIONS.get(rotation), orientation);
                }
            }
        };
    }

    public void enable(Display display) {
        mDisplay = display;
        mOrientationEventListener.enable();
        // Immediately dispatch the first callback
        int displayOrientation = DISPLAY_ORIENTATIONS.get(display.getRotation());
        dispatchOnOrientationChanged(displayOrientation, displayOrientation);
    }

    public void disable() {
        mOrientationEventListener.disable();
        mDisplay = null;
    }

    public int getLastKnownDisplayOrientation() {
        return mLastKnownDisplayOrientation;
    }

    void dispatchOnOrientationChanged(int displayOrientation, int sensorOrientation) {
        mLastKnownDisplayOrientation = displayOrientation;
        onOrientationChanged(displayOrientation, sensorOrientation);
    }

    /**
     * Called when display orientation is changed.
     *
     * @param displayOrientation One of 0, 90, 180, and 270.
     */
    public abstract void onOrientationChanged(int displayOrientation, int sensorOrientation);

}
