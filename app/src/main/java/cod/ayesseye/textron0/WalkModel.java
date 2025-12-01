package cod.ayesseye.textron0;

import java.util.ArrayList;
import java.util.HashMap;

public class WalkModel {
    public float distanceKm;
    public String avgPace;
    public String duration;
    public long timestamp;
    public ArrayList<HashMap<String, Double>> route;

    public WalkModel() {}

    public WalkModel(float distanceKm, String avgPace, String duration,
                     long timestamp, ArrayList<HashMap<String, Double>> rote) {
        this.distanceKm = distanceKm;
        this.avgPace = avgPace;
        this.duration = duration;
        this.timestamp = timestamp;
        this.route = route;
    }
}
