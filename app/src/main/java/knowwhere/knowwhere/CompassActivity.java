package knowwhere.knowwhere;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.RelativeLayout;

public class CompassActivity extends AppCompatActivity implements SensorEventListener {

    private RelativeLayout mFrame;
    private CompassArrowView mCompassArrow;
    private SensorManager mSensorManager;
    private Sensor accelerometer;
    private Sensor magnetometer;
    private float[] mGravity = null;
    private float[] mGeomagnetic = null;
    private double mRotationInDegrees;

    private Location target, current;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_compass);
        mFrame = findViewById(R.id.frame);
        mCompassArrow = new CompassArrowView(getApplicationContext());
        mFrame.addView(mCompassArrow);
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        Log.i("compass", "null");
        if (null != mSensorManager) {
            accelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            magnetometer = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        }
        if (null == accelerometer || null == magnetometer)
            finish();

        //mGravity = new float[3];
        //mGeomagnetic = new float[3];

        Log.i("compass", "onCreate");
        target = getIntent().getParcelableExtra("target");
        current = getIntent().getParcelableExtra("current");
    }

    @Override
    protected void onResume() {
        super.onResume();
        mSensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        mSensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        final float alpha = 0.97f;

        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            //mGravity[0] = alpha * mGravity[0] + (1 - alpha) * event.values[0];
            //mGravity[1] = alpha * mGravity[1] + (1 - alpha) * event.values[1];
            //mGravity[2] = alpha * mGravity[2] + (1 - alpha) * event.values[2];
            mGravity = new float[3];
            System.arraycopy(event.values, 0, mGravity, 0, 3);
        } else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            //mGeomagnetic[0] = alpha * mGeomagnetic[0] + (1 - alpha) * event.values[0];
            //mGeomagnetic[1] = alpha * mGeomagnetic[1] + (1 - alpha) * event.values[1];
            //mGeomagnetic[2] = alpha * mGeomagnetic[2] + (1 - alpha) * event.values[2];
            mGeomagnetic = new float[3];
            System.arraycopy(event.values, 0, mGeomagnetic, 0, 3);
        }
        if (mGravity != null && mGeomagnetic != null) {
            float rotationMatrix[] = new float[9];
            if (SensorManager.getRotationMatrix(rotationMatrix, null, mGravity, mGeomagnetic)) {
                float orientationMatrix[] = new float[3];
                SensorManager.getOrientation(rotationMatrix, orientationMatrix);
                mRotationInDegrees = Math.toDegrees(orientationMatrix[0]);

                mRotationInDegrees = (mRotationInDegrees + 360) % 360;
                mRotationInDegrees -= bearing(current, target);

                mCompassArrow.invalidate();
                mGravity = mGeomagnetic = null;
            }
        }
    }

    protected double bearing(Location l1, Location l2) {
        double long1 = l1.getLongitude();
        double long2 = l2.getLongitude();
        double lat1 = Math.toRadians(l1.getLatitude());
        double lat2 = Math.toRadians(l2.getLatitude());
        double longDif = Math.toRadians(long2 - long1);
        double y = Math.sin(longDif) * Math.cos(lat2);
        double x = Math.cos(lat1) * Math.sin(lat2) - Math.sin(lat1) * Math.cos(lat2) * Math.cos(longDif);
        return (Math.toDegrees(Math.atan2(y, x)) + 360) % 360;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    public class CompassArrowView extends View {

        int mParentWidth;
        int mParentHeight;
        int mBitmapWidth;
        int mParentCenterX;
        int mParentCenterY;
        int mViewTopX;
        int mViewLeftY;
        Bitmap mBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.arrow);

        public CompassArrowView(Context context) {
            super(context);
        }

        @Override
        protected void onSizeChanged(int w, int h, int oldw, int oldh) {
            mParentWidth = mFrame.getWidth();
            mParentHeight = mFrame.getHeight();
            int size = Math.round(Math.min(mParentHeight, mParentWidth) / 1.33f);
            mBitmap = Bitmap.createScaledBitmap(mBitmap, size, size, false);
            mBitmapWidth = mBitmap.getWidth();
            mParentCenterX = mParentWidth / 2;
            mParentCenterY = mParentHeight / 2;
            mViewLeftY = mParentCenterX - mBitmapWidth / 2;
            mViewTopX = mParentCenterY - mBitmapWidth / 2;
        }

        @Override
        protected void onDraw(Canvas canvas) {
            canvas.save();
            canvas.rotate((float) -mRotationInDegrees, mParentCenterX, mParentCenterY);
            canvas.drawBitmap(mBitmap, mViewLeftY, mViewTopX, null);
            canvas.restore();
        }

    }

}
