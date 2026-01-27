package cod.ayesseye.textron0;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;

// Adaptor pentru RecyclerView care gestioneaza afisarea listei de curse in interfata
public class WalksAdapter extends RecyclerView.Adapter<WalksAdapter.WalkViewHolder> {

    private ArrayList<WalkModel> walks;
    private OnWalkClickListener onWalkClickListener;
    private OnDeleteClickListener onDeleteClickListener;

    // Interfete pentru a transmite actiunile de click catre Activitatea parinte
    public interface OnWalkClickListener {
        void onWalkClick(WalkModel walk);
    }

    public interface OnDeleteClickListener {
        void onDeleteClick(WalkModel walk);
    }

    // Constructorul care initializeaza lista de date si listenerii pentru evenimente
    public WalksAdapter(ArrayList<WalkModel> walks,
                        OnWalkClickListener onWalkClickListener,
                        OnDeleteClickListener onDeleteClickListener) {

        this.walks = walks;
        this.onWalkClickListener = onWalkClickListener;
        this.onDeleteClickListener = onDeleteClickListener;
    }

    // Creaza instanta vizuala a unui element din lista (inflating layout)
    @NonNull
    @Override
    public WalkViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_walk, parent, false);
        return new WalkViewHolder(view);
    }

    // Leaga datele din modelul WalkModel de elementele vizuale ale randului curent
    @Override
    public void onBindViewHolder(@NonNull WalkViewHolder holder, int position) {
        WalkModel walk = walks.get(position);

        holder.distance.setText(String.format("%.2f km", walk.distanceKm));
        holder.duration.setText(walk.duration);
        holder.avgPace.setText(walk.avgPace);

        // Formateaza data calendaristica dintr-un timestamp
        String date = DateFormat.getDateInstance().format(new Date(walk.timestamp));
        holder.date.setText(date);

        // Gestioneaza click-ul pe intregul element pentru a deschide detaliile
        holder.itemView.setOnClickListener(v ->
                onWalkClickListener.onWalkClick(walk)
        );

        // Gestioneaza click-ul pe butonul de stergere
        holder.deleteBtn.setOnClickListener(v ->
                onDeleteClickListener.onDeleteClick(walk)
        );
    }

    @Override
    public int getItemCount() {
        return walks.size();
    }

    // Clasa ViewHolder pentru optimizarea performantei prin retinerea referintelor la View-uri
    public static class WalkViewHolder extends RecyclerView.ViewHolder {

        TextView distance, duration, avgPace, date;
        Button deleteBtn;

        public WalkViewHolder(@NonNull View itemView) {
            super(itemView);

            distance = itemView.findViewById(R.id.walkDistance);
            duration = itemView.findViewById(R.id.walkDuration);
            avgPace = itemView.findViewById(R.id.walkAvgPace);
            date = itemView.findViewById(R.id.walkDate);

            deleteBtn = itemView.findViewById(R.id.deleteButton);
        }
    }
}