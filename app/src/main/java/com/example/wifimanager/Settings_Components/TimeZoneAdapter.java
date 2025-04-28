package com.example.wifimanager.Settings_Components;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.wifimanager.R;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class TimeZoneAdapter extends RecyclerView.Adapter<TimeZoneAdapter.TimeZoneViewHolder> {

    private List<TimeZoneItem> timeZoneItems;
    private List<TimeZoneItem> originalTimeZoneItems;
    private OnTimeZoneSelectedListener listener;
    private int selectedPosition = -1;

    public TimeZoneAdapter(List<TimeZoneItem> timeZoneItems) {
        this.timeZoneItems = timeZoneItems;
        this.originalTimeZoneItems = new ArrayList<>(timeZoneItems);
    }

    public void setOnTimeZoneSelectedListener(OnTimeZoneSelectedListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public TimeZoneViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_time_zone, parent, false);
        return new TimeZoneViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TimeZoneViewHolder holder, int position) {
        TimeZoneItem timeZoneItem = timeZoneItems.get(position);
        holder.timeZoneName.setText(timeZoneItem.getDisplayName());

        // Format current time in this time zone
        SimpleDateFormat sdf = new SimpleDateFormat("h:mm a", Locale.getDefault());
        sdf.setTimeZone(TimeZone.getTimeZone(timeZoneItem.getTimeZoneId()));
        String currentTime = "Current time: " + sdf.format(new Date());
        holder.timeZoneCurrentTime.setText(currentTime);

        // Highlight selected item
        holder.itemView.setSelected(selectedPosition == position);
    }

    @Override
    public int getItemCount() {
        return timeZoneItems.size();
    }

    public void filter(String query) {
        timeZoneItems.clear();
        if (query.isEmpty()) {
            timeZoneItems.addAll(originalTimeZoneItems);
        } else {
            query = query.toLowerCase();
            for (TimeZoneItem item : originalTimeZoneItems) {
                if (item.getDisplayName().toLowerCase().contains(query)) {
                    timeZoneItems.add(item);
                }
            }
        }
        notifyDataSetChanged();
    }

    public void setSelectedPosition(int position) {
        int previousPosition = selectedPosition;
        selectedPosition = position;
        notifyItemChanged(previousPosition);
        notifyItemChanged(selectedPosition);
    }

    public TimeZoneItem getSelectedTimeZone() {
        if (selectedPosition >= 0 && selectedPosition < timeZoneItems.size()) {
            return timeZoneItems.get(selectedPosition);
        }
        return null;
    }

    class TimeZoneViewHolder extends RecyclerView.ViewHolder {
        TextView timeZoneName;
        TextView timeZoneCurrentTime;

        TimeZoneViewHolder(@NonNull View itemView) {
            super(itemView);
            timeZoneName = itemView.findViewById(R.id.time_zone_name);
            timeZoneCurrentTime = itemView.findViewById(R.id.time_zone_current_time);

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        setSelectedPosition(position);
                        listener.onTimeZoneSelected(timeZoneItems.get(position));
                    }
                }
            });
        }
    }

    public interface OnTimeZoneSelectedListener {
        void onTimeZoneSelected(TimeZoneItem timeZoneItem);
    }

    public static class TimeZoneItem {
        private String timeZoneId;
        private String displayName;

        public TimeZoneItem(String timeZoneId, String displayName) {
            this.timeZoneId = timeZoneId;
            this.displayName = displayName;
        }

        public String getTimeZoneId() {
            return timeZoneId;
        }

        public String getDisplayName() {
            return displayName;
        }
    }
}