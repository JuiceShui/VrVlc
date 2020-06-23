package com.demo.vlcvr;

import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Message;
import android.util.Log;
import android.view.Surface;

public class SensorEventHandler implements SensorEventListener {
    public static String TAG = "SensorEventHandler";

    private float[] rotationMatrix = new float[16];
    private SensorHandlerCallback sensorHandlerCallback;
    private Context context;
    private boolean sensorRegistered;
    private SensorManager sensorManager;
    private int mDeviceRotation;
    private float[] degree = new float[]{0.0f, 0.0f, 0.0f};
    private float[] changed = new float[]{0.0f, 0.0f, 0.0f};


    private float[] orientationVals = new float[3];
    private float[] mRotationMatrix = new float[16];

    public SensorEventHandler(Context context) {
        this.context = context;
        mDeviceRotation = ((Activity) context).getWindowManager().getDefaultDisplay().getRotation();
        if (mDeviceRotation == Surface.ROTATION_0) {
            degree = new float[]{0.0f, 0.0f, 0.0f};
        } else if (mDeviceRotation == Surface.ROTATION_90) {
            degree = new float[]{0.0f, 0.0f, -90.0f};
        }
    }

    public void init() {
        sensorRegistered = false;
        sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        Sensor sensorRot = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        if (sensorRot == null) return;
        sensorManager.registerListener(this, sensorRot, SensorManager.SENSOR_DELAY_GAME);
        sensorRegistered = true;
    }

    public void releaseResources() {
        if (!sensorRegistered || sensorManager == null) return;
        sensorManager.unregisterListener(this);
        sensorRegistered = false;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.accuracy != 0) {
            int type = event.sensor.getType();
            switch (type) {
                case Sensor.TYPE_ROTATION_VECTOR:
                    //FIXME
                    /*mDeviceRotation = ((Activity) context).getWindowManager().getDefaultDisplay().getRotation();
                    SensorUtils.sensorRotationVectorToMatrix(event, mDeviceRotation, rotationMatrix);
                    sensorHandlerCallback.updateSensorMatrix(rotationMatrix);
                    determineOrientation(rotationMatrix);*/
                    handleMotionValues(event.values);
                    break;
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    public void setSensorHandlerCallback(SensorHandlerCallback sensorHandlerCallback) {
        this.sensorHandlerCallback = sensorHandlerCallback;
    }

    public interface SensorHandlerCallback {
        void onDegreeChange(float[] degree);
    }

    public void removeCallback() {
        if (sensorHandlerCallback != null) {
            sensorHandlerCallback = null;
        }
    }

    private void handleMotionValues(float[] values) {
        SensorManager.getRotationMatrixFromVector(mRotationMatrix, values);
        SensorManager.remapCoordinateSystem(mRotationMatrix, SensorManager.AXIS_X, SensorManager.AXIS_Z, mRotationMatrix);
        SensorManager.getOrientation(mRotationMatrix, orientationVals);
        orientationVals[0] = (float) Math.toDegrees(orientationVals[0]);
        orientationVals[1] = (float) Math.toDegrees(orientationVals[1]);
        orientationVals[2] = (float) Math.toDegrees(orientationVals[2]);

        changed[0] = (float) (orientationVals[0] - degree[0]);
        changed[1] = (float) (orientationVals[1] - degree[1]);
        changed[2] = (float) (orientationVals[2] - degree[2]);
        degree[0] = (float) orientationVals[0];
        degree[1] = (float) orientationVals[1];
        degree[2] = (float) orientationVals[2];
        Message message = new Message();
        message.what = MainActivity.CHANGE_POINTER;
        Bundle bundle = new Bundle();
        bundle.putFloat("yaw", changed[0]);
        bundle.putFloat("pitch", changed[1]);
        bundle.putFloat("roll", changed[2]);
        //changed[2] = 0;
        message.setData(bundle);
        ((PlayerActivity) context).getHandler().sendMessage(message);
        if (sensorHandlerCallback != null) {
            sensorHandlerCallback.onDegreeChange(changed);
        }
    }
}
