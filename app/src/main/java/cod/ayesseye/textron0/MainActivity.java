package cod.ayesseye.textron0;


import android.content.Intent;

import android.os.Bundle;
import android.widget.Button;


import androidx.activity.EdgeToEdge;

import androidx.appcompat.app.AppCompatActivity;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;


// Ecranul principal (Dashboard) care serveste drept meniu de navigare
public class MainActivity extends AppCompatActivity {

    Button goToOpenWeather, goToGoogleMaps,  gotoHistory, logOut;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        // Configureaza afisarea corecta a interfetei pe tot ecranul (Edge-to-Edge)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });


        // Initializarea elementelor de UI (butoane)
        goToOpenWeather = findViewById(R.id.open_weather);
        goToGoogleMaps = findViewById(R.id.google_maps);
        gotoHistory = findViewById(R.id.go_to_history);
        logOut = findViewById(R.id.btnLogout);

        // Configurarea listenerilor pentru navigarea catre modulele principale
        goToOpenWeather.setOnClickListener(v -> changePage(OpenWeather.class));
        goToGoogleMaps.setOnClickListener(v -> changePage(MapsActivity.class));
        gotoHistory.setOnClickListener(v -> changePage(WalkHistoryActivity.class));

        // Logica de delogare: Deconecteaza Firebase si revine la Login
        logOut.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            changePage(LoginPage.class);
            finish();
        });


    }
    // Metoda helper pentru tranzitia simplificata intre Activitati
    private void changePage(Class<?> targetActivity) {
        Intent intent = new Intent(this, targetActivity);
        startActivity(intent);
    }
}