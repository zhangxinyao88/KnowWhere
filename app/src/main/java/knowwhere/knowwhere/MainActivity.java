package knowwhere.knowwhere;

import android.content.Context;
import android.location.Location;
import android.location.LocationManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

public class MainActivity extends AppCompatActivity {

    private static String TAG = "KnowWhere";

    private LocationManager mLocationManager;
    private Location latestLocation;

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
                //MainActivity.this.latestLocation =
                Log.i(TAG, "mark");
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
}
