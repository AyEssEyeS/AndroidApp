package cod.ayesseye.textron0;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.PopupMenu;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.toolbox.JsonObjectRequest;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import cod.ayesseye.textron0.databinding.ActivityMapsBinding;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.json.JSONException;




public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    //Current Working mode
    private enum AppMode {
        NORMAL,
        LEADER,
        FOLLOWER
    }
    //Default Mode  when the app starts
    private AppMode currentMode = AppMode.NORMAL;

    //Udp port shared
    private static final int UDP_PORT = 5000;
    //Leader
    private DatagramSocket leaderSocket;
    private InetAddress broadcastAdress;
    //Follower
    private DatagramSocket followerSocket;
    private Thread followerThread;
    //Wi-fi Lock
    private WifiManager.MulticastLock multicastLock;
    //Map marker for the leader
    private com.google.android.gms.maps.model.Marker leaderMarker;


    //Map object
    private GoogleMap mMap;

    //Timer Variables
    private ScheduledExecutorService scheduler;
    private int seconds = 0;
    private boolean isPaused = false;
    private boolean isRunning = false;
    TextView timerText, txtCurrentTime, txtWeather;

    Button home, weather;
    FloatingActionButton startWalk, pauseWalk, stopWalk;


    private long lastUpdateTime = 0;

    //Route tracking variables
    private ArrayList<LatLng> routePoints = new ArrayList<>();
    private Polyline routePolyline;

    //Location objects
    private FusedLocationProviderClient fusedLocationProviderClient;
    private com.google.android.gms.location.LocationCallback locationCallback;
    FloatingActionButton modeSelectButton;

    LinearLayout bottomButtonContainer, statsContainer;
    TextView txtKm, txtPace, txtCurrentPace;
    private static final String API_KEY = "3496e86f154d566be3a095951868974f";



    //Distance + Pace variables
    private float totalDistanceMeters = 0f;
    private long startTimeMillis;




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ActivityMapsBinding binding = ActivityMapsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        assert mapFragment != null;
        mapFragment.getMapAsync(this);

        //Referemces to xml
        home = findViewById(R.id.btn_home);
        weather = findViewById(R.id.btn_weather);
        startWalk = findViewById(R.id.btn_start);
        pauseWalk = findViewById(R.id.btn_pause);
        stopWalk = findViewById(R.id.btn_stop);
        timerText = findViewById(R.id.timerText);

        bottomButtonContainer = findViewById(R.id.bottomButtonContainer);
        statsContainer = findViewById(R.id.statsContainer);

        txtKm = findViewById(R.id.txt_km);
        txtPace = findViewById(R.id.txt_pace);
        txtCurrentPace = findViewById(R.id.txt_currentPace);

        modeSelectButton = findViewById(R.id.btn_mode_select);

        txtCurrentTime = findViewById(R.id.txtCurrentTime);
        txtWeather = findViewById(R.id.txtWeather);
        startClock();
        getWeather();


        pauseWalk.hide();
        stopWalk.hide();


        home.setOnClickListener(v -> changePage(MainActivity.class));
        weather.setOnClickListener(v -> changePage(OpenWeather.class));

        startWalk.setOnClickListener(v -> {
            startTimer();
            startTrackingRoute();

            startWalk.hide();
            pauseWalk.show();
            stopWalk.show();

            bottomButtonContainer.setVisibility(View.GONE);
            statsContainer.setVisibility(View.VISIBLE);
        });


        pauseWalk.setOnClickListener(v -> togglePause());

        modeSelectButton.setOnClickListener(v -> showModePopupMenu(v));
        stopWalk.setOnClickListener(v -> {
            stopTimer();
            stopTrackingRoute();

            pauseWalk.hide();
            stopWalk.hide();
            startWalk.show();

            statsContainer.setVisibility(View.GONE);
            bottomButtonContainer.setVisibility(View.VISIBLE);

            pauseWalk.setImageResource(R.drawable.ic_pause);
        });


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

            String formatted = String.format("%02d:%02d", minutes, secs);

            runOnUiThread(() -> timerText.setText(formatted));
        }, 1, 1, TimeUnit.SECONDS);
    }

    private void togglePause() {
        isPaused = !isPaused;

        if (isPaused) {
            pauseWalk.setImageResource(R.drawable.ic_play_arrow); // Switch to resume
        } else {
            pauseWalk.setImageResource(R.drawable.ic_pause); // Switch to pause
        }


    }


    private void stopTimer() {
        if (scheduler != null) {
            scheduler.shutdownNow();
            scheduler = null;
        }
        timerText.setText("00:00");
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
           public void onLocationResult(LocationResult result) {
               if (isPaused) return;  // Ignore updates when paused

               Location location = result.getLastLocation();
               if (location == null) return;
               long now = System.currentTimeMillis();
               float elapsedSec = 0;

               if (lastUpdateTime != 0) {
                   elapsedSec = (now - lastUpdateTime) / 1000f;
               }

               lastUpdateTime = now;


               LatLng point = new LatLng(location.getLatitude(), location.getLongitude());
               float segmentMeters = 0f;

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

               routePoints.add(point);
               drawRoutePolyline();

               // Update stats
               float km = totalDistanceMeters / 1000f;
               String currentPace = getCurrentPace(segmentMeters, elapsedSec);
               String avgPace = getAveragePace();

               runOnUiThread(() -> {
                   txtKm.setText(String.format("KM: %.2f", km));
                   txtCurrentPace.setText("Current Pace: " + currentPace);
                   txtPace.setText("Avg Pace: " + avgPace);
               });
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

    private String getCurrentPace(float segmentMeters, float seconds) {

        // GPS noise handling
        if (segmentMeters < 5 || seconds <= 0) {
            return "0:00";
        }


        float km = segmentMeters / 1000f;
        float paceSecPerKm = seconds / km;

        int minutes = (int) (paceSecPerKm / 60);
        int sec = (int) (paceSecPerKm % 60);

        return String.format(Locale.getDefault(), "%d:%02d", minutes, sec);
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


    private void startClock() {
        final android.os.Handler handler = new android.os.Handler();

        handler.post(new Runnable() {
            @Override
            public void run() {
                String time = new java.text.SimpleDateFormat("HH:mm", Locale.getDefault()).format(new java.util.Date());

                txtCurrentTime.setText("Time: " + time);
                handler.postDelayed(this,1000);
            }
        });
    }

    @SuppressLint("MissingPermission")
    private void getWeather() {

        FusedLocationProviderClient client =
                LocationServices.getFusedLocationProviderClient(this);

        client.getLastLocation().addOnSuccessListener(location -> {
            if (location == null) {
                txtWeather.setText("Weather: Location unavailable");
                return;
            }

            double lat = location.getLatitude();
            double lng = location.getLongitude();

            String url = "https://api.openweathermap.org/data/2.5/weather?" +
                    "lat=" + lat +
                    "&lon=" + lng +
                    "&units=metric" +
                    "&appid=" + API_KEY;

            JsonObjectRequest request = new JsonObjectRequest(
                    Request.Method.GET,
                    url,
                    null,
                    response -> {
                        try {
                            String city = response.getString("name");
                            double temp = response.getJSONObject("main").getDouble("temp");
                            String desc = response.getJSONArray("weather")
                                    .getJSONObject(0)
                                    .getString("description");

                            txtWeather.setText(
                                    "Weather: " + city + "\n" +
                                            temp + "°C, " + desc
                            );

                        } catch (JSONException e) {
                            txtWeather.setText("Weather: JSON error");
                        }
                    },
                    error -> txtWeather.setText("Weather: fetch error")
            );

            AppController.getInstance().addToRequestQueue(request);
        });
    }

    private void showModePopupMenu(View view) {
        PopupMenu popup = new PopupMenu(this, view);
        popup.getMenuInflater().inflate(R.menu.mode_menu, popup.getMenu());

        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {

                int itemId = item.getItemId();
                AppMode newMode;

                if (itemId == R.id.mode_normal) {
                    newMode = AppMode.NORMAL;
                } else if (itemId == R.id.mode_leader) {
                    newMode = AppMode.LEADER;
                } else if (itemId == R.id.mode_follower) {
                    newMode = AppMode.FOLLOWER;
                } else {
                    return false;
                }

                // Aplică noul mod și logica specifică
                setAppMode(newMode);
                return true;
            }
        });
        popup.show();
    }

    private void setAppMode(AppMode newMode) {
        currentMode = newMode;
        Log.d("AppMode", "Modul a fost schimbat la: " + newMode.toString());

        // Logica vizuală (opțional)
        int color;
        String modeName;

        switch (newMode) {
            case NORMAL:
                color = Color.BLACK; // Mod implicit
                modeName = "Normal";
                // Oprirea oricărei funcționalități Leader/Follower
                break;
            case LEADER:
                color = Color.RED; // Evidențiere roșie pentru Leader
                modeName = "Leader";
                // Implementează logica de broadcasting (trimitere coordonate)
                break;
            case FOLLOWER:
                color = Color.GREEN; // Evidențiere verde pentru Follower
                modeName = "Follower";
                // Implementează logica de ascultare/primire coordonate
                break;
            default:
                color = Color.BLACK;
                modeName = "Necunoscut";
        }

    }
}
