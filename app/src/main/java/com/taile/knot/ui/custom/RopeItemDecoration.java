package com.taile.knot.ui.custom;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class RopeItemDecoration extends RecyclerView.ItemDecoration {

    private final Paint ropePaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    public RopeItemDecoration() {
        ropePaint.setColor(Color.parseColor("#8A674A")); // Màu nâu dây thừng
        ropePaint.setStrokeWidth(6f); // Độ dày sợi dây (~2dp-3dp)
        ropePaint.setStyle(Paint.Style.STROKE);
        ropePaint.setStrokeCap(Paint.Cap.ROUND);
    }

    @Override
    public void onDraw(@NonNull Canvas canvas, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
        // Tọa độ X nằm chính giữa RecyclerView
        float centerX = parent.getWidth() / 2.0f;

        // Vẽ sợi dây liên tục từ đỉnh xuống đáy của RecyclerView
        float top = parent.getPaddingTop();
        float bottom = parent.getHeight() - parent.getPaddingBottom();

        canvas.drawLine(centerX, top, centerX, bottom, ropePaint);
    }
}