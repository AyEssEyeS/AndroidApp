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

import com.android.volley.toolbox.JsonObjectRequest;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


public class OpenWeather extends AppCompatActivity {

    private static final int LOCATION_REQUEST_CODE = 1001; // ID folosit cand cerem permisiuni
    private FusedLocationProviderClient fusedLocationClient; // Obiect GPlayServ care preia locatia curent

    private static final String API_KEY = "3496e86f154d566be3a095951868974f";
    Button homeBtn, mapsBtn;

    TextView tvCity, tvTemperature, tvDescription, tvHumidity;
    ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_open_weather);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        tvCity = findViewById(R.id.tvCity);
        tvTemperature = findViewById(R.id.tvTemperature);
        tvDescription = findViewById(R.id.tvDescription);
        tvHumidity = findViewById(R.id.tvHumidity);
        progressBar = findViewById(R.id.progressBar);
        homeBtn = findViewById(R.id.btn_home);
        mapsBtn = findViewById(R.id.btn_maps);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        requestLocationPermission();

        homeBtn.setOnClickListener(v -> changePage(MainActivity.class));
        mapsBtn.setOnClickListener(v -> changePage(MapsActivity.class));
    }

    private void requestLocationPermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
        != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_REQUEST_CODE);
        } else {
            getUserLocation();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, int @NotNull [] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == LOCATION_REQUEST_CODE && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            getUserLocation();
        } else {
            Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show();
        }
    }

    private void getUserLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        tvCity.setText(R.string.fetching_location);

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(location -> {
                    if (location != null) {
                        double lat  = location.getLatitude();
                        double lon = location.getLongitude();

                        Log.d("LOCATION", "Lat: " + lat + ", Lon: " + lon);

                        fetchWeatherData(lat, lon);
                    } else {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(this, "Unable to get location.", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Location error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
    private void fetchWeatherData(double latitude, double longitude) {

        String url = "https://api.openweathermap.org/data/2.5/weather?"
                + "lat=" + latitude
                + "&lon=" + longitude
                + "&units=metric"
                + "&appid=" + API_KEY;

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.GET, url, null,
                response -> {
                    progressBar.setVisibility(View.GONE);

                    try {
                        String city = response.getString("name");

                        JSONObject main = response.getJSONObject("main");
                        double temp = main.getDouble("temp");
                        int humidity = main.getInt("humidity");

                        JSONArray weatherArr = response.getJSONArray("weather");
                        JSONObject weather = weatherArr.getJSONObject(0);
                        String description = weather.getString("description");

                        tvCity.setText(getString(R.string.city) + " " + city);
                        tvTemperature.setText(getString(R.string.temperature, temp));
                        tvDescription.setText(getString(R.string.description) + " " + description);
                        tvHumidity.setText(getString(R.string.humidity)  + " " + humidity + "%");

                    } catch (JSONException e) {
                        tvCity.setText(R.string.city_error);
                        Toast.makeText(this,
                                "Parsing error: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this,
                            "Weather fetch failed.",
                            Toast.LENGTH_LONG).show();

                    tvCity.setText(R.string.city_error);
                    tvTemperature.setText(R.string.temperature_na);
                    tvDescription.setText(R.string.description_na);
                    tvHumidity.setText(R.string.humidity_na);
                }
        );
        AppController.getInstance().addToRequestQueue(request);
    }

    private void changePage(Class<?> targetActivity) {
        Intent intent = new Intent(this, targetActivity);
        startActivity(intent);
    }
}