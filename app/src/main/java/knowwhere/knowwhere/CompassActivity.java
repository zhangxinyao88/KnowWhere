package knowwhere.knowwhere;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_compass);
        mFrame = findViewById(R.id.frame);
        mCompassArrow = new CompassArrowView(getApplicationContext());
        mFrame.addView(mCompassArrow);
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        if (null != mSensorManager) {
            accelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            magnetometer = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        }
        if (null == accelerometer || null == magnetometer)
            finish();
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
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            mGravity = new float[3];
            System.arraycopy(event.values, 0, mGravity, 0, 3);
        } else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            mGeomagnetic = new float[3];
            System.arraycopy(event.values, 0, mGeomagnetic, 0, 3);
        }
        if (mGravity != null && mGeomagnetic != null) {
            float rotationMatrix[] = new float[9];
            if (SensorManager.getRotationMatrix(rotationMatrix, null, mGravity, mGeomagnetic)) {
                float orientationMatrix[] = new float[3];
                SensorManager.getOrientation(rotationMatrix, orientationMatrix);
                mRotationInDegrees = Math.toDegrees(orientationMatrix[0]);
                mCompassArrow.invalidate();
                mGravity = mGeomagnetic = null;
            }
        }
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
