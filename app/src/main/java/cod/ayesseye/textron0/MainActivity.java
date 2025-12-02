package cod.ayesseye.textron0;


import android.content.Intent;

import android.os.Bundle;
import android.widget.Button;


import androidx.activity.EdgeToEdge;

import androidx.appcompat.app.AppCompatActivity;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;


public class MainActivity extends AppCompatActivity {

    Button goToOpenWeather, goToGoogleMaps,  gotoHistory, goToDetails;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });


        goToOpenWeather = findViewById(R.id.open_weather);
        goToGoogleMaps = findViewById(R.id.google_maps);
        gotoHistory = findViewById(R.id.go_to_history);
        goToDetails = findViewById(R.id.go_to_walk_details);

        goToOpenWeather.setOnClickListener(v -> changePage(OpenWeather.class));
        goToGoogleMaps.setOnClickListener(v -> changePage(MapsActivity.class));
        gotoHistory.setOnClickListener(v -> changePage(WalkHistoryActivity.class));
        goToDetails.setOnClickListener(v -> changePage(WalkDetailsActivity.class));


    }
        private void changePage(Class<?> targetActivity) {
        Intent intent = new Intent(this, targetActivity);
        startActivity(intent);
    }
}