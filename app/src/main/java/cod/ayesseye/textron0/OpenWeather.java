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

// Activitatea responsabila cu afisarea datelor meteo in timp real bazate pe locatia utilizatorului
public class OpenWeather extends AppCompatActivity {

    // Constante pentru codul cererii de locatie si cheia API OpenWeatherMap
    private static final int LOCATION_REQUEST_CODE = 1001;
    private static final String API_KEY = "3496e86f154d566be3a095951868974f";

    // Client pentru accesarea serviciilor de locatie Google (GPS)
    private FusedLocationProviderClient fusedLocationClient;

    // Elemente de interfata (UI)
    private Button homeBtn, mapsBtn;
    private TextView tvCity, tvTemperature, tvDescription, tvHumidity;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Activare afisare Edge-to-Edge si setare layout
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_open_weather);

        // Gestionare padding pentru barele de sistem
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initializare componente UI si client Locatie
        initializeViews();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Verificare permisiuni si pornire flux de obtinere date meteo
        checkAndRequestPermissions();

        // Configurare navigare butoane inferioare
        setupNavigation();
    }

    // Leaga variabilele Java de elementele din fisierul XML
    private void initializeViews() {
        tvCity = findViewById(R.id.tvCity);
        tvTemperature = findViewById(R.id.tvTemperature);
        tvDescription = findViewById(R.id.tvDescription);
        tvHumidity = findViewById(R.id.tvHumidity);
        progressBar = findViewById(R.id.progressBar);
        homeBtn = findViewById(R.id.btn_home);
        mapsBtn = findViewById(R.id.btn_maps);
    }

    // Configureaza listenerii pentru butoanele de navigare (Acasa, Harta)
    private void setupNavigation() {
        homeBtn.setOnClickListener(v -> {
            changePage(MainActivity.class);
            finish(); // Inchide activitatea curenta pentru a elibera memoria
        });

        mapsBtn.setOnClickListener(v -> {
            changePage(MapsActivity.class);
            finish();
        });
    }

    // Verifica daca permisiunea de Locatie este acordata; daca nu, o solicita utilizatorului
    private void checkAndRequestPermissions() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            // Afiseaza dialogul de sistem pentru permisiune
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_REQUEST_CODE);
        } else {
            // Permisiunea exista deja, obtinem locatia
            getCurrentLocation();
        }
    }

    // Callback apelat dupa ce utilizatorul raspunde la dialogul de permisiuni
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

    // Obtine coordonatele GPS (Latitudine, Longitudine) folosind FusedLocationClient
    private void getCurrentLocation() {
        // Verificare de securitate obligatorie pentru Android
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        tvCity.setText(R.string.fetching_location);

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, location -> {
                    if (location != null) {
                        // Locatie gasita, apelam API-ul Meteo
                        double lat = location.getLatitude();
                        double lon = location.getLongitude();
                        Log.d("OpenWeather", "Coordinates: " + lat + ", " + lon);

                        fetchWeatherDataFromApi(lat, lon);
                    } else {
                        // Locatie nula (posibil GPS oprit)
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(this, "Could not determine location. Try opening Google Maps first.", Toast.LENGTH_LONG).show();
                    }
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "GPS Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    // Executa cererea HTTP catre API-ul OpenWeatherMap folosind biblioteca Volley
    private void fetchWeatherDataFromApi(double latitude, double longitude) {
        String url = "https://api.openweathermap.org/data/2.5/weather?"
                + "lat=" + latitude
                + "&lon=" + longitude
                + "&units=metric" // Asigura date in grade Celsius
                + "&appid=" + API_KEY;

        // Creare coada de cereri
        RequestQueue queue = Volley.newRequestQueue(this);

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(
                Request.Method.GET,
                url,
                null,
                response -> {
                    // Succes: Ascunde bara de progres si proceseaza JSON-ul
                    progressBar.setVisibility(View.GONE);
                    parseAndDisplayWeather(response);
                },
                error -> {
                    // Eroare retea: Afiseaza mesaj corespunzator
                    progressBar.setVisibility(View.GONE);
                    Log.e("OpenWeather", "Network Error: " + error.toString());
                    Toast.makeText(this, "Failed to load weather data.", Toast.LENGTH_SHORT).show();

                    tvCity.setText(R.string.city_error);
                    tvTemperature.setText(R.string.temperature_na);
                }
        );

        // Adaugare cerere in coada de executie
        queue.add(jsonObjectRequest);
    }

    // Parseaza raspunsul JSON de la server si actualizeaza elementele vizuale
    private void parseAndDisplayWeather(JSONObject response) {
        try {
            // 1. Extragere Nume Oras
            String cityName = response.getString("name");

            // 2. Extragere Date Principale (Temperatura, Umiditate)
            JSONObject mainObj = response.getJSONObject("main");
            double temp = mainObj.getDouble("temp");
            int humidity = mainObj.getInt("humidity");

            // 3. Extragere Descriere Meteo (din array-ul 'weather')
            JSONArray weatherArray = response.getJSONArray("weather");
            JSONObject weatherObj = weatherArray.getJSONObject(0);
            String description = weatherObj.getString("description");

            // 4. Actualizare UI
            tvCity.setText(cityName);

            // Formatare temperatura si text
            tvTemperature.setText(String.format(java.util.Locale.getDefault(), "%.1f°C", temp));
            tvDescription.setText(description.substring(0, 1).toUpperCase() + description.substring(1)); // Capitalizare prima litera
            tvHumidity.setText("Humidity: " + humidity + "%");

        } catch (JSONException e) {
            Log.e("OpenWeather", "JSON Parsing Error", e);
            Toast.makeText(this, "Error reading weather data.", Toast.LENGTH_SHORT).show();
        }
    }

    // Metoda helper pentru schimbarea activitatii
    private void changePage(Class<?> targetActivity) {
        Intent intent = new Intent(this, targetActivity);
        startActivity(intent);
    }
}