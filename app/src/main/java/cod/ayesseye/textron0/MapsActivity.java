package cod.ayesseye.textron0;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.gson.Gson;

import org.json.JSONException;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import cod.ayesseye.textron0.databinding.ActivityMapsBinding;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    // --- ENUMS & CONSTANTS ---
    private enum AppMode { NORMAL, LEADER, FOLLOWER }
    private AppMode currentMode = AppMode.NORMAL;

    private static final int UDP_PORT = 50505; // Port for Network Game
    private static final String API_KEY = "3496e86f154d566be3a095951868974f"; // OpenWeatherMap Key
    private static final float EAT_DISTANCE_METERS = 15.0f; // Distance to "eat" the line in Follower mode

    // --- UI ELEMENTS ---
    private GoogleMap mMap;
    private TextView timerText, txtCurrentTime, txtWeather;
    private TextView txtKm, txtPace, txtCurrentPace;
    private TextView txtSession; // To show Session ID in Leader Mode
    private Button homeBtn, weatherBtn;
    private FloatingActionButton startBtn, pauseBtn, stopBtn, modeBtn;
    private LinearLayout bottomButtonContainer, statsContainer;

    // --- TIMERS ---
    private ScheduledExecutorService scheduler; // For the stopwatch
    private int seconds = 0;
    private boolean isPaused = false;
    private final Handler clockHandler = new Handler(Looper.getMainLooper()); // For real-time clock

    // --- GPS & ROUTE ---
    private FusedLocationProviderClient locationClient;
    private LocationCallback locationCallback;
    private boolean isFirstZoom = true; // To zoom only on app start
    private boolean weatherFetched = false; // To fetch weather only once

    // Normal Mode Path (Blue)
    private ArrayList<LatLng> routePoints = new ArrayList<>();
    private Polyline routePolyline;
    private float totalDistanceMeters = 0f;
    private long startTimeMillis;
    private long lastUpdateTime = 0;

    // Leader/Follower Path (Red)
    private ArrayList<LatLng> leaderPoints = new ArrayList<>();
    private Polyline leaderPolyline;
    private Marker leaderMarker;

    // --- NETWORKING (UDP) ---
    private DatagramSocket leaderSocket;
    private DatagramSocket followerSocket;
    private Thread followerThread;
    private String followerIp;
    private String sessionId;
    private final Gson gson = new Gson();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // View Binding Setup
        ActivityMapsBinding binding = ActivityMapsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Initialize Services
        locationClient = LocationServices.getFusedLocationProviderClient(this);

        // Initialize Map
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) mapFragment.getMapAsync(this);

        // --- BIND VIEWS ---
        initializeViews();

        // --- SETUP LISTENERS ---
        setupClickListeners();

        // --- START BACKGROUND TASKS ---
        startRealTimeClock();

        // Start GPS immediately (for Weather & Zoom, but not recording yet)
        startTrackingService();
    }

    /**
     * Finds all views by ID.
     */
    private void initializeViews() {
        homeBtn = findViewById(R.id.btn_home);
        weatherBtn = findViewById(R.id.btn_weather);
        startBtn = findViewById(R.id.btn_start);
        pauseBtn = findViewById(R.id.btn_pause);
        stopBtn = findViewById(R.id.btn_stop);
        modeBtn = findViewById(R.id.btn_mode_select);

        timerText = findViewById(R.id.timerText);
        txtSession = findViewById(R.id.txtSessionId); // Make sure this exists in XML or add it

        txtCurrentTime = findViewById(R.id.txtCurrentTime);
        txtWeather = findViewById(R.id.txtWeather);

        bottomButtonContainer = findViewById(R.id.bottomButtonContainer);
        statsContainer = findViewById(R.id.statsContainer);

        txtKm = findViewById(R.id.txt_km);
        txtPace = findViewById(R.id.txt_pace);
        txtCurrentPace = findViewById(R.id.txt_currentPace);

        // Initial Visibility
        pauseBtn.hide();
        stopBtn.hide();
        if(txtSession != null) txtSession.setVisibility(View.GONE);
    }

    /**
     * Sets up button interactions.
     */
    private void setupClickListeners() {
        homeBtn.setOnClickListener(v -> {
            stopTrackingService();
            finish();
        });

        weatherBtn.setOnClickListener(v -> {
            startActivity(new Intent(this, OpenWeather.class));
        });

        startBtn.setOnClickListener(v -> {
            // 1. Common UI changes (Hide Start, Show Stop)
            startBtn.hide();
            stopBtn.show();
            isPaused = false;

            // 2. Mode-Specific Logic
            if (currentMode == AppMode.NORMAL) {
                // NORMAL MODE: Show Pause Button & Stats
                pauseBtn.show();

                startTimer();
                resetNormalRouteData();
                bottomButtonContainer.setVisibility(View.GONE);
                statsContainer.setVisibility(View.VISIBLE);

            } else {
                // LEADER / FOLLOWER MODE: Keep Pause Button HIDDEN
                pauseBtn.hide();

                if (currentMode == AppMode.LEADER) {
                    Toast.makeText(this, "Broadcasting Location...", Toast.LENGTH_SHORT).show();
                }
            }
        });

        pauseBtn.setOnClickListener(v -> togglePause());

        stopBtn.setOnClickListener(v -> {
            if (currentMode == AppMode.NORMAL) {
                stopTimer();
                saveWalkToFirebase();
                resetNormalRouteData();

                bottomButtonContainer.setVisibility(View.VISIBLE);
                statsContainer.setVisibility(View.GONE);
            } else {
                stopNetworkModes();
            }

            // UI Reset (Always hide pause when stopped)
            pauseBtn.hide();
            stopBtn.hide();
            startBtn.show();
            pauseBtn.setImageResource(R.drawable.ic_pause);
        });

        modeBtn.setOnClickListener(this::showModePopupMenu);
    }

    // --- MAP & GPS LOGIC ---

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        mMap.getUiSettings().setZoomControlsEnabled(false);

        // Enable Blue Dot if permission granted
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
        }
    }

    /**
     * Starts the GPS engine. This runs continuously.
     * The logic inside handles what to do based on 'currentMode'.
     */
    @SuppressLint("MissingPermission")
    private void startTrackingService() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            return;
        }

        LocationRequest request = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 2000)
                .setMinUpdateIntervalMillis(1000)
                .setMinUpdateDistanceMeters(0)
                .build();

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult result) {
                Location location = result.getLastLocation();
                if (location == null) return;

                // 1. Initial Zoom (Once)
                if (isFirstZoom && mMap != null) {
                    LatLng currentLoc = new LatLng(location.getLatitude(), location.getLongitude());
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLoc, 16));
                    isFirstZoom = false;
                }

                // 2. Fetch Weather (Once)
                if (!weatherFetched) {
                    fetchWeather(location.getLatitude(), location.getLongitude());
                }

                if (isPaused) return; // Stop processing if paused

                LatLng point = new LatLng(location.getLatitude(), location.getLongitude());

                // 3. MODE SPECIFIC LOGIC
                switch (currentMode) {
                    case NORMAL:
                        // Only record if timer is running (Start button pressed)
                        if (stopBtn.isShown()) {
                            processNormalWalk(location, point);
                        }
                        pauseBtn.setVisibility(View.VISIBLE);
                        break;

                    case LEADER:
                        pauseBtn.setVisibility(View.GONE);
                        // Send location to Follower
                        sendLeaderPacket(point);
                        break;

                    case FOLLOWER:
                        pauseBtn.setVisibility(View.GONE);
                        // Check if we are "eating" the leader's line
                        checkIfConsumingLine(location);

                        break;
                }
            }
        };

        locationClient.requestLocationUpdates(request, locationCallback, Looper.getMainLooper());
    }

    /**
     * Logic for Normal Mode: Draw Blue Line, Calculate Stats.
     */
    private void processNormalWalk(Location location, LatLng point) {
        long now = System.currentTimeMillis();
        float elapsedSec = 0;
        if (lastUpdateTime != 0) {
            elapsedSec = (now - lastUpdateTime) / 1000f;
        }
        lastUpdateTime = now;

        // Calculate Distance
        float segmentMeters = 0f;
        if (!routePoints.isEmpty()) {
            LatLng lastPoint = routePoints.get(routePoints.size() - 1);
            float[] results = new float[1];
            Location.distanceBetween(lastPoint.latitude, lastPoint.longitude, point.latitude, point.longitude, results);
            segmentMeters = results[0];
            totalDistanceMeters += segmentMeters;
        }

        // Add to path and draw
        routePoints.add(point);
        drawNormalPolyline();

        // Update Text Stats
        float km = totalDistanceMeters / 1000f;
        String currentPace = calculateCurrentPace(segmentMeters, elapsedSec);
        String avgPace = calculateAveragePace();

        timerText.post(() -> {
            txtKm.setText(String.format(Locale.getDefault(), "KM: %.2f", km));
            txtCurrentPace.setText("Current Pace: " + currentPace);
            txtPace.setText("Avg Pace: " + avgPace);
        });
    }

    // --- DRAWING HELPERS ---

    private void drawNormalPolyline() {
        if (mMap == null || routePoints.size() < 2) return;
        if (routePolyline != null) routePolyline.remove();

        routePolyline = mMap.addPolyline(new PolylineOptions()
                .addAll(routePoints)
                .width(12)
                .color(Color.BLUE) // Normal = Blue
                .geodesic(true));
    }

    private void drawLeaderPolyline() {
        if (mMap == null) return;
        if (leaderPolyline != null) leaderPolyline.remove();

        leaderPolyline = mMap.addPolyline(new PolylineOptions()
                .addAll(leaderPoints)
                .width(15)
                .color(Color.RED) // Leader/Game = Red
                .geodesic(true));
    }

    // --- UDP NETWORKING LOGIC ---

    /**
     * LEADER: Sends current position to the Follower's IP.
     */
    private void sendLeaderPacket(LatLng pos) {
        new Thread(() -> {
            try {
                if (leaderSocket == null) leaderSocket = new DatagramSocket();
                if (followerIp == null || followerIp.isEmpty()) return;

                // SyncLocationPacket inner class used for JSON
                SyncLocationPacket packet = new SyncLocationPacket(
                        sessionId, pos.latitude, pos.longitude, System.currentTimeMillis()
                );

                byte[] data = gson.toJson(packet).getBytes();
                InetAddress addr = InetAddress.getByName(followerIp);
                leaderSocket.send(new DatagramPacket(data, data.length, addr, UDP_PORT));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    /**
     * FOLLOWER: Starts a thread to listen for incoming packets.
     */
    private void startFollowerNetwork() {
        followerThread = new Thread(() -> {
            try {
                if (followerSocket == null) followerSocket = new DatagramSocket(UDP_PORT);
                byte[] buffer = new byte[4096];

                while (!Thread.currentThread().isInterrupted()) {
                    DatagramPacket dp = new DatagramPacket(buffer, buffer.length);
                    followerSocket.receive(dp); // Blocks until data arrives

                    String json = new String(dp.getData(), 0, dp.getLength());
                    SyncLocationPacket packet = gson.fromJson(json, SyncLocationPacket.class);

                    if (packet != null) {
                        runOnUiThread(() -> processLeaderPacket(packet));
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        followerThread.start();
    }

    /**
     * FOLLOWER: Updates the Red Line when a packet arrives.
     */
    private void processLeaderPacket(SyncLocationPacket packet) {
        LatLng pos = new LatLng(packet.lat, packet.lng);

        // Update Marker
        if (mMap != null) {
            if (leaderMarker == null) {
                leaderMarker = mMap.addMarker(new MarkerOptions()
                        .position(pos)
                        .title("LEADER")
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));
            } else {
                leaderMarker.setPosition(pos);
            }
        }

        // Add to Red Path
        leaderPoints.add(pos);
        drawLeaderPolyline();
    }

    /**
     * FOLLOWER: Checks distance to every point in the Red Line.
     * If close (< 15m), removes the point (Eat it).
     */
    private void checkIfConsumingLine(Location myLoc) {
        if (leaderPoints.isEmpty()) return;

        boolean changed = false;
        Iterator<LatLng> iterator = leaderPoints.iterator();

        while (iterator.hasNext()) {
            LatLng p = iterator.next();
            float[] res = new float[1];
            Location.distanceBetween(myLoc.getLatitude(), myLoc.getLongitude(), p.latitude, p.longitude, res);

            if (res[0] < EAT_DISTANCE_METERS) {
                iterator.remove(); // Eat the dot
                changed = true;
            }
        }

        if (changed) drawLeaderPolyline(); // Redraw if consumed
    }

    // --- MODE SWITCHING ---

    private void showModePopupMenu(View view) {
        PopupMenu popup = new PopupMenu(this, view);
        popup.getMenu().add("NORMAL");
        popup.getMenu().add("LEADER");
        popup.getMenu().add("FOLLOWER");

        popup.setOnMenuItemClickListener(item -> {
            stopNetworkModes(); // Cleanup previous mode
            resetNormalRouteData(); // Cleanup normal data
            stopTimer();

            // Reset UI
            startBtn.show();
            stopBtn.hide();
            pauseBtn.hide();
            if(txtSession != null) txtSession.setVisibility(View.GONE);
            bottomButtonContainer.setVisibility(View.VISIBLE);
            statsContainer.setVisibility(View.GONE);

            String title = item.getTitle().toString();
            if (title.equals("NORMAL")) currentMode = AppMode.NORMAL;
            if (title.equals("LEADER")) setupLeader();
            if (title.equals("FOLLOWER")) setupFollower();

            Toast.makeText(this, "Mode: " + title, Toast.LENGTH_SHORT).show();
            return true;
        });
        popup.show();
    }

    private void setupLeader() {
        currentMode = AppMode.LEADER;
        sessionId = UUID.randomUUID().toString().substring(0, 6).toUpperCase(Locale.ROOT);
        if(txtSession != null) {
            txtSession.setText("Session: " + sessionId);
            txtSession.setVisibility(View.VISIBLE);
        }
        askForFollowerIp();
    }

    private void setupFollower() {
        currentMode = AppMode.FOLLOWER;
        startFollowerNetwork();
    }

    private void askForFollowerIp() {
        AlertDialog.Builder b = new AlertDialog.Builder(this);
        b.setTitle("Follower IP Address");
        EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        b.setView(input);
        b.setPositiveButton("OK", (d, w) -> followerIp = input.getText().toString().trim());
        b.show();
    }

    private void stopNetworkModes() {
        new Thread(() -> {
            if (leaderSocket != null) leaderSocket.close();
            if (followerSocket != null) followerSocket.close();
            leaderSocket = null;
            followerSocket = null;
        }).start();
        if (followerThread != null) followerThread.interrupt();
        leaderPoints.clear();
        if (leaderPolyline != null) leaderPolyline.remove();
        if (leaderMarker != null) leaderMarker.remove();
    }

    // --- TIMERS & STATS ---

    private void startTimer() {
        if (scheduler != null && !scheduler.isShutdown()) return;
        seconds = 0;
        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleWithFixedDelay(() -> {
            if (!isPaused) seconds++;
            int m = seconds / 60;
            int s = seconds % 60;
            runOnUiThread(() -> timerText.setText(String.format(Locale.getDefault(), "%02d:%02d", m, s)));
        }, 1, 1, TimeUnit.SECONDS);
    }

    private void stopTimer() {
        if (scheduler != null) {
            scheduler.shutdownNow();
            scheduler = null;
        }
        timerText.setText("00:00");
    }

    private void startRealTimeClock() {
        clockHandler.post(new Runnable() {
            @Override
            public void run() {
                String time = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date());
                txtCurrentTime.setText("Time: " + time);
                clockHandler.postDelayed(this, 1000);
            }
        });
    }

    private String calculateCurrentPace(float segmentMeters, float seconds) {
        if (segmentMeters < 5 || seconds <= 0) return "0:00";
        float km = segmentMeters / 1000f;
        float pace = seconds / km;
        return String.format(Locale.getDefault(), "%d:%02d", (int)(pace/60), (int)(pace%60));
    }

    private String calculateAveragePace() {
        float km = totalDistanceMeters / 1000f;
        if (km <= 0) return "0:00";
        long elapsed = System.currentTimeMillis() - startTimeMillis;
        float pace = (elapsed / 1000f) / km;
        return String.format(Locale.getDefault(), "%d:%02d", (int)(pace/60), (int)(pace%60));
    }

    // --- CLEANUP & SAVING ---

    private void resetNormalRouteData() {
        routePoints.clear();
        if (routePolyline != null) routePolyline.remove();
        totalDistanceMeters = 0f;
        startTimeMillis = System.currentTimeMillis();
        lastUpdateTime = 0;
    }

    private void saveWalkToFirebase() {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) return;

        // Prepare Data using raw HashMap (Compatible with your code)
        HashMap<String, Object> walk = new HashMap<>();
        walk.put("distanceKm", totalDistanceMeters / 1000f);
        walk.put("duration", timerText.getText().toString());
        walk.put("timestamp", System.currentTimeMillis());
        walk.put("avgPace", txtPace.getText().toString());

        // Route Points
        ArrayList<HashMap<String, Double>> routeList = new ArrayList<>();
        for (LatLng p : routePoints) {
            HashMap<String, Double> map = new HashMap<>();
            map.put("lat", p.latitude);
            map.put("lng", p.longitude);
            routeList.add(map);
        }
        walk.put("route", routeList);

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("users")
                .child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                .child("walks");

        ref.push().setValue(walk)
                .addOnSuccessListener(a -> Log.d("FIREBASE", "Saved!"))
                .addOnFailureListener(e -> Log.e("FIREBASE", "Error: " + e.getMessage()));
    }

    private void stopTrackingService() {
        if (locationCallback != null) {
            locationClient.removeLocationUpdates(locationCallback);
        }
        stopTimer();
        stopNetworkModes();
    }

    private void togglePause() {
        isPaused = !isPaused;
        pauseBtn.setImageResource(isPaused ? R.drawable.ic_play_arrow : R.drawable.ic_pause);
    }

    // --- WEATHER ---

    private void fetchWeather(double lat, double lng) {
        String url = "https://api.openweathermap.org/data/2.5/weather?lat=" + lat + "&lon=" + lng + "&units=metric&appid=" + API_KEY;

        // Use Volley.newRequestQueue to avoid needing AppController
        Volley.newRequestQueue(this).add(new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        String city = response.getString("name");
                        double temp = response.getJSONObject("main").getDouble("temp");
                        String desc = response.getJSONArray("weather").getJSONObject(0).getString("description");
                        txtWeather.setText("Weather: " + city + "\n" + temp + "°C, " + desc);
                        weatherFetched = true;
                    } catch (JSONException e) { e.printStackTrace(); }
                },
                error -> txtWeather.setText("Weather: Error")
        ));
    }

    // --- HELPER CLASSES ---

    // Packet structure for UDP
    public static class SyncLocationPacket {
        String sessionId;
        double lat;
        double lng;
        long timestamp;

        public SyncLocationPacket(String sessionId, double lat, double lng, long timestamp) {
            this.sessionId = sessionId;
            this.lat = lat;
            this.lng = lng;
            this.timestamp = timestamp;
        }
    }
}