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
import android.os.Bundle;
import android.os.Handler;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import cod.ayesseye.textron0.databinding.ActivityMapsBinding;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;

    private ScheduledExecutorService scheduler;
    private int seconds = 0;
    private boolean isPaused = false;
    private boolean isRunning = false;
    TextView timerText;

    Button home, weather, startWalk, stopWalk, pauseResumeWalk;

    private Handler handler = new Handler();
    private ArrayList<LatLng> routePoints = new ArrayList<>();
    private Polyline routePolyline;
    private Marker lastMarker;

    private Runnable locationRunnable;



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

        home = findViewById(R.id.btn_home);
        weather = findViewById(R.id.btn_weather);
        startWalk = findViewById(R.id.btn_start);
        stopWalk = findViewById(R.id.btn_stop);
        timerText = findViewById(R.id.timerText);
        pauseResumeWalk = findViewById(R.id.btn_pause_resume);

        home.setOnClickListener(v -> changePage(MainActivity.class));
        weather.setOnClickListener(v -> changePage(OpenWeather.class));
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        startWalk.setOnClickListener(v -> {
            startTimer();
            startTrackingRoute();
        });
        stopWalk.setOnClickListener(v -> {
            stopTimer();
            startTrackingRoute();
        });
        pauseResumeWalk.setOnClickListener(v -> pauseResumeTimer());
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;

        //Enable zoom control
        mMap.getUiSettings().setZoomControlsEnabled(false);

        //Fortarea bar ului deasupra g Maps
        findViewById(R.id.bottomBar).bringToFront();

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

        moveCameraToCurrentLocation();
    }

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

        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleWithFixedDelay(() -> {

            if (!isPaused) {
                seconds++;
            }
            int minutes = seconds / 60;
            int secs = seconds % 60;

            String formatted = String.format("%2d:%2d", minutes, secs);

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

    private void startTrackingRoute() {
        locationRunnable = new Runnable() {
            @Override
            public void run() {
                if (ActivityCompat.checkSelfPermission(
                        MapsActivity.this,
                        Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED) return;

                FusedLocationProviderClient fusedLocationProviderClient =
                        LocationServices.getFusedLocationProviderClient(MapsActivity.this);

                fusedLocationProviderClient.getLastLocation().addOnSuccessListener(location -> {
                    if (location != null) {
                        LatLng point = new LatLng(location.getLatitude(), location.getLongitude());

                        //Save point
                        routePoints.add(point);

                        //Add a marker
                        if (mMap != null) {
                            if (lastMarker != null)
                                lastMarker.remove();
                            lastMarker = mMap.addMarker(new MarkerOptions().position(point));
                        }

                        drawRoutePolyline();
                    }
                });

                handler.postDelayed(this, 5000);
            }
        };

        handler.post(locationRunnable);
    }

    private void stopTrackingRoute() {
        handler.removeCallbacks(locationRunnable);

        if (routePolyline != null){
            routePolyline.remove();
        }

        routePoints.clear();
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
}
