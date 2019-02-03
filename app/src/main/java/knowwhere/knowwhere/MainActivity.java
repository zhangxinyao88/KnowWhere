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
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;

import mehdi.sakout.fancybuttons.FancyButton;

public class MainActivity extends AppCompatActivity implements SensorEventListener, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    private static final float MIN_ACCURACY = 10.0f;
    private static final float MIN_DISTANCE = 0.01f;
    private static final float TARGET_DISTANCE = 15;
    private static final long TWO_MIN = 1000 * 60 * 2;
    private static final long POLLING_FREQ = 100;
    private static final int REQUEST_FINE_LOC_PERM_ONCREATE = 200;
    private static final int REQUEST_FINE_LOC_PERM_ONRESUME = 201;

    private FancyButton mMarkButton;
    private FancyButton mTrackButton;
    private FancyButton mStopButton;

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

    private int cnt = 0;

    GPSTracker gps = new GPSTracker(MainActivity.this );
    float latitude = (float)gps.getLatitude();
    float longitude = (float)gps.getLongitude();

    private FusedLocationProviderClient mFusedLocationClient;
    private GoogleApiClient mGoogleApiClient;
    private LocationRequest mLocationRequest;
    private LocationCallback mLocationCallback;

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
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                || checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
            requestPermissions(new String[] {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, REQUEST_FINE_LOC_PERM_ONCREATE);

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

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(MainActivity.this);

        mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                Location bestResult = null;
                float bestAccuracy = Float.MAX_VALUE;
                for (Location location : locationResult.getLocations()) {
                    if (location != null) {
                        if (location.getAccuracy() < bestAccuracy) {
                            bestResult = location;
                            bestAccuracy = location.getAccuracy();
                        }
                    }
                }
                if (mIsRequestingUpdates && bestResult != null) {
                    mCurrentLocation = bestResult;
                    Log.i(TAG, "New location received");

                    cnt++;

                    float dist = mCurrentLocation.distanceTo(mMarkedLocation);
                    TextView tv = (TextView) findViewById(R.id.textView);
                    tv.setText(((int) dist) + "m");
                    //((TextView) findViewById(R.id.lat)).setText(latitude + "m");
                    //((TextView) findViewById(R.id.lng)).setText(longitude + "m");
                    //((TextView) findViewById(R.id.lat)).setText(mCurrentLocation.getLatitude() + "");
                    //((TextView) findViewById(R.id.lng)).setText(mCurrentLocation.getLongitude() + "");
                    if (dist < TARGET_DISTANCE) {
                        Toast.makeText(MainActivity.this, "You are almost there!", Toast.LENGTH_LONG).show();
                    }
                }
            }
        };

        mMarkButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i(TAG, "Marking current location");
                /*mFusedLocationClient = LocationServices.getFusedLocationProviderClient(MainActivity.this);
                mFusedLocationClient.getLastLocation().addOnSuccessListener(new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        if (location != null) {
                            mMarkButton.setVisibility(View.INVISIBLE);
                            mTrackButton.setVisibility(View.VISIBLE);
                            mMarkedLocation = location;
                            Toast.makeText(MainActivity.this, "Current Location Marked", Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(MainActivity.this, "Failed to Mark Current Location", Toast.LENGTH_LONG).show();
                        }
                    }
                });*/
                mMarkedLocation = new Location("");
                mMarkedLocation.setLatitude(38.9066473);
                mMarkedLocation.setLongitude(-77.0726751);
                mMarkButton.setVisibility(View.INVISIBLE);
                mTrackButton.setVisibility(View.VISIBLE);
                Toast.makeText(MainActivity.this, "Current Location Marked", Toast.LENGTH_LONG).show();
            }
        });

        mTrackButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mTrackButton.setVisibility(View.INVISIBLE);
                mStopButton.setVisibility(View.VISIBLE);
                mIsRequestingUpdates = true;
                requestLocationUpdates();
            }
        });

        mStopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mStopButton.setVisibility(View.INVISIBLE);
                mMarkButton.setVisibility(View.VISIBLE);
                mFusedLocationClient.removeLocationUpdates(mLocationCallback);
                Log.i(TAG, "stop updating location");
                mIsRequestingUpdates = false;
                mMarkedLocation = null;
                mRotationInDegrees = 0;
                mCompassArrow.invalidate();

                cnt = 0;
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mIsRequestingUpdates) {
            if (mGoogleApiClient != null && mFusedLocationClient != null) {
                requestLocationUpdates();
            } else {
                buildGoogleApiClient();
            }
        }
        mSensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        mSensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_NORMAL);
    }

    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        mGoogleApiClient.connect();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mIsRequestingUpdates && mFusedLocationClient != null) {
            mFusedLocationClient.removeLocationUpdates(mLocationCallback);
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
                //Log.i(TAG, mGeomagnetic[0] + ", " + mGeomagnetic[1] + ", " + mGeomagnetic[2]);
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
        if (grantResults.length <= 0 || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "This app requires ACCESS_FINE_LOCATION and ACCESS_COARSE_LOCATION permissions", Toast.LENGTH_LONG).show();
            finish();
        }
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        requestLocationUpdates();
    }

    public void requestLocationUpdates() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(100);
        mLocationRequest.setFastestInterval(100);
        mLocationRequest.setMaxWaitTime(100);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());
        }
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

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
