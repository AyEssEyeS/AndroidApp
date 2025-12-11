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

public class WalksAdapter extends RecyclerView.Adapter<WalksAdapter.WalkViewHolder> {

    private ArrayList<WalkModel> walks;
    private OnWalkClickListener onWalkClickListener;
    private OnDeleteClickListener onDeleteClickListener;

    public interface OnWalkClickListener {
        void onWalkClick(WalkModel walk);
    }

    public interface OnDeleteClickListener {
        void onDeleteClick(WalkModel walk);
    }

    public WalksAdapter(ArrayList<WalkModel> walks,
                        OnWalkClickListener onWalkClickListener,
                        OnDeleteClickListener onDeleteClickListener) {

        this.walks = walks;
        this.onWalkClickListener = onWalkClickListener;
        this.onDeleteClickListener = onDeleteClickListener;
    }

    @NonNull
    @Override
    public WalkViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_walk, parent, false);
        return new WalkViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull WalkViewHolder holder, int position) {
        WalkModel walk = walks.get(position);

        holder.distance.setText(String.format("%.2f km", walk.distanceKm));
        holder.duration.setText(walk.duration);
        holder.avgPace.setText(walk.avgPace);

        String date = DateFormat.getDateInstance().format(new Date(walk.timestamp));
        holder.date.setText(date);

        // OPEN DETAILS
        holder.itemView.setOnClickListener(v ->
                onWalkClickListener.onWalkClick(walk)
        );

        // DELETE BUTTON ACTION
        holder.deleteBtn.setOnClickListener(v ->
                onDeleteClickListener.onDeleteClick(walk)
        );
    }

    @Override
    public int getItemCount() {
        return walks.size();
    }

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
