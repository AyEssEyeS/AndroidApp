package cod.ayesseye.textron0;


import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;


import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.ArrayList;
import java.util.HashMap;

public class WalkDetailsActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private WalkModel walk;
    private PolylineOptions replayOptions;
    private Handler handler = new Handler();
    private Polyline replayLine;

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

        // Enable user location (needs permissions!)
        try {
            mMap.setMyLocationEnabled(true);
        } catch (SecurityException e) {
            Log.e("MAP", "Location permission not granted");
        }

        ArrayList<LatLng> polyPoints = new ArrayList<>();
        for (HashMap<String, Double> p : walk.route) {
            polyPoints.add(new LatLng(p.get("lat"), p.get("lng")));
        }

        LatLng startPoint = polyPoints.get(0);
        LatLng endPoint = polyPoints.get(polyPoints.size() - 1);

        // Add START marker
        mMap.addMarker(new MarkerOptions()
                .position(startPoint)
                .title("Start")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));

        //  Add END marker
        mMap.addMarker(new MarkerOptions()
                .position(endPoint)
                .title("End")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_CYAN))
        );

        if (polyPoints.size() < 2) return;

        // Start camera at first point
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(polyPoints.get(0), 17f));

        // Draw full route
        mMap.addPolyline(new PolylineOptions()
                .addAll(polyPoints)
                .color(Color.BLUE)
                .width(12));

        // Button to replay
        Button replayBtn = findViewById(R.id.btnReplay);
        replayBtn.setOnClickListener(v -> replayRun(polyPoints));
    }

    private void replayRun(ArrayList<LatLng> points) {

        if (replayLine != null) replayLine.remove(); // Remove old animation

        replayOptions = new PolylineOptions()
                .color(Color.RED)
                .width(12);

        replayLine = mMap.addPolyline(new PolylineOptions().width(12).color(Color.RED));

        final int[] index = {0};

        handler.postDelayed(new Runnable() {
            @Override
            public void run() {

                if (index[0] < points.size()) {
                    ArrayList<LatLng> currentList = new ArrayList<>(replayLine.getPoints());
                    currentList.add(points.get(index[0]));
                    replayLine.setPoints(currentList);

                    mMap.animateCamera(CameraUpdateFactory.newLatLng(points.get(index[0])), 300, null);

                    index[0]++;
                    handler.postDelayed(this, 250); // speed of replay
                }
            }
        }, 100);
    }

}