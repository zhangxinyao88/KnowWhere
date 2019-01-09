package knowwhere.knowwhere;

import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity implements LocationListener {

    private static final float MIN_ACCURACY = 5.0f;
    private static final long ONE_MIN = 1000 * 60;

    private static String TAG = "KnowWhere";

    private float mMinDistance = 10000.0f;

    private long mMinTime = 5000;
    private Location mMarkedLocation;
    private LocationManager mLocationManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if ((mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE)) == null) {
            Log.i(TAG, "Fail to get location service");
            finish();
        }

        final Button markButton = findViewById(R.id.markButton);
        markButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.i(TAG, "Marking current location");
                getLocationUpdates();
                if (mMarkedLocation != null) {
                    Log.i(TAG, "Location marked");
                    Toast.makeText(MainActivity.this, "Location Marked", Toast.LENGTH_LONG).show();
                } else {
                    Log.i(TAG, "Location error");
                    Toast.makeText(MainActivity.this, "Failed to Mark Location", Toast.LENGTH_LONG).show();
                }
            }
        });

        final Button trackButton = findViewById(R.id.trackButton);
        trackButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //MainActivity.this.latestLocation =
                Log.i(TAG, "track");
            }
        });
    }

    private void getLocationUpdates() {
        if (Build.VERSION.SDK_INT >= 23 && ContextCompat.checkSelfPermission(getApplicationContext(),
                "android.permission.ACCESS_FINE_LOCATION") != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(getApplicationContext(),
                "android.permission.ACCESS_COARSE_LOCATION") != PackageManager.PERMISSION_GRANTED) {
            Log.i(TAG, "Request permission");
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[] {"android.permission.ACCESS_FINE_LOCATION",
                            "android.permission.ACCESS_COARSE_LOCATION"}, 0);
        } else {
            Log.i(TAG, "Start reading location");
            if (null != mLocationManager.getProvider(LocationManager.NETWORK_PROVIDER))
                mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, mMinTime, mMinDistance,
                        this);
            if (null != mLocationManager.getProvider(LocationManager.GPS_PROVIDER))
                mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, mMinTime, mMinDistance,
                        this);
            Location bestResult = null;
            float bestAccuracy = Float.MAX_VALUE;
            long bestAge = Long.MIN_VALUE;
            for (String provider : new String[] {LocationManager.NETWORK_PROVIDER, LocationManager.GPS_PROVIDER}) {
                Location location = mLocationManager.getLastKnownLocation(provider);
                Log.i(TAG, provider);
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
            if (bestAccuracy > MIN_ACCURACY || (System.currentTimeMillis() - bestAge) > ONE_MIN) {
                mMarkedLocation = null;
            } else {
                mMarkedLocation = bestResult;
            }
        }

    }

    @Override
    public void onLocationChanged(Location location) {

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

}
