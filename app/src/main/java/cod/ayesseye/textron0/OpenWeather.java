package cod.ayesseye.textron0;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class OpenWeather extends AppCompatActivity {

    // Constants for permissions and API configuration
    private static final int LOCATION_REQUEST_CODE = 1001;
    // Note: In a production app, store this key in a secure place (like local.properties)
    private static final String API_KEY = "3496e86f154d566be3a095951868974f";

    // Location client to retrieve GPS coordinates
    private FusedLocationProviderClient fusedLocationClient;

    // UI Elements
    private Button homeBtn, mapsBtn;
    private TextView tvCity, tvTemperature, tvDescription, tvHumidity;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Enable Edge-to-Edge display for modern Android look
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_open_weather);

        // Handle window insets to prevent UI overlap with system bars
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize UI components
        initializeViews();

        // Initialize Location Services
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Check for permissions and fetch location/weather
        checkAndRequestPermissions();

        // Setup navigation buttons
        setupNavigation();
    }

    /**
     * Binds XML IDs to Java objects.
     */
    private void initializeViews() {
        tvCity = findViewById(R.id.tvCity);
        tvTemperature = findViewById(R.id.tvTemperature);
        tvDescription = findViewById(R.id.tvDescription);
        tvHumidity = findViewById(R.id.tvHumidity);
        progressBar = findViewById(R.id.progressBar);
        homeBtn = findViewById(R.id.btn_home);
        mapsBtn = findViewById(R.id.btn_maps);
    }

    /**
     * Sets up click listeners for the bottom navigation bar.
     */
    private void setupNavigation() {
        homeBtn.setOnClickListener(v -> {
            changePage(MainActivity.class);
            finish(); // Closes this activity so back button behaves correctly
        });

        mapsBtn.setOnClickListener(v -> {
            changePage(MapsActivity.class);
            finish();
        });
    }

    /**
     * Checks if the user has granted Fine Location permission.
     * If yes, gets location. If no, requests permission.
     */
    private void checkAndRequestPermissions() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            // Request permission dialog
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_REQUEST_CODE);
        } else {
            // Permission already granted
            getCurrentLocation();
        }
    }

    /**
     * Callback received when user accepts or denies permission.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == LOCATION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getCurrentLocation();
            } else {
                Toast.makeText(this, "Location permission is required for local weather.", Toast.LENGTH_SHORT).show();
                progressBar.setVisibility(View.GONE);
                tvCity.setText(R.string.city_error);
            }
        }
    }

    /**
     * Retrieves the last known location of the device.
     */
    private void getCurrentLocation() {
        // Security check required by Android Studio
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        tvCity.setText(R.string.fetching_location);

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, location -> {
                    if (location != null) {
                        // Location found, fetch weather data
                        double lat = location.getLatitude();
                        double lon = location.getLongitude();
                        Log.d("OpenWeather", "Coordinates: " + lat + ", " + lon);

                        fetchWeatherDataFromApi(lat, lon);
                    } else {
                        // Location is null (common in emulators or if GPS was off)
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(this, "Could not determine location. Try opening Google Maps first.", Toast.LENGTH_LONG).show();
                    }
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "GPS Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * Makes a network request to OpenWeatherMap API using Volley.
     * @param latitude Device latitude
     * @param longitude Device longitude
     */
    private void fetchWeatherDataFromApi(double latitude, double longitude) {
        String url = "https://api.openweathermap.org/data/2.5/weather?"
                + "lat=" + latitude
                + "&lon=" + longitude
                + "&units=metric" // Ensures Celsius
                + "&appid=" + API_KEY;

        // Create a new request queue
        RequestQueue queue = Volley.newRequestQueue(this);

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(
                Request.Method.GET,
                url,
                null,
                response -> {
                    // Success Callback
                    progressBar.setVisibility(View.GONE);
                    parseAndDisplayWeather(response);
                },
                error -> {
                    // Error Callback
                    progressBar.setVisibility(View.GONE);
                    Log.e("OpenWeather", "Network Error: " + error.toString());
                    Toast.makeText(this, "Failed to load weather data.", Toast.LENGTH_SHORT).show();

                    // Set fallback text
                    tvCity.setText(R.string.city_error);
                    tvTemperature.setText(R.string.temperature_na);
                }
        );

        // Add the request to the queue to execute it
        queue.add(jsonObjectRequest);
    }

    /**
     * Parses the JSON response and updates the UI.
     * @param response The JSON object returned by the API.
     */
    private void parseAndDisplayWeather(JSONObject response) {
        try {
            // 1. Extract City Name
            String cityName = response.getString("name");

            // 2. Extract Main Data (Temp, Humidity)
            JSONObject mainObj = response.getJSONObject("main");
            double temp = mainObj.getDouble("temp");
            int humidity = mainObj.getInt("humidity");

            // 3. Extract Weather Description (it is inside an array)
            JSONArray weatherArray = response.getJSONArray("weather");
            JSONObject weatherObj = weatherArray.getJSONObject(0);
            String description = weatherObj.getString("description");

            // 4. Update UI Elements
            // Using String.format or concatenation based on your string resources
            tvCity.setText(cityName);

            // Assuming your strings.xml has placeholders like "Temp: %1$s"
            // If strictly using the IDs you provided:
            tvTemperature.setText(String.format(java.util.Locale.getDefault(), "%.1f°C", temp));
            tvDescription.setText(description.substring(0, 1).toUpperCase() + description.substring(1)); // Capitalize first letter
            tvHumidity.setText("Humidity: " + humidity + "%");

        } catch (JSONException e) {
            Log.e("OpenWeather", "JSON Parsing Error", e);
            Toast.makeText(this, "Error reading weather data.", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Helper method to switch activities.
     */
    private void changePage(Class<?> targetActivity) {
        Intent intent = new Intent(this, targetActivity);
        startActivity(intent);
    }
}