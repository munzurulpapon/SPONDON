package com.example.spondon;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class ReportAdapter extends RecyclerView.Adapter<ReportAdapter.ReportViewHolder> {

    public interface OnReportClickListener {
        void onReportClick(ReportItem report);
    }

    private final List<ReportItem> reportList;
    private final OnReportClickListener clickListener;

    public ReportAdapter(List<ReportItem> reportList, OnReportClickListener clickListener) {
        this.reportList = reportList != null ? reportList : new ArrayList<>();
        this.clickListener = clickListener;
    }

    @NonNull
    @Override
    public ReportViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_report_card, parent, false);
        return new ReportViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ReportViewHolder holder, int position) {
        ReportItem item = reportList.get(position);
        holder.bind(item, clickListener);
    }

    @Override
    public int getItemCount() {
        return reportList.size();
    }

    public static class ReportViewHolder extends RecyclerView.ViewHolder {

        private final TextView tvCardCategory;
        private final TextView tvCardStatus;
        private final TextView tvCardTitle;
        private final TextView tvCardDescription;
        private final TextView tvCardLocation;
        private final TextView tvCardAction;
        private final TextView tvCardTime;
        private final android.widget.ImageView ivCardThumb;

        public ReportViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCardCategory = itemView.findViewById(R.id.tvCardCategory);
            tvCardStatus = itemView.findViewById(R.id.tvCardStatus);
            tvCardTitle = itemView.findViewById(R.id.tvCardTitle);
            tvCardDescription = itemView.findViewById(R.id.tvCardDescription);
            tvCardLocation = itemView.findViewById(R.id.tvCardLocation);
            tvCardAction = itemView.findViewById(R.id.tvCardAction);
            tvCardTime = itemView.findViewById(R.id.tvCardTime);
            ivCardThumb = itemView.findViewById(R.id.ivCardThumb);
        }

        public void bind(ReportItem item, OnReportClickListener listener) {
            if (tvCardCategory != null) {
                tvCardCategory.setText(item.getCategory() != null ? item.getCategory() : "Civic Issue");
            }
            if (tvCardTitle != null) {
                tvCardTitle.setText(item.getTitle() != null ? item.getTitle() : "Untitled Report");
            }
            if (tvCardDescription != null) {
                tvCardDescription.setText(item.getDescription() != null && !item.getDescription().isEmpty()
                        ? item.getDescription() : "No detailed description provided.");
            }
            if (tvCardLocation != null) {
                tvCardLocation.setText("📍 " + (item.getLocation() != null ? item.getLocation() : "Khulna City"));
            }
            if (tvCardTime != null) {
                String time = item.getCreatedAt();
                tvCardTime.setText(time != null && !time.isEmpty() ? time : "Recently");
            }

            if (ivCardThumb != null) {
                if (item.getImageUri() != null && !item.getImageUri().isEmpty()) {
                    try {
                        android.net.Uri uri = android.net.Uri.parse(item.getImageUri());
                        ivCardThumb.setImageURI(uri);
                    } catch (Exception ignored) {
                        ivCardThumb.setImageResource(R.drawable.ic_report_problem_blue);
                    }
                } else {
                    ivCardThumb.setImageResource(R.drawable.ic_report_problem_blue);
                }
            }

            String status = item.getStatus() != null ? item.getStatus() : "Pending";
            if (tvCardStatus != null) {
                tvCardStatus.setText(status);
                applyStatusStyle(status);
            }

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onReportClick(item);
                }
            });
        }

        private void applyStatusStyle(String status) {
            int bg;
            int text;

            switch (status) {
                case "Resolved":
                    bg = Color.parseColor("#DCFCE7");
                    text = Color.parseColor("#166534");
                    break;
                case "In Progress":
                    bg = Color.parseColor("#DBEAFE");
                    text = Color.parseColor("#1E40AF");
                    break;
                case "Pending":
                    bg = Color.parseColor("#FFEDD5");
                    text = Color.parseColor("#C2410C");
                    break;
                case "Rejected":
                    bg = Color.parseColor("#FEE2E2");
                    text = Color.parseColor("#991B1B");
                    break;
                default: // Submitted
                    bg = Color.parseColor("#FEF3C7");
                    text = Color.parseColor("#92400E");
                    break;
            }

            GradientDrawable gd = new GradientDrawable();
            gd.setColor(bg);
            gd.setCornerRadius(12);
            tvCardStatus.setBackground(gd);
            tvCardStatus.setTextColor(text);
        }
    }
}
