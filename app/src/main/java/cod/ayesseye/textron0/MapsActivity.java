package cod.ayesseye.textron0;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.view.WindowCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import cod.ayesseye.textron0.databinding.ActivityMapsBinding;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    //Map object
    private GoogleMap mMap;

    //Timer Variables
    private ScheduledExecutorService scheduler;
    private int seconds = 0;
    private boolean isPaused = false;
    private boolean isRunning = false;
    TextView timerText;

    Button home, weather, startWalk, stopWalk, pauseResumeWalk;


    //Route tracking variables
    private ArrayList<LatLng> routePoints = new ArrayList<>();
    private Polyline routePolyline;

    //Location objects
    private FusedLocationProviderClient fusedLocationProviderClient;
    private com.google.android.gms.location.LocationCallback locationCallback;

    //Distance + Pace variables
    private float totalDistanceMeters = 0f;
    private long startTimeMillis;




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ActivityMapsBinding binding = ActivityMapsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        assert mapFragment != null;
        mapFragment.getMapAsync(this);

        //Referemces to xml
        home = findViewById(R.id.btn_home);
        weather = findViewById(R.id.btn_weather);
        startWalk = findViewById(R.id.btn_start);
        stopWalk = findViewById(R.id.btn_stop);
        timerText = findViewById(R.id.timerText);
        pauseResumeWalk = findViewById(R.id.btn_pause_resume);

        home.setOnClickListener(v -> changePage(MainActivity.class));
        weather.setOnClickListener(v -> changePage(OpenWeather.class));
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        //Initialize gps Provider
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        startWalk.setOnClickListener(v -> {
            startTimer();
            startTrackingRoute();
        });
        stopWalk.setOnClickListener(v -> {
            stopTimer();
            stopTrackingRoute();
        });
        pauseResumeWalk.setOnClickListener(v -> pauseResumeTimer());
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;

        //Disable zoom control
        mMap.getUiSettings().setZoomControlsEnabled(false);

        //Check for location permission
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            return;
        }
        mMap.setMyLocationEnabled(true);

        moveCameraToCurrentLocation(); // One time location, once when the activity starts
    }

    //Moves camera to location once activity starts
    @SuppressLint("MissingPermission")
    private void moveCameraToCurrentLocation() {
        FusedLocationProviderClient fusedLocationProviderClient =
                LocationServices.getFusedLocationProviderClient(this);

        fusedLocationProviderClient.getLastLocation().addOnSuccessListener(location -> {
            if (location != null) {
                LatLng currentLocation = new LatLng(location.getLatitude(), location.getLongitude());
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 16));

            }
        });
    }

    @Override
    public void onRequestPermissionsResult (int requestCode, @NonNull String[] permissions, @NonNull int[] grantResult) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResult);

        if (requestCode == 1) {
            if (grantResult.length > 0 && grantResult[0] == PackageManager.PERMISSION_GRANTED) {
                //Permission granted => reload map with location enabled
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                        == PackageManager.PERMISSION_GRANTED ) {
                    mMap.setMyLocationEnabled(true);
                    moveCameraToCurrentLocation();
                }
            }
        }
    }

    private void changePage(Class<?> targetActivity) {
        Intent intent = new Intent(this, targetActivity);
        startActivity(intent);
    }

    private void startTimer() {
        if (scheduler != null && !scheduler.isShutdown())
            return;

        seconds = 0;
        isRunning = true;
        isPaused = false;

        //Create 1 second loop
        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleWithFixedDelay(() -> {
            if (!isPaused) {
                seconds++;
            }

            int minutes = seconds / 60;
            int secs = seconds % 60;

            String formatted = String.format("%2d:%2d", minutes, secs);

            //Update UI
            runOnUiThread(() -> timerText.setText(formatted));
        }, 1, 1, TimeUnit.SECONDS);
    }

    private void pauseResumeTimer() {
        if (!isRunning)
            return;

        isPaused = !isPaused;

        if (isPaused) {
            pauseResumeWalk.setText("Resume");
        } else {
            pauseResumeWalk.setText("Pause");
        }
    }

    private void stopTimer() {
        if (scheduler != null) {
            scheduler.shutdownNow();
            scheduler = null;
        }
        timerText.setText(" 0: 0");
    }

    @SuppressLint("MissingPermission")
    private void startTrackingRoute() {

        //Reset old Data
        startTimeMillis = System.currentTimeMillis(); // start timer
        totalDistanceMeters = 0f;  //Reset Distance
        routePoints.clear();


        //Creates LocationRequest for continuous GPS
        LocationRequest request = new LocationRequest.Builder(
                Priority.PRIORITY_HIGH_ACCURACY,
                2000
        ).setMinUpdateIntervalMillis(1000)
                .setMinUpdateDistanceMeters(0)
                .build();


        //Build what happens on each GPS update | Android calls this every 2 secs
       locationCallback = new com.google.android.gms.location.LocationCallback() {
           @Override
           public void onLocationResult(com.google.android.gms.location.LocationResult result) {
               Location location =  result.getLastLocation();
               if (location == null) {
                   return;
               }

               LatLng point = new LatLng(location.getLatitude(), location.getLongitude());
               float segmentMeters = 0f;

               //Calculate segments distance
               if (!routePoints.isEmpty()) {
                   LatLng lastPoint = routePoints.get(routePoints.size() - 1);

                   float[] results = new float[1];
                   Location.distanceBetween(
                           lastPoint.latitude, lastPoint.longitude,
                           point.latitude, point.longitude,
                           results
                   );

                   segmentMeters = results[0];
                   totalDistanceMeters += segmentMeters;
               }

               //Save new point
               routePoints.add(point);

               drawRoutePolyline();
           }
       };

        fusedLocationProviderClient.requestLocationUpdates(request, locationCallback, Looper.getMainLooper());
    }

    //Stops updates and resets everything
    private void stopTrackingRoute() {
        //Stop Tracking
        if (locationCallback != null) {
            fusedLocationProviderClient.removeLocationUpdates(locationCallback);
        }

        //Preparing walk data
        float distanceKm = totalDistanceMeters / 1000f;
        String avgPace = getAveragePace();

        int minutes = seconds / 60;
        int secs = seconds % 60;
        String duration = minutes + ":" + secs;

        long timestamp = System.currentTimeMillis();

        Log.e("STOP", "routePoints size at STOP = " + routePoints.size());
        ArrayList<HashMap<String, Double>> routeForFirebase = convertRouteToFirebase();

        WalkModel walk = new WalkModel(
                distanceKm, avgPace, duration, timestamp, routeForFirebase
        );

        //Save to firebase
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference ref = FirebaseDatabase.getInstance()
                .getReference("users")
                .child(uid)
                .child("walks");

        ref.push().setValue(walk)
                .addOnSuccessListener(a -> Log.d("FIREBASE", "Walk save successfully!"))
                .addOnFailureListener(e -> Log.e("FIREBASE", "Failed: " + e.getMessage()));

        //Reset local data
        if (routePolyline != null){
            routePolyline.remove();
        }

        routePoints.clear();
        totalDistanceMeters = 0f; //reset
    }

    private void drawRoutePolyline() {
        if (mMap == null || routePoints.size() < 2)
            return;

        if (routePolyline != null) {
            routePolyline.remove();
        }

        PolylineOptions options = new PolylineOptions()
                .addAll(routePoints)
                .width(12)
                .color(Color.BLUE)
                .geodesic(true);

        routePolyline = mMap.addPolyline(options);
    }

    private String getCurrentPace(float segmentMeters) {
        if (segmentMeters <= 0){
            return "0:00";
        }

        float segmentKm = segmentMeters / 1000f;
        float seconds  = 2f; // Running every 2 seconds

        float paceSecPerkm = seconds / segmentKm;

        int minutes = (int) (paceSecPerkm / 60);
        int sec = (int) (paceSecPerkm % 60);

        return String.format(Locale.getDefault(), "%d:%02d min/km", minutes, sec);
    }

    private String getAveragePace() {
        float totalKm = totalDistanceMeters / 1000f;
        if (totalKm <= 0) {
            return "0:00";
        }

        long elapsedMillis = System.currentTimeMillis() - startTimeMillis;
        float seconds = elapsedMillis / 1000f;

        float paceSecPerKm = seconds / totalKm;

        int minutes = (int) (paceSecPerKm / 60);
        int sec = (int) (paceSecPerKm % 60);

        return String.format(Locale.getDefault(), "%d:%02d min/km", minutes, sec);
    }

    private ArrayList<HashMap<String, Double>> convertRouteToFirebase() {
        ArrayList<HashMap<String, Double>> routeList = new ArrayList<>();

        for (LatLng p : routePoints) {
            HashMap<String, Double> map = new HashMap<>();
            map.put("lat", p.latitude);
            map.put("lng", p.longitude);
            routeList.add(map);
        }

        return routeList;
    }


}
