package com.taile.knot.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.taile.knot.data.model.Knot;
import com.taile.knot.databinding.ItemTimelineFooterBinding;
import com.taile.knot.databinding.ItemTimelineHeaderBinding;
import com.taile.knot.databinding.ItemTimelineKnotBinding;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TimelineAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_KNOT = 1;
    private static final int TYPE_FOOTER = 2;

    private final List<Knot> knots = new ArrayList<>();
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
    private OnAddKnotClickListener onAddKnotClickListener;

    public interface OnAddKnotClickListener {
        void onAddKnotClick();
    }

    public void setOnAddKnotClickListener(OnAddKnotClickListener listener) {
        this.onAddKnotClickListener = listener;
    }
    public void submitList(List<Knot> newKnots) {
        knots.clear();
        if (newKnots != null) {
            knots.addAll(newKnots);
        }
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        if (position == 0) return TYPE_HEADER;
        if (position == knots.size() + 1) return TYPE_FOOTER;
        return TYPE_KNOT;
    }

    @Override
    public int getItemCount() {
        return knots.size() + 2;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == TYPE_HEADER) {
            ItemTimelineHeaderBinding binding = ItemTimelineHeaderBinding.inflate(inflater, parent, false);
            return new HeaderViewHolder(binding);
        } else if (viewType == TYPE_FOOTER) {
            ItemTimelineFooterBinding binding = ItemTimelineFooterBinding.inflate(inflater, parent, false);
            return new FooterViewHolder(binding);
        } else {
            ItemTimelineKnotBinding binding = ItemTimelineKnotBinding.inflate(inflater, parent, false);
            return new KnotViewHolder(binding);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof HeaderViewHolder) {
            ((HeaderViewHolder) holder).bind(knots);
        } else if (holder instanceof FooterViewHolder) {
            ((FooterViewHolder) holder).bind();
        }
        else if (holder instanceof KnotViewHolder) {
            // Vị trí thực tế trong danh sách knots bị lệch 1 đơn vị do Header
            Knot knot = knots.get(position - 1);
            ((KnotViewHolder) holder).bind(knot, position - 1);
        }
    }

    // ================= HOLDER CHO HEADER =================
    class HeaderViewHolder extends RecyclerView.ViewHolder {
        private final ItemTimelineHeaderBinding binding;

        HeaderViewHolder(ItemTimelineHeaderBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(List<Knot> items) {
            int count = items.size();
            binding.tvKnotCount.setText(count + " Knot(s)");

            if (count > 0) {
                binding.tvStartDate.setText(dateFormat.format(new Date(items.get(0).getMemoryDate())));
                binding.tvEndDate.setText(dateFormat.format(new Date(items.get(count - 1).getMemoryDate())));
            } else {
                binding.tvStartDate.setText("--/--/----");
                binding.tvEndDate.setText("--/--/----");
            }
        }
    }

    // ================== HOLDER CHO FOOTER =================
    class FooterViewHolder extends RecyclerView.ViewHolder {
        private final ItemTimelineFooterBinding binding;

        FooterViewHolder(ItemTimelineFooterBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind() {
            binding.btnAddKnot.setOnClickListener(v -> {
                if (onAddKnotClickListener != null) {
                    onAddKnotClickListener.onAddKnotClick();
                }
            });
        }
    }

    // ================= HOLDER CHO TỪNG KNOT =================
    class KnotViewHolder extends RecyclerView.ViewHolder {
        private final ItemTimelineKnotBinding binding;

        KnotViewHolder(ItemTimelineKnotBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(Knot knot, int knotIndex) {
            String formattedDate = dateFormat.format(new Date(knot.getMemoryDate()));
            boolean isLeft = (knotIndex % 2 == 0); // Vị trí chẵn nằm bên Trái, lẻ nằm bên Phải

            if (isLeft) {
                // Hiển thị nhánh Trái, ẩn nhánh Phải
                binding.polaroidLeft.setVisibility(View.VISIBLE);
                binding.layoutTextRight.setVisibility(View.VISIBLE);
                binding.polaroidRight.setVisibility(View.GONE);
                binding.layoutTextLeft.setVisibility(View.GONE);

                binding.tvDateRight.setText(formattedDate);
                binding.tvDescRight.setText(knot.getDescription());

                loadImage(knot, binding.polaroidLeft);
            } else {
                // Hiển thị nhánh Phải, ẩn nhánh Trái
                binding.polaroidLeft.setVisibility(View.GONE);
                binding.layoutTextRight.setVisibility(View.GONE);
                binding.polaroidRight.setVisibility(View.VISIBLE);
                binding.layoutTextLeft.setVisibility(View.VISIBLE);

                binding.tvDateLeft.setText(formattedDate);
                binding.tvDescLeft.setText(knot.getDescription());

                loadImage(knot, binding.polaroidRight);
            }
        }

        private void loadImage(Knot knot, com.taile.knot.ui.custom.PolaroidView polaroidView) {
            if (knot.getImageResId() != 0) {
                polaroidView.setImageResource(knot.getImageResId());
            } else if (knot.getImageUrl() != null && !knot.getImageUrl().isEmpty()) {
                Glide.with(itemView.getContext())
                        .load(knot.getImageUrl())
                        .thumbnail(0.1f)
                        .into(polaroidView);
            }
        }
    }
}