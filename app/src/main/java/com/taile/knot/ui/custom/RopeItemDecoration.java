package com.taile.knot.ui.custom;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.taile.knot.R;

public class RopeItemDecoration extends RecyclerView.ItemDecoration {

    private final Paint ropePaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    public RopeItemDecoration() {
        ropePaint.setColor(Color.parseColor("#8A674A"));
        ropePaint.setStrokeWidth(6f);
        ropePaint.setStyle(Paint.Style.STROKE);
        ropePaint.setStrokeCap(Paint.Cap.ROUND);
    }

    @Override
    public void onDraw(@NonNull Canvas canvas, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
        float centerX = parent.getWidth() / 2.0f;
        float top = parent.getPaddingTop();
        float bottom = parent.getHeight() - parent.getPaddingBottom();

        // Kiểm tra xem Footer có đang hiển thị trên màn hình không
        for (int i = 0; i < parent.getChildCount(); i++) {
            View child = parent.getChildAt(i);
            int adapterPos = parent.getChildAdapterPosition(child);

            // Nếu là item cuối cùng (Footer)
            if (parent.getAdapter() != null && adapterPos == parent.getAdapter().getItemCount() - 1) {
                View node = child.findViewById(R.id.knotNode);
                if (node != null) {
                    bottom = child.getY() + node.getY() + (node.getHeight() / 2.0f);
                }
                break;
            }
        }

        canvas.drawLine(centerX, top, centerX, bottom, ropePaint);
    }
}