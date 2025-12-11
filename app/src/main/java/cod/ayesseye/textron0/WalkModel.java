package cod.ayesseye.textron0;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;

public class WalkModel implements Serializable {

    public String key;  // Firebase key for deleting the walk

    public float distanceKm;
    public String avgPace;
    public String duration;
    public long timestamp;
    public ArrayList<HashMap<String, Double>> route;

    public WalkModel() {
        // Required empty constructor for Firebase
    }

    public WalkModel(float distanceKm,
                     String avgPace,
                     String duration,
                     long timestamp,
                     ArrayList<HashMap<String, Double>> route) {

        this.distanceKm = distanceKm;
        this.avgPace = avgPace;
        this.duration = duration;
        this.timestamp = timestamp;
        this.route = route;
    }
}
