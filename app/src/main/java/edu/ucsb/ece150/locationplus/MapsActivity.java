package edu.ucsb.ece150.locationplus;

import static com.google.android.gms.location.Geofence.NEVER_EXPIRE;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.GnssStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.internal.Constants;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingClient;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;

public class MapsActivity extends AppCompatActivity implements LocationListener, OnMapReadyCallback {

    private static final String TAG = "Location: ";
    private static final String TAG2 = "Button: ";
    final private int REQUEST_CODE_ASK_PERMISSIONS = 125;

    private Geofence mGeofence;
    private GeofencingClient mGeofencingClient;
    private PendingIntent mPendingIntent = null;

    private GnssStatus.Callback mGnssStatusCallback;
    private GoogleMap mMap;
    private Marker currentLocation;
    private Marker destination;
    private Circle circle;
    private double latitude;
    private double longitude;
    private LocationManager mLocationManager;
    private FloatingActionButton cancelFab;
    private FloatingActionButton backFab;
    private TextView satelliteCountTextView;
    private TextView satelliteFixCountTextView;

    private ArrayList<Satellite> satelliteArray;
    private ArrayAdapter adapter;
    private ListView satelliteList;

    private boolean autoCameraButtonPressed;

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        final Toolbar mToolbar = findViewById(R.id.appToolbar);
        setSupportActionBar(mToolbar);

        satelliteCountTextView = findViewById(R.id.satelliteCount);
        satelliteFixCountTextView = findViewById(R.id.satelliteFixCount);


        backFab = findViewById(R.id.backFab);
        backFab.setVisibility(View.INVISIBLE);
        backFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                satelliteList.setVisibility(View.INVISIBLE);
                backFab.setVisibility(View.INVISIBLE);
            }
        });

        if(satelliteArray == null){
            satelliteArray = new ArrayList<>();
        }
        satelliteList = findViewById(R.id.satelliteList);
        satelliteList.setVisibility(View.INVISIBLE);
        satelliteList.setBackgroundColor(Color.WHITE);
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, satelliteArray);
        satelliteList.setAdapter(adapter);


        cancelFab = findViewById(R.id.cancelFab);
        cancelFab.setVisibility(View.INVISIBLE);
        cancelFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                removeEverything();
            }
        });

        // Set up Google Maps
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        SharedPreferences prefs = getSharedPreferences("LocationPlusStorage", MODE_PRIVATE);
        //load value of autoCameraButtonPressed
        autoCameraButtonPressed = prefs.getBoolean("autoCameraButton", false);

        //load destination parameters
        latitude = (double) prefs.getFloat("latitude", 0);
        longitude = (double) prefs.getFloat("longitude", 0);

        // Set up Geofencing Client
        mGeofencingClient = LocationServices.getGeofencingClient(MapsActivity.this);

        // Set up Satellite List
        mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        mGnssStatusCallback = new GnssStatus.Callback() {
            @RequiresApi(api = Build.VERSION_CODES.O)
            @Override
            public void onSatelliteStatusChanged(GnssStatus status) {
                // [TODO] Implement behavior when the satellite status is updated
                super.onSatelliteStatusChanged(status);

                satelliteArray.clear();

                int satelliteCount = status.getSatelliteCount();
                int satelliteFixCount = 0;

                for(int sat = 0; sat<satelliteCount; sat++ ){
                    double azimuth = status.getAzimuthDegrees(sat);
                    double elevation = status.getElevationDegrees(sat);
                    double carrierFrequency = status.getCarrierFrequencyHz(sat);
                    double noiseDensity = status.getCn0DbHz(sat);
                    int constellationName = status.getConstellationType(sat);
                    int SVID = status.getSvid(sat);
                    if(status.usedInFix(sat)){
                        satelliteFixCount++;
                    }
                    Satellite satellite = new Satellite(sat + 1,azimuth,elevation,carrierFrequency,noiseDensity,constellationName,SVID);
                    satelliteArray.add(satellite);
                }
                Log.e("SATELLITEINFO", "Satelitte Total Count: "+ satelliteCount);
                Log.e("SATELLITEINFO", "Satelitte Fix Count: "+ satelliteFixCount);
                satelliteCountTextView.setText("" + satelliteCount);
                satelliteFixCountTextView.setText("" + satelliteFixCount);
                adapter.notifyDataSetChanged();

            }
        };

        // [TODO] Additional setup for viewing satellite information (lists, adapters, etc.)
        satelliteList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                double azimuth = satelliteArray.get(position).azimuth;
                double elevation = satelliteArray.get(position).elevation;
                double carrierFrequency = satelliteArray.get(position).carrierFrequency;
                double noiseDensity = satelliteArray.get(position).noiseDensity;
                int constellationName = satelliteArray.get(position).constellationName;
                int SVID = satelliteArray.get(position).SVID;
                String constellationDictionary[] = {"UNKNOWN", "GPS", "SBAS", "GLOSNASS", "QZSS", "BEIDOU", "GALILEO", "IRNSS"};

                AlertDialog.Builder builder
                        = new AlertDialog
                        .Builder(MapsActivity.this);
                builder.setTitle("Satellite " + satelliteArray.get(position).satelliteNum);
                builder.setMessage("Azimuth: " + azimuth + "\u00B0" + "\n"
                        + "Elevation: " + elevation + "\u00B0" + "\n" + "\n"
                        + "Carrier Frequency: " + carrierFrequency + "Hz" + "\n"
                        + "C/ND: " + noiseDensity + "dB Hz" + "\n" + "\n"
                        + "Constellation: " + constellationDictionary[constellationName] + "\n"
                        + "SVID: " + SVID);
                builder
                        .setPositiveButton(
                                "OK",
                                new DialogInterface
                                        .OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.cancel();
                                    }
                                });
                AlertDialog alertDialog = builder.create();
                alertDialog.show();
            }
        });

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        final Toolbar mToolbar = (Toolbar) findViewById(R.id.appToolbar);
        mToolbar.inflateMenu(R.menu.menu_buttons);
        mToolbar.setOnMenuItemClickListener(item -> onOptionsItemSelected(item));
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.satelliteInfoButton:
                Log.e(TAG2, "satelliteInfoButton pressed");
                satelliteList.setVisibility(View.VISIBLE);
                backFab.setVisibility(View.VISIBLE);
                break;
            case R.id.autoCameraButton:
                Log.e(TAG2, "autoCameraButton pressed");
                autoCameraButtonPressed = !autoCameraButtonPressed;
                break;
            default:
                break;
        }
        return true;
    }

    @SuppressLint("MissingPermission")
    public void createDestinationMarker(LatLng latLng){
        cancelFab.show();
        mGeofence = new Geofence.Builder()
                .setRequestId("destination")
                .setCircularRegion(
                        latLng.latitude,
                        latLng.longitude,
                        100
                )
                .setExpirationDuration(-1)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
                .build();
        Log.e(TAG, mGeofence.toString());
        mGeofencingClient.addGeofences(
                getGeofenceRequest(),
                getGeofencePendingIntent());
        destination = mMap.addMarker(new MarkerOptions()
                .position(latLng)
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
                .title("Destination"));
        circle = mMap.addCircle(new CircleOptions()
                .center(latLng)
                .radius(100)
                .strokeColor(Color.RED)
                .strokeWidth(5)
                .fillColor(Color.argb(128,255,0,0)));
    }

    public void removeEverything(){
        destination.remove();
        circle.remove();
        mGeofencingClient.removeGeofences(getGeofencePendingIntent());
        final LatLng loc = new LatLng(0, 0);
        destination.setPosition(loc);
        SharedPreferences.Editor mEditor = getSharedPreferences("LocationPlusStorage", MODE_PRIVATE).edit();
        mEditor.putFloat("latitude", 0).apply();
        mEditor.putFloat("longitude", 0).apply();
        cancelFab.hide();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // [TODO] Implement behavior when Google Maps is ready
        if (destination == null && latitude != 0 && longitude != 0) {
            LatLng loc = new LatLng(latitude, longitude);
            createDestinationMarker(loc);
        }
        // [TODO] In addition, add a listener for long clicks (which is the starting point for
        // creating a Geofence for the destination and listening for transitions that indicate
        // arrival)
        mMap.setOnMapLongClickListener(new GoogleMap.OnMapLongClickListener() {
            @Override
            public void onMapLongClick(LatLng latLng) {
                if (destination == null || (destination.getPosition().latitude == 0 && destination.getPosition().longitude == 0)) {
                    AlertDialog.Builder builder
                            = new AlertDialog
                            .Builder(MapsActivity.this);
                    builder.setTitle("Confirm Destination");
                    builder.setMessage("Set postition " + latLng.toString() + " as your destination?");
                    builder
                            .setPositiveButton(
                                    "Yes",
                                    new DialogInterface
                                            .OnClickListener() {

                                        @SuppressLint("MissingPermission")
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            createDestinationMarker(latLng);
                                        }
                                    });
                    builder
                            .setNegativeButton(
                                    "No",
                                    new DialogInterface
                                            .OnClickListener() {

                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            dialog.cancel();
                                        }
                                    });
                    AlertDialog alertDialog = builder.create();
                    alertDialog.show();
                }
            }
        });

    }

    @Override
    public void onLocationChanged(Location location) {
        // [TODO] Implement behavior when a location update is received
        Log.e(TAG,"Location Changed");

        if(currentLocation != null){
            currentLocation.remove();
        }

        final LatLng loc = new LatLng(location.getLatitude(), location.getLongitude());

        currentLocation = mMap.addMarker(new MarkerOptions()
                .position(loc)
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
                .title("Current Location"));

        //auto-centering
        if(autoCameraButtonPressed == true){
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(loc, 15));
        }
    }

    /*
     * The following three methods onProviderDisabled(), onProviderEnabled(), and onStatusChanged()
     * do not need to be implemented -- they must be here because this Activity implements
     * LocationListener.
     *
     * You may use them if you need to.
     */
    @Override
    public void onProviderDisabled(String provider) {}

    @Override
    public void onProviderEnabled(String provider) {}

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {}

    private GeofencingRequest getGeofenceRequest() {
        // [TODO] Set the initial trigger (i.e. what should be triggered if the user is already
        // inside the Geofence when it is created)

        return new GeofencingRequest.Builder()
                .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER )
                .addGeofence(mGeofence)
                .build();
    }

    private PendingIntent getGeofencePendingIntent() {
        if(mPendingIntent != null)
            return mPendingIntent;

        Intent intent = new Intent(MapsActivity.this, GeofenceBroadcastReceiver.class);
        mPendingIntent = PendingIntent.getBroadcast(MapsActivity.this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        return mPendingIntent;
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    protected void onStart() throws SecurityException {
        super.onStart();

        // [TODO] Ensure that necessary permissions are granted (look in AndroidManifest.xml to
        // see what permissions are needed for this app)
        int hasBackgroundLocationPermissions = checkSelfPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION);
        int hasCoarseLocationPermissions = checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION);
        int hasFineLocationPermissions = checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION);
        int hasInternetPermissions = checkSelfPermission(Manifest.permission.INTERNET);


        if (hasBackgroundLocationPermissions != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[] {Manifest.permission.ACCESS_BACKGROUND_LOCATION},
                    REQUEST_CODE_ASK_PERMISSIONS);
        }
        if (hasCoarseLocationPermissions != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[] {Manifest.permission.ACCESS_COARSE_LOCATION},
                    REQUEST_CODE_ASK_PERMISSIONS);
        }
        if (hasFineLocationPermissions != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[] {Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_CODE_ASK_PERMISSIONS);
        }
        if (hasInternetPermissions != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[] {Manifest.permission.INTERNET},
                    REQUEST_CODE_ASK_PERMISSIONS);
        }
        if (hasBackgroundLocationPermissions == PackageManager.PERMISSION_GRANTED && hasCoarseLocationPermissions == PackageManager.PERMISSION_GRANTED && hasFineLocationPermissions == PackageManager.PERMISSION_GRANTED && hasInternetPermissions == PackageManager.PERMISSION_GRANTED) {
            mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 0, this);
            mLocationManager.registerGnssStatusCallback(mGnssStatusCallback);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        // [TODO] Data recovery
    }

    @Override
    protected void onPause() {
        super.onPause();

        // [TODO] Data saving
        SharedPreferences.Editor  mEditor = getSharedPreferences("LocationPlusStorage", MODE_PRIVATE).edit();
        //save autoCameraButtonPressed state
        mEditor.putBoolean("autoCameraButton", autoCameraButtonPressed).apply();

        //save destination
        if(destination != null){
            double latitude = destination.getPosition().latitude;
            double longitude = destination.getPosition().longitude;
            mEditor.putFloat("latitude", (float)latitude).apply();
            mEditor.putFloat("longitude", (float)longitude).apply();
        }

    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    protected void onStop() {
        super.onStop();

        mLocationManager.removeUpdates(this);
        mLocationManager.unregisterGnssStatusCallback(mGnssStatusCallback);
    }
}
