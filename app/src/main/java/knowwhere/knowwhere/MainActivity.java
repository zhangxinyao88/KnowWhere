package knowwhere.knowwhere;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private static final float MIN_ACCURACY = 5.0f;
    private static final float MIN_DISTANCE = 10.0f;
    private static final float TARGET_DISTANCE = 1000;
    private static final long TWO_MIN = 1000 * 60 * 2;
    private static final long POLLING_FREQ = 1000;
    private static final int REQUEST_FINE_LOC_PERM_ONCREATE = 200;
    private static final int REQUEST_FINE_LOC_PERM_ONRESUME = 201;

    private Button mMarkButton;
    private Button mTrackButton;
    private Button mStopButton;

    private LocationManager mLocationManager;
    private LocationListener mLocationListener;

    private final String TAG = "KnowWhere";

    private Location mMarkedLocation;
    private Location mCurrentLocation;
    private boolean mIsRequestingUpdates;

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
        setContentView(R.layout.activity_main);
        mMarkButton = findViewById(R.id.markButton);
        mTrackButton = findViewById(R.id.trackButton);
        mTrackButton.setVisibility(View.INVISIBLE);
        mStopButton = findViewById(R.id.stopButton);
        mStopButton.setVisibility(View.INVISIBLE);

        if (null == (mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE)))
            finish();
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
            requestPermissions(new String[] {Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_FINE_LOC_PERM_ONCREATE);

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

        mGravity = new float[3];
        mGeomagnetic = new float[3];

        mMarkButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i(TAG, "Marking current location");
                Location location = getBestLastKnownLocation();
                if (location != null) {
                    mMarkButton.setVisibility(View.INVISIBLE);
                    mTrackButton.setVisibility(View.VISIBLE);
                    mMarkedLocation = location;
                    Toast.makeText(MainActivity.this, "Current Location Marked", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(MainActivity.this, "Failed to Mark Current Location", Toast.LENGTH_LONG).show();
                }
            }
        });

        mTrackButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mTrackButton.setVisibility(View.INVISIBLE);
                mStopButton.setVisibility(View.VISIBLE);
                mIsRequestingUpdates = true;
                mLocationListener = getLocationListener();
                installLocationListeners();
            }
        });

        mStopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mStopButton.setVisibility(View.INVISIBLE);
                mMarkButton.setVisibility(View.VISIBLE);
                mLocationManager.removeUpdates(mLocationListener);
                Log.i(TAG, "stop updating location");
                mIsRequestingUpdates = false;
                mMarkedLocation = null;
                mRotationInDegrees = 0;
                mCompassArrow.invalidate();
            }
        });
    }

    private void installLocationListeners() {
        if (null == mMarkedLocation || mMarkedLocation.getAccuracy() > MIN_ACCURACY || mMarkedLocation.getTime() < System.currentTimeMillis() - TWO_MIN) {
            if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[] {Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_FINE_LOC_PERM_ONRESUME);
            } else {
                continueInstallLocationListeners();
            }
        }
    }

    private void continueInstallLocationListeners() {
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            if (null != mLocationManager.getProvider(LocationManager.NETWORK_PROVIDER)) {
                Log.i(TAG, "Network location updates requested");
                mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, POLLING_FREQ, MIN_DISTANCE, mLocationListener);
            }
            if (null != mLocationManager.getProvider(LocationManager.GPS_PROVIDER)) {
                Log.i(TAG, "GPS location updates requested");
                mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, POLLING_FREQ, MIN_DISTANCE, mLocationListener);
            }
        }
    }

    private Location getBestLastKnownLocation() {
        Location bestResult = null;
        float bestAccuracy = Float.MAX_VALUE;
        long bestAge = Long.MIN_VALUE;
        for (String provider : mLocationManager.getAllProviders()) {
            if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                Location location = mLocationManager.getLastKnownLocation(provider);
                if (location != null) {
                    float accuracy = location.getAccuracy();
                    long time = location.getTime();
                    if (accuracy < bestAccuracy) {
                        bestResult = location;
                        bestAccuracy = accuracy;
                        bestAge = time;
                    }
                }
            }
        }
        /*if (bestAccuracy > MIN_ACCURACY || System.currentTimeMillis() - bestAge > TWO_MIN) {
            return null;
        } else {
            return bestResult;
        }*/
        return bestResult;
    }

    private LocationListener getLocationListener() {
        return new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                Log.i(TAG, "New location received");
                if (null == mCurrentLocation || location.getAccuracy() <= mCurrentLocation.getAccuracy()) {
                    mCurrentLocation = location;

                    TextView tv = (TextView) findViewById(R.id.textView);
                    tv.setText((int)mCurrentLocation.distanceTo(mMarkedLocation) + "m");
                    //updateDisplay(location);
                    //mCompassArrow.invalidate();

                    if (location.distanceTo(mMarkedLocation) < TARGET_DISTANCE) {
                        Toast.makeText(MainActivity.this, "You are almost there!", Toast.LENGTH_LONG).show();
                    }
                }
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {

            }

            @Override
            public void onProviderEnabled(String provider) {

            }

            @Override
            public void onProviderDisabled(String provider) {

            }
        };
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mIsRequestingUpdates) {
            installLocationListeners();
        }
        mSensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        mSensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mIsRequestingUpdates) {
            mLocationManager.removeUpdates(mLocationListener);
            Log.i(TAG, "Removing location updates in onPause()");
        }
        mSensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (mIsRequestingUpdates && mCurrentLocation != null && mMarkedLocation != null) {
        //if (mCurrentLocation != null && mMarkedLocation != null) {
            final float alpha = 0.97f;

            Log.i(TAG, "onSensorChanged");
            if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                mGravity[0] = alpha * mGravity[0] + (1 - alpha) * event.values[0];
                mGravity[1] = alpha * mGravity[1] + (1 - alpha) * event.values[1];
                mGravity[2] = alpha * mGravity[2] + (1 - alpha) * event.values[2];
                //mGravity = new float[3];
                //System.arraycopy(event.values, 0, mGravity, 0, 3);
            } else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
                Log.i(TAG, mGeomagnetic[0] + ", " + mGeomagnetic[1] + ", " + mGeomagnetic[2]);
                mGeomagnetic[0] = alpha * mGeomagnetic[0] + (1 - alpha) * event.values[0];
                mGeomagnetic[1] = alpha * mGeomagnetic[1] + (1 - alpha) * event.values[1];
                mGeomagnetic[2] = alpha * mGeomagnetic[2] + (1 - alpha) * event.values[2];
                //mGeomagnetic = new float[3];
                //System.arraycopy(event.values, 0, mGeomagnetic, 0, 3);
            }
            if (mGravity != null && mGeomagnetic != null) {
                float rotationMatrix[] = new float[9];
                if (SensorManager.getRotationMatrix(rotationMatrix, null, mGravity, mGeomagnetic)) {
                    float orientationMatrix[] = new float[3];
                    SensorManager.getOrientation(rotationMatrix, orientationMatrix);
                    mRotationInDegrees = Math.toDegrees(orientationMatrix[0]);

                    mRotationInDegrees = (mRotationInDegrees + 360) % 360;
                    mRotationInDegrees -= bearing(mCurrentLocation, mMarkedLocation);

                    Log.i(TAG, "updating canvas");
                    mCompassArrow.invalidate();
                    Log.i(TAG, "canvas updated");
                    //mGravity = mGeomagnetic = null;
                }
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

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            if (REQUEST_FINE_LOC_PERM_ONRESUME == requestCode) {
                continueInstallLocationListeners();
            }
        } else {
            Toast.makeText(this, "This app requires ACCESS_FINE_LOCATION permission", Toast.LENGTH_LONG).show();
        }
    }

    private void updateDisplay(Location location) {
        //TODO: implement here
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
