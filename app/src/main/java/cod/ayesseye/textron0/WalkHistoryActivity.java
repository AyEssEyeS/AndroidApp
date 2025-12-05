package cod.ayesseye.textron0;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;

public class WalkHistoryActivity extends AppCompatActivity {

    RecyclerView recyclerView;
    ArrayList<WalkModel> walks = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_walk_history);

        recyclerView = findViewById(R.id.walksRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        loadWalks();
    }

    private void loadWalks() {
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            finish();
            return;
        }

        DatabaseReference ref = FirebaseDatabase.getInstance()
                .getReference("users")
                .child(uid)
                .child("walks");

        ref.get().addOnSuccessListener(snapshot -> {
            walks.clear();
            for (DataSnapshot s : snapshot.getChildren()) {
                WalkModel walk = s.getValue(WalkModel.class);
                if (walk != null) {
                    walks.add(walk);
                }
            }

            WalksAdapter adapter = new WalksAdapter(walks, walk -> {
                Intent i = new Intent(WalkHistoryActivity.this, WalkDetailsActivity.class);
                i.putExtra("walk" , walk);
                startActivity(i);
            });

            recyclerView.setAdapter(adapter);
        });
    }
}