package cod.ayesseye.textron0;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.Collections;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;

// Activitate care afiseaza lista tuturor antrenamentelor salvate de utilizator
public class WalkHistoryActivity extends AppCompatActivity {

    RecyclerView recyclerView;
    ArrayList<WalkModel> walks = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_walk_history);

        // Configurare RecyclerView pentru afisarea listei in format vertical
        recyclerView = findViewById(R.id.walksRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        loadWalks();
    }

    // Incarca datele asincron din Firebase Realtime Database
    private void loadWalks() {
        // Verificare de securitate: utilizatorul trebuie sa fie autentificat
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            finish();
            return;
        }

        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // Referinta catre nodul 'walks' specific utilizatorului curent
        DatabaseReference ref = FirebaseDatabase.getInstance()
                .getReference("users")
                .child(uid)
                .child("walks");

        // Ascultator pentru citirea datelor (Single Value Event)
        ref.get().addOnSuccessListener(snapshot -> {
            walks.clear();

            // Iterare prin snapshot-uri si convertire JSON in obiecte WalkModel
            for (DataSnapshot s : snapshot.getChildren()) {
                WalkModel walk = s.getValue(WalkModel.class);
                if (walk != null) {
                    walk.key = s.getKey(); // <-- SALVARE CHEIE (necesara pentru stergere)
                    walks.add(walk);
                }
            }
            // Inversare lista pentru a afisa cele mai recente curse primele
            Collections.reverse(walks);

            // Initializare adaptor cu functii Lambda pentru actiunile utilizatorului
            WalksAdapter adapter = new WalksAdapter(

                    walks,

                    // Actiunea 1: Click pe element -> Deschide Detalii
                    walk -> {
                        Intent i = new Intent(WalkHistoryActivity.this, WalkDetailsActivity.class);
                        i.putExtra("walk", walk);
                        startActivity(i);
                    },

                    // Actiunea 2: Click pe stergere -> Sterge din baza de date
                    walk -> {
                        deleteWalk(walk);
                    }
            );

            recyclerView.setAdapter(adapter);
            recyclerView.setHasFixedSize(false);
            adapter.notifyDataSetChanged();
        });
    }

    // Sterge inregistrarea selectata atat din Cloud cat si din lista locala
    private void deleteWalk(WalkModel walk) {
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        DatabaseReference ref = FirebaseDatabase.getInstance()
                .getReference("users")
                .child(uid)
                .child("walks")
                .child(walk.key);

        ref.removeValue().addOnSuccessListener(unused -> {
            walks.remove(walk);
            recyclerView.getAdapter().notifyDataSetChanged();
        });
    }

}