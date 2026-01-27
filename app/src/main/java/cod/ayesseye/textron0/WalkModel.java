package cod.ayesseye.textron0;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;

// Model de date (POJO) care reprezinta o sesiune de alergare/mers
// Implementeaza Serializable pentru a putea fi transferat intre Activitati prin Intent
public class WalkModel implements Serializable {

    public String key;  // Cheia unica din Firebase, necesara pentru identificarea si stergerea cursei

    public float distanceKm;
    public String avgPace;
    public String duration;
    public long timestamp;

    // Lista de puncte GPS stocata ca HashMap pentru compatibilitate directa cu structura JSON din Firebase
    public ArrayList<HashMap<String, Double>> route;

    // Constructor gol obligatoriu pentru deserializarea automata a datelor din Firebase
    public WalkModel() {
        // Required empty constructor for Firebase
    }

    // Constructor complet pentru crearea unui nou obiect inainte de salvare
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