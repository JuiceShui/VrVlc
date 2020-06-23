package com.demo.vlcvr;

import android.hardware.SensorEvent;
import android.hardware.SensorManager;
import android.opengl.Matrix;
import android.util.Log;
import android.view.Surface;

/**
 * Project: Pano360
 * Package: com.martin.ads.pano360.utils
 * Created by Ads on 2016/5/2.
 */
public class SensorUtils {
    private static float[] mTmp = new float[16];
    private static float[] oTmp = new float[16];

    //by default, the coordinate is remapped to landscape
    public static void sensorRotationVectorToMatrix(SensorEvent event, int deviceRotation, float[] output) {
        float[] values = event.values;
        switch (deviceRotation) {
            case Surface.ROTATION_0:
                SensorManager.getRotationMatrixFromVector(output, values);
                //determineOrientation(output);
                break;
            default:
                //determineOrientation(output);
                SensorManager.getRotationMatrixFromVector(mTmp, values);
                SensorManager.remapCoordinateSystem(mTmp, SensorManager.AXIS_Y, SensorManager.AXIS_MINUS_X, output);
        }
        Matrix.rotateM(output, 0, 90.0F, 1.0F, 0.0F, 0.0F);
    }

    public static void getOrientation(SensorEvent event, float[] output) {
        //sensorRotationVectorToMatrix(event,oTmp);
        SensorManager.getRotationMatrixFromVector(oTmp, event.values);
        SensorManager.getOrientation(oTmp, output);
    }

    public static void getOrientationFromRotationMatrix(float[] rotationMatrix, float[] output) {
        SensorManager.getOrientation(rotationMatrix, output);
    }

    private static void determineOrientation(float[] rotationMatrix) {
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < rotationMatrix.length; i++) {
            if (i == 0) {
                stringBuilder.append("\n\n");
            }
            stringBuilder.append(rotationMatrix[i]).append("  ");
            if (i == 3 || i == 7 || i == 11) {
                stringBuilder.append("\n");
            }
        }
        Log.e("MATRI", stringBuilder.toString());
        float[] orientationValues = new float[3];
        SensorManager.getOrientation(rotationMatrix, orientationValues);
        double azimuth = Math.toDegrees(orientationValues[0]);
        double pitch = Math.toDegrees(orientationValues[1]);
        double roll = Math.toDegrees(orientationValues[2]);
        Log.e("RAW PITH ROLL", String.valueOf(azimuth) + "  " + String.valueOf(pitch) + "  " + String.valueOf(roll));
    }
}