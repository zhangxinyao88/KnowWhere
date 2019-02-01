package knowwhere.knowwhere;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    private static final float MIN_ACCURACY = 5.0f;
    private static final float MIN_DISTANCE = 10.0f;
    private static final long TWO_MIN = 1000 * 60 * 2;
    private static final long POLLING_FREQ = 1000 * 10;

    private LocationManager mLocationManager;
    private LocationListener mLocationListener;

    private final String TAG = "KnowWhere";

    private Location mBestReading;
    private Location mMarkedLocation;
    private boolean mIsRequestingUpdates;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (null == (mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE)))
            finish();
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
            requestPermissions(new String[] {Manifest.permission.ACCESS_FINE_LOCATION}, 0);

        final Button markButton = findViewById(R.id.markButton);
        markButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i(TAG, "Marking current location");
                Location location = getBestLastKnownLocation();
                if (location != null) {
                    mMarkedLocation = location;
                    Toast.makeText(MainActivity.this, "Current Location Marked", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(MainActivity.this, "Failed to Mark Current Location", Toast.LENGTH_LONG).show();
                }
            }
        });

        final Button trackButton = findViewById(R.id.trackButton);
        trackButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mMarkedLocation == null) {
                    Toast.makeText(MainActivity.this, "No Marked Location", Toast.LENGTH_LONG).show();
                } else {
                    mLocationListener = getLocationListener();
                    installLocationListeners();
                }
            }
        });
    }

    private void installLocationListeners() {
        if (null == mBestReading || mBestReading.getAccuracy() > MIN_ACCURACY || mBestReading.getTime() < System.currentTimeMillis() - TWO_MIN) {
            if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[] {Manifest.permission.ACCESS_FINE_LOCATION}, 0);
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
                mIsRequestingUpdates = true;
            }
            if (null != mLocationManager.getProvider(LocationManager.GPS_PROVIDER)) {
                Log.i(TAG, "GPS location updates requested");
                mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, POLLING_FREQ, MIN_DISTANCE, mLocationListener);
                mIsRequestingUpdates = true;
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
        if (bestAccuracy > MIN_ACCURACY || System.currentTimeMillis() - bestAge > TWO_MIN) {
            return null;
        } else {
            return bestResult;
        }
    }

    private LocationListener getLocationListener() {
        return new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                Log.i(TAG, "New location received");
                if (null == mBestReading || location.getAccuracy() <= mBestReading.getAccuracy()) {
                    mBestReading = location;
                    updateDisplay(location);
                    if (mBestReading.getAccuracy() < MIN_ACCURACY) {
                        mLocationManager.removeUpdates(mLocationListener);
                        Log.i(TAG, "stop updating location");
                        mIsRequestingUpdates = false;
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

    private void updateDisplay(Location location) {
        //TODO: implement here
    }

}
