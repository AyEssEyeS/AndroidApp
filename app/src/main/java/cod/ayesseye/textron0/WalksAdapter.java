package cod.ayesseye.textron0;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;

public class WalksAdapter extends RecyclerView.Adapter<WalksAdapter.WalkViewHolder> {

    private ArrayList<WalkModel> walks;
    private OnWalkClickListener listener;

    public interface  OnWalkClickListener {
        void onWalkClick (WalkModel walk);
    }

    public WalksAdapter(ArrayList<WalkModel> walks, OnWalkClickListener listener) {
        this.walks = walks;
        this.listener = listener;
    }

    @NonNull
    @Override
    public WalkViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_walk, parent, false);
        return  new WalkViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull WalkViewHolder holder, int position) {
        WalkModel walk = walks.get(position);
        holder.distance.setText(String.format("%.2f km", walk.distanceKm));
        holder.duration.setText(walk.duration);
        holder.avgPace.setText(walk.avgPace);

        //convert timestamp to date
        String date = DateFormat.getDateInstance().format(new Date(walk.timestamp));
        holder.date.setText(date);
        holder.itemView.setOnClickListener(v -> listener.onWalkClick(walk));
    }

    @Override
    public int getItemCount() {
        return walks.size();
    }

    public static class WalkViewHolder extends RecyclerView.ViewHolder {
        TextView distance, duration, avgPace, date;

        public WalkViewHolder (@NonNull View itemView) {
            super(itemView);
            distance = itemView.findViewById(R.id.walkDistance);
            duration = itemView.findViewById(R.id.walkDuration);
            avgPace = itemView.findViewById(R.id.walkAvgPace);
            date = itemView.findViewById(R.id.walkDate);
        }
    }

}
