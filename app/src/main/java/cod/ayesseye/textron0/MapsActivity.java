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
import android.os.StrictMode;
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
import java.net.InetSocketAddress;
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

// Activitatea principala care gestioneaza Harta, GPS-ul si logica Multiplayer (UDP)
public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    // Definirea starilor aplicatiei: Normal , Leader sau Follower
    private enum AppMode { NORMAL, LEADER, FOLLOWER }
    private AppMode currentMode = AppMode.NORMAL;

    private static final int UDP_PORT = 50505;
    private static final String API_KEY = "3496e86f154d566be3a095951868974f"; // Cheie API Weather
    private static final float EAT_DISTANCE_METERS = 15.0f; // Distanta de interactiune in Maps

    // --- UI ELEMENTS ---
    private GoogleMap mMap;
    private TextView timerText, txtCurrentTime, txtWeather;
    private TextView txtKm, txtPace, txtCurrentPace;
    private Button homeBtn, weatherBtn;
    private FloatingActionButton startBtn, pauseBtn, stopBtn, modeBtn;
    private LinearLayout bottomButtonContainer, statsContainer;
    private FloatingActionButton lockBtn;
    private boolean isCameraLocked = false;

    // --- TIMERS ---
    // Executor pentru cronometrul antrenamentului
    private ScheduledExecutorService scheduler;
    private int seconds = 0;
    private boolean isPaused = false;
    private final Handler clockHandler = new Handler(Looper.getMainLooper());

    // --- GPS & ROUTE ---
    private FusedLocationProviderClient locationClient;
    private LocationCallback locationCallback;
    private boolean isFirstZoom = true;
    private boolean weatherFetched = false;

    // Normal Mode Path - Stocarea traseului local
    private ArrayList<LatLng> routePoints = new ArrayList<>();
    private Polyline routePolyline;
    private float totalDistanceMeters = 0f;
    private long startTimeMillis;
    private long lastUpdateTime = 0;

    // Game Elements - Elementele pentru modul Multiplayer
    private ArrayList<LatLng> leaderPoints = new ArrayList<>();
    private Polyline leaderPolyline;
    private Marker leaderMarker;
    private Marker followerMarker;

    // --- NETWORKING (UDP) ---
    // Socket si Thread-uri pentru comunicarea Peer-to-Peer
    private DatagramSocket udpSocket;
    private Thread receiverThread;
    private String targetIp; // IP-ul PARTENERULUI
    private String sessionId;
    private final Gson gson = new Gson();
    private boolean isRunningNetwork = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Permitem retea pe main thread pentru initializare rapida (Strict Mode Policy)
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        ActivityMapsBinding binding = ActivityMapsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Initializare client locatie (Google Services)
        locationClient = LocationServices.getFusedLocationProviderClient(this);

        // Incarcare asincrona a hartii
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) mapFragment.getMapAsync(this);

        initializeViews();
        setupClickListeners();
        startRealTimeClock();
        startTrackingService(); // Porneste urmarirea GPS
    }

    // Leaga variabilele din cod de elementele vizuale din XML
    private void initializeViews() {
        homeBtn = findViewById(R.id.btn_home);
        weatherBtn = findViewById(R.id.btn_weather);
        startBtn = findViewById(R.id.btn_start);
        pauseBtn = findViewById(R.id.btn_pause);
        stopBtn = findViewById(R.id.btn_stop);
        modeBtn = findViewById(R.id.btn_mode_select);
        timerText = findViewById(R.id.timerText);
        txtCurrentTime = findViewById(R.id.txtCurrentTime);
        txtWeather = findViewById(R.id.txtWeather);
        bottomButtonContainer = findViewById(R.id.bottomButtonContainer);
        statsContainer = findViewById(R.id.statsContainer);
        txtKm = findViewById(R.id.txt_km);
        txtPace = findViewById(R.id.txt_pace);
        txtCurrentPace = findViewById(R.id.txt_currentPace);
        lockBtn = findViewById(R.id.btn_lock_camera);

        lockBtn.hide();
        pauseBtn.hide();
        stopBtn.hide();
    }

    // Defineste actiunile butoanelor (Start, Stop, Lock Camera, Schimbare Mod)
    private void setupClickListeners() {
        homeBtn.setOnClickListener(v -> {
            stopTrackingService();
            finish();
        });
        weatherBtn.setOnClickListener(v -> startActivity(new Intent(this, OpenWeather.class)));

        startBtn.setOnClickListener(v -> {
            startBtn.hide();
            stopBtn.show();
            isPaused = false;

            if (currentMode == AppMode.NORMAL) {
                pauseBtn.show();
                startTimer();
                resetNormalRouteData();
                bottomButtonContainer.setVisibility(View.GONE);
                statsContainer.setVisibility(View.VISIBLE);
            } else {
                pauseBtn.hide();
                Toast.makeText(this, "Conexiune Activa...", Toast.LENGTH_SHORT).show();
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
            pauseBtn.hide();
            stopBtn.hide();
            startBtn.show();
            pauseBtn.setImageResource(R.drawable.ic_pause);
        });

        modeBtn.setOnClickListener(this::showModePopupMenu);

        // Logica pentru blocarea camerei pe partener (Follower Mode)
        lockBtn.setOnClickListener(v -> {
            isCameraLocked = !isCameraLocked; // Inversam starea

            if (isCameraLocked) {
                // Stare: BLOCAT (LIPIT)
                Toast.makeText(this, "Camera lipita de Leader!", Toast.LENGTH_SHORT).show();
                lockBtn.setImageTintList(android.content.res.ColorStateList.valueOf(Color.RED)); // Facem iconita rosie
            } else {
                // Stare: LIBER
                Toast.makeText(this, "Camera libera", Toast.LENGTH_SHORT).show();
                lockBtn.setImageTintList(android.content.res.ColorStateList.valueOf(Color.BLACK)); // Revenim la normal
            }
        });
    }

    // --- MAP & GPS LOGIC ---

    // Callback apelat cand harta este gata de utilizare
    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        mMap.getUiSettings().setZoomControlsEnabled(false);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
        }
    }

    // Configureaza si porneste ascultatorul de locatie GPS (High Accuracy)
    @SuppressLint("MissingPermission")
    private void startTrackingService() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
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

                if (isFirstZoom && mMap != null) {
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(location.getLatitude(), location.getLongitude()), 16));
                    isFirstZoom = false;
                }
                if (!weatherFetched) fetchWeather(location.getLatitude(), location.getLongitude());
                if (isPaused) return;

                LatLng point = new LatLng(location.getLatitude(), location.getLongitude());

                // Logica principala in functie de modul activ
                switch (currentMode) {
                    case NORMAL:
                        if (stopBtn.isShown()) processNormalWalk(location, point);
                        break;

                    case LEADER:
                        // Leader trimite pachetul marcat ca LEADER
                        sendUdpPacket(point, "LEADER");
                        break;

                    case FOLLOWER:
                        // Follower trimite pachetul marcat ca FOLLOWER
                        checkIfConsumingLine(location);
                        sendUdpPacket(point, "FOLLOWER");
                        break;
                }
            }
        };
        locationClient.requestLocationUpdates(request, locationCallback, Looper.getMainLooper());
    }

    // Calculeaza distanta, viteza si deseneaza traseul pentru modul Normal
    private void processNormalWalk(Location location, LatLng point) {
        // ... (Codul pentru Normal Mode ramane neschimbat)
        long now = System.currentTimeMillis();
        float elapsedSec = 0;
        if (lastUpdateTime != 0) elapsedSec = (now - lastUpdateTime) / 1000f;
        lastUpdateTime = now;

        float segmentMeters = 0f;
        if (!routePoints.isEmpty()) {
            LatLng lastPoint = routePoints.get(routePoints.size() - 1);
            float[] results = new float[1];
            Location.distanceBetween(lastPoint.latitude, lastPoint.longitude, point.latitude, point.longitude, results);
            segmentMeters = results[0];
            totalDistanceMeters += segmentMeters;
        }
        routePoints.add(point);
        drawNormalPolyline();

        float km = totalDistanceMeters / 1000f;
        String currentPace = calculateCurrentPace(segmentMeters, elapsedSec);
        String avgPace = calculateAveragePace();
        timerText.post(() -> {
            txtKm.setText(String.format(Locale.getDefault(), "KM: %.2f", km));
            txtCurrentPace.setText("Current Pace: " + currentPace);
            txtPace.setText("Avg Pace: " + avgPace);
        });
    }

    // Deseneaza linia albastra pe harta
    private void drawNormalPolyline() {
        if (mMap == null || routePoints.size() < 2) return;
        if (routePolyline != null) routePolyline.remove();
        routePolyline = mMap.addPolyline(new PolylineOptions().addAll(routePoints).width(12).color(Color.BLUE).geodesic(true));
    }

    // Deseneaza linia rosie a Leader-ului
    private void drawLeaderPolyline() {
        if (mMap == null) return;
        if (leaderPolyline != null) leaderPolyline.remove();
        leaderPolyline = mMap.addPolyline(new PolylineOptions().addAll(leaderPoints).width(15).color(Color.RED).geodesic(true));
    }

    // --- UDP NETWORKING LOGIC (REPARAT) ---

    // Functie noua pentru a initializa socket-ul garantat
    private void initSocket() {
        try {
            if (udpSocket == null || udpSocket.isClosed()) {
                udpSocket = new DatagramSocket(null);
                udpSocket.setReuseAddress(true);
                udpSocket.bind(new InetSocketAddress(UDP_PORT));
                Log.d("UDP_INIT", "Socket creat cu succes pe portul " + UDP_PORT);
            }
        } catch (Exception e) {
            Log.e("UDP_INIT", "Eroare creare socket: " + e.getMessage());
        }
    }

    // Trimite locatia curenta catre partener prin protocolul UDP
    private void sendUdpPacket(LatLng pos, String type) {
        if (targetIp == null || targetIp.isEmpty() || !isRunningNetwork) return;

        // VERIFICARE critica: Daca socket-ul e null, incercam sa-l cream pe loc
        if (udpSocket == null) {
            initSocket();
        }

        new Thread(() -> {
            try {
                if (udpSocket == null) return;

                SyncLocationPacket packet = new SyncLocationPacket(
                        sessionId, type, pos.latitude, pos.longitude, System.currentTimeMillis()
                );

                byte[] data = gson.toJson(packet).getBytes();
                InetAddress addr = InetAddress.getByName(targetIp);

                DatagramPacket dp = new DatagramPacket(data, data.length, addr, UDP_PORT);
                udpSocket.send(dp);

                // --- LOG PENTRU DEPANARE ---
                // Verifica acest tag in Logcat pe telefonul FOLLOWER
                Log.d("UDP_SEND", "Trimis " + type + " catre " + targetIp + " | Lat: " + pos.latitude);

            } catch (Exception e) {
                Log.e("UDP_SEND_ERR", "Eroare la trimitere: " + e.getMessage());
            }
        }).start();
    }

    // Thread care asculta permanent pachetele UDP de la partener
    private void startReceiver() {
        isRunningNetwork = true;

        // Ne asiguram ca socket-ul exista inainte de a porni thread-ul
        initSocket();

        receiverThread = new Thread(() -> {
            byte[] buffer = new byte[4096];

            while (isRunningNetwork && !Thread.currentThread().isInterrupted()) {
                try {
                    if (udpSocket == null) continue;

                    DatagramPacket dp = new DatagramPacket(buffer, buffer.length);
                    udpSocket.receive(dp); // Blocant

                    String json = new String(dp.getData(), 0, dp.getLength());
                    SyncLocationPacket packet = gson.fromJson(json, SyncLocationPacket.class);

                    // LOG PENTRU DEPANARE (RECEIVE)
                    Log.d("UDP_RECEIVE", "PRIMIT de la " + dp.getAddress() + " tip: " + packet.type);

                    if (packet != null) {
                        runOnUiThread(() -> processIncomingPacket(packet));
                    }
                } catch (Exception e) {
                    Log.e("UDP_RECEIVE_ERR", "Eroare: " + e.getMessage());
                }
            }
        });
        receiverThread.start();
    }

    // Proceseaza pachetele primite si actualizeaza pozitiile pe harta
    private void processIncomingPacket(SyncLocationPacket packet) {
        LatLng pos = new LatLng(packet.lat, packet.lng);

        // LEADER-ul vede FOLLOWER-ul
        if (currentMode == AppMode.LEADER && packet.type.equals("FOLLOWER")) {
            if (followerMarker == null) {
                followerMarker = mMap.addMarker(new MarkerOptions()
                        .position(pos)
                        .title("FOLLOWER")
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));
            } else {
                followerMarker.setPosition(pos);
            }
        }

        // FOLLOWER-ul vede LEADER-ul
        else if (currentMode == AppMode.FOLLOWER && packet.type.equals("LEADER")) {
            if (leaderMarker == null) {
                leaderMarker = mMap.addMarker(new MarkerOptions()
                        .position(pos)
                        .title("LEADER")
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));
            } else {
                leaderMarker.setPosition(pos);
            }
            if (isCameraLocked) {
                // Mutam camera pe pozitia Leader-ului, pastrand zoom-ul curent
                mMap.animateCamera(CameraUpdateFactory.newLatLng(pos));
            }
            leaderPoints.add(pos);
            drawLeaderPolyline();
        }
    }

    // Logica jocului: Daca Follower este aproape de Leader, sterge traseul
    private void checkIfConsumingLine(Location myLoc) {
        if (leaderPoints.isEmpty()) return;
        boolean changed = false;
        Iterator<LatLng> iterator = leaderPoints.iterator();
        while (iterator.hasNext()) {
            LatLng p = iterator.next();
            float[] res = new float[1];
            Location.distanceBetween(myLoc.getLatitude(), myLoc.getLongitude(), p.latitude, p.longitude, res);
            if (res[0] < EAT_DISTANCE_METERS) {
                iterator.remove();
                changed = true;
            }
        }
        if (changed) drawLeaderPolyline();
    }

    // --- MODE SWITCHING ---

    // Afiseaza meniul de selectie a modurilor
    private void showModePopupMenu(View view) {
        PopupMenu popup = new PopupMenu(this, view);
        popup.getMenu().add("NORMAL");
        popup.getMenu().add("LEADER");
        popup.getMenu().add("FOLLOWER");

        popup.setOnMenuItemClickListener(item -> {
            stopNetworkModes();
            resetNormalRouteData();
            stopTimer();
            startBtn.show();
            stopBtn.hide();
            pauseBtn.hide();
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

    // Modifica setupLeader: Configureaza rolul de gazda
    private void setupLeader() {
        currentMode = AppMode.LEADER;
        sessionId = UUID.randomUUID().toString().substring(0, 6).toUpperCase(Locale.ROOT);
        initSocket();

        // Iti aratam propriul IP ca sa stii ce sa-i zici Follower-ului
        String myIp = getMyIpAddress();
        askForIp("EU SUNT LEADER.\nIP-ul meu este: " + myIp + "\n\nIntrodu IP-ul Follower-ului:");
    }

    // Configureaza rolul de client
    private void setupFollower() {
        currentMode = AppMode.FOLLOWER;
        initSocket();

        // ARATAM BUTONUL DE LOCK
        lockBtn.show();

        String myIp = getMyIpAddress();
        askForIp("EU SUNT FOLLOWER.\nIP-ul meu: " + myIp + "\nIntrodu IP Leader:");
    }

    // Dialog pentru introducerea IP-ului celuilalt telefon
    private void askForIp(String title) {
        AlertDialog.Builder b = new AlertDialog.Builder(this);
        b.setTitle(title);
        EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_PHONE);
        b.setView(input);
        b.setPositiveButton("OK", (d, w) -> {
            targetIp = input.getText().toString().trim();
            // Pornim receiver-ul care va asculta confirmarile
            startReceiver();
            Toast.makeText(this, "Target: " + targetIp, Toast.LENGTH_SHORT).show();
        });
        b.setCancelable(false);
        b.show();
    }

    // Opreste comunicarea UDP si curata harta
    private void stopNetworkModes() {
        isRunningNetwork = false;
        if (receiverThread != null) receiverThread.interrupt();
        new Thread(() -> {
            if (udpSocket != null) {
                udpSocket.close();
                udpSocket = null;
            }
        }).start();

        leaderPoints.clear();
        if (leaderPolyline != null) leaderPolyline.remove();
        if (leaderMarker != null) leaderMarker.remove();
        if (followerMarker != null) followerMarker.remove();
        leaderMarker = null;
        followerMarker = null;
        targetIp = null;
        lockBtn.hide(); // Ascundem butonul cand oprim modul
        isCameraLocked = false; // Resetam starea
        lockBtn.setImageTintList(android.content.res.ColorStateList.valueOf(Color.BLACK));
    }

    // --- UTILS ---

    // Cronometru pentru plimbare
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

    // Afiseaza ceasul curent al sistemului
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

    // Calculeaza ritmul (min/km) curent
    private String calculateCurrentPace(float segmentMeters, float seconds) {
        if (segmentMeters < 5 || seconds <= 0) return "0:00";
        float km = segmentMeters / 1000f;
        float pace = seconds / km;
        return String.format(Locale.getDefault(), "%d:%02d", (int)(pace/60), (int)(pace%60));
    }

    // Calculeaza ritmul mediu
    private String calculateAveragePace() {
        float km = totalDistanceMeters / 1000f;
        if (km <= 0) return "0:00";
        long elapsed = System.currentTimeMillis() - startTimeMillis;
        float pace = (elapsed / 1000f) / km;
        return String.format(Locale.getDefault(), "%d:%02d", (int)(pace/60), (int)(pace%60));
    }

    private void resetNormalRouteData() {
        routePoints.clear();
        if (routePolyline != null) routePolyline.remove();
        totalDistanceMeters = 0f;
        startTimeMillis = System.currentTimeMillis();
        lastUpdateTime = 0;
    }

    // Salvarea datelor in baza de date Firebase
    private void saveWalkToFirebase() {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) return;
        HashMap<String, Object> walk = new HashMap<>();
        walk.put("distanceKm", totalDistanceMeters / 1000f);
        walk.put("duration", timerText.getText().toString());
        walk.put("timestamp", System.currentTimeMillis());
        walk.put("avgPace", txtPace.getText().toString());
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
        ref.push().setValue(walk);
    }

    private void togglePause() {
        isPaused = !isPaused;
        pauseBtn.setImageResource(isPaused ? R.drawable.ic_play_arrow : R.drawable.ic_pause);
    }

    private void stopTrackingService() {
        if (locationCallback != null) locationClient.removeLocationUpdates(locationCallback);
        stopTimer();
        stopNetworkModes();
    }

    // Preluare date meteo prin API
    private void fetchWeather(double lat, double lng) {
        String url = "https://api.openweathermap.org/data/2.5/weather?lat=" + lat + "&lon=" + lng + "&units=metric&appid=" + API_KEY;
        Volley.newRequestQueue(this).add(new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        String city = response.getString("name");
                        double temp = response.getJSONObject("main").getDouble("temp");
                        String desc = response.getJSONArray("weather").getJSONObject(0).getString("description");
                        txtWeather.setText("Weather: " + city + "\n" + temp + "°C, " + desc);
                        weatherFetched = true;
                    } catch (JSONException e) { e.printStackTrace(); }
                }, error -> txtWeather.setText("Weather: Error")));
    }

    // Clasa pentru structura pachetelor de date trimise prin retea
    public static class SyncLocationPacket {
        String sessionId;
        String type;
        double lat;
        double lng;
        long timestamp;

        public SyncLocationPacket(String sessionId, String type, double lat, double lng, long timestamp) {
            this.sessionId = sessionId;
            this.type = type;
            this.lat = lat;
            this.lng = lng;
            this.timestamp = timestamp;
        }
    }
    // Adauga asta la finalul clasei, inainte de ultima acolada
    // Identifica IP-ul local pentru a putea fi partajat cu partenerul
    private String getMyIpAddress() {
        try {
            java.util.List<java.net.NetworkInterface> interfaces = java.util.Collections.list(java.net.NetworkInterface.getNetworkInterfaces());
            for (java.net.NetworkInterface intf : interfaces) {
                java.util.List<java.net.InetAddress> addrs = java.util.Collections.list(intf.getInetAddresses());
                for (java.net.InetAddress addr : addrs) {
                    if (!addr.isLoopbackAddress()) {
                        String sAddr = addr.getHostAddress();
                        // Vrem doar IPv4
                        boolean isIPv4 = sAddr.indexOf(':') < 0;
                        if (isIPv4) return sAddr;
                    }
                }
            }
        } catch (Exception ex) { } // Ignoram erorile
        return "Nu s-a gasit IP";
    }
}