package cod.ayesseye.textron0;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.ArrayList;
import java.util.HashMap;

public class WalkDetailsActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private WalkModel walk;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_walk_details);

        //Receive WalkModel from WalkHistoryActivity
        walk = (WalkModel) getIntent().getSerializableExtra("walk");

        if (walk == null) {
            Log.e("DETAILS", "Walk is NULL");
            finish();
            return;
        }

        //Initialize map fragment
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.detailsMap);

        mapFragment.getMapAsync(this);
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;

        //Converting saved route to LatLng list
        ArrayList<LatLng> polyPoints = new ArrayList<>();

        for (HashMap<String, Double> p : walk.route) {
            double lat = p.get("lat");
            double lng = p.get("lng");
            polyPoints.add(new LatLng(lat, lng));
        }

        if (polyPoints.size() < 2) {
            Log.e("DETAILS", "Not enough points to draw a line.");
            return;
        }

        //Draw Line, polyLine
        PolylineOptions options = new PolylineOptions()
                .addAll(polyPoints)
                .color(Color.BLUE)
                .width(12);

        mMap.addPolyline(options);

        //Move camera to see entire route
        LatLngBounds.Builder boundsBuilder = new LatLngBounds.Builder();
        for (LatLng p : polyPoints) {
            boundsBuilder.include(p);
        }

        //Animate camera to fit route with padding
        mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(boundsBuilder.build(), 100));
    }
}