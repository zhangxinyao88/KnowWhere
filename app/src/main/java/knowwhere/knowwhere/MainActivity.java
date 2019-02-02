package knowwhere.knowwhere;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    private static final float MIN_ACCURACY = 5.0f;
    private static final float MIN_DISTANCE = 10.0f;
    private static final float TARGET_DISTANCE = 1000;
    private static final long TWO_MIN = 1000 * 60 * 2;
    private static final long POLLING_FREQ = Long.MAX_VALUE;
    private static final int REQUEST_FINE_LOC_PERM_ONCREATE = 200;
    private static final int REQUEST_FINE_LOC_PERM_ONRESUME = 201;

    private Button mMarkButton;
    private Button mTrackButton;
    private Button mStopButton;

    private LocationManager mLocationManager;
    private LocationListener mLocationListener;

    private final String TAG = "KnowWhere";

    private Location mMarkedLocation;
    private boolean mIsRequestingUpdates;

    private CompassFragment mCompassFragment;
    private android.app.FragmentManager mFragmentManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mMarkButton = findViewById(R.id.markButton);
        mTrackButton = findViewById(R.id.trackButton);
        mTrackButton.setVisibility(View.INVISIBLE);
        mStopButton = findViewById(R.id.stopButton);
        mStopButton.setVisibility(View.INVISIBLE);

        // Get a reference to the Fragment
        mFragmentManager = getFragmentManager();
        mCompassFragment = (CompassFragment) mFragmentManager.findFragmentById(R.id.compass);

        if (null == (mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE)))
            finish();
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
            requestPermissions(new String[] {Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_FINE_LOC_PERM_ONCREATE);

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
                startActivity(new Intent(MainActivity.this, CompassActivity.class));
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
                if (null == mMarkedLocation || location.getAccuracy() <= mMarkedLocation.getAccuracy()) {
                    mMarkedLocation = location;
                    updateDisplay(location);
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
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mIsRequestingUpdates) {
            mLocationManager.removeUpdates(mLocationListener);
            Log.i(TAG, "Removing location updates in onPause()");
        }
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

}
