package com.taile.knot.ui.custom;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.core.content.ContextCompat;

import com.taile.knot.R;

public class PolaroidView extends AppCompatImageView {

    private final Paint framePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint strokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF frameRect = new RectF();

    private Drawable tapeDrawable;
    private boolean showTape = true;
    private int tapeWidth;
    private int tapeHeight;

    // Khoảng đệm cố định viền Polaroid
    private int sidePadding;
    private int topPadding;
    private int bottomPadding;

    public PolaroidView(@NonNull Context context) {
        this(context, null);
    }

    public PolaroidView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PolaroidView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    private void init(Context context, @Nullable AttributeSet attrs) {
        // 1. Khoảng đệm viền
        sidePadding = dpToPx(6);     // Viền trái/phải: 6dp
        topPadding = dpToPx(14);     // Viền trên: 14dp (chừa chỗ dán Tape)
        bottomPadding = dpToPx(28);  // Cằm dưới: 28dp (cằm Polaroid)

        // 2. Cọ vẽ khung
        framePaint.setColor(Color.WHITE);
        framePaint.setStyle(Paint.Style.FILL);

        strokePaint.setColor(Color.parseColor("#E8DFD8"));
        strokePaint.setStyle(Paint.Style.STROKE);
        strokePaint.setStrokeWidth(dpToPx(1));

        setElevation(dpToPx(4));

        // 3. Khởi tạo Băng dính
        tapeDrawable = ContextCompat.getDrawable(context, R.drawable.bg_washi_tape);
        tapeWidth = dpToPx(44);
        tapeHeight = dpToPx(14);

        if (attrs != null) {
            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.PolaroidView);

            Drawable src = a.getDrawable(R.styleable.PolaroidView_polaroidSrc);
            if (src != null) {
                setImageDrawable(src);
            }

            int direction = a.getInt(R.styleable.PolaroidView_tiltDirection, 0);
            float angle = a.getFloat(R.styleable.PolaroidView_tiltAngle, 2.5f);
            setTilt(direction, angle);

            showTape = a.getBoolean(R.styleable.PolaroidView_showTape, true);

            a.recycle();
        }
    }

    // Tự động đo lại kích thước khi Glide nạp xong ảnh mới
    @Override
    public void setImageDrawable(@Nullable Drawable drawable) {
        super.setImageDrawable(drawable);
        requestLayout();
        invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        Drawable drawable = getDrawable();

        if (drawable == null || drawable.getIntrinsicWidth() <= 0 || drawable.getIntrinsicHeight() <= 0) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            return;
        }

        int dWidth = drawable.getIntrinsicWidth();
        int dHeight = drawable.getIntrinsicHeight();

        // 1. Lấy chiều rộng được chỉ định trên Layout XML
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);

        int measuredWidth;
        if (widthMode == MeasureSpec.EXACTLY) {
            measuredWidth = widthSize;
        } else if (widthMode == MeasureSpec.AT_MOST) {
            measuredWidth = Math.min(widthSize, dWidth + (sidePadding * 2));
        } else {
            measuredWidth = dWidth + (sidePadding * 2);
        }

        // 2. Tính chiều cao vùng ảnh chuẩn xác theo Aspect Ratio
        int photoWidth = Math.max(0, measuredWidth - (sidePadding * 2));
        int photoHeight = Math.round((float) photoWidth * dHeight / dWidth);

        // 3. Chiều cao tổng của cả khung Polaroid
        int measuredHeight = photoHeight + topPadding + bottomPadding;

        setMeasuredDimension(measuredWidth, measuredHeight);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int w = getWidth();
        int h = getHeight();
        if (w == 0 || h == 0) return;

        frameRect.set(0, 0, w, h);

        // 1. Vẽ nền trắng khung Polaroid
        canvas.drawRoundRect(frameRect, dpToPx(4), dpToPx(4), framePaint);

        // 2. Vẽ viền mỏng mép giấy
        canvas.drawRoundRect(frameRect, dpToPx(4), dpToPx(4), strokePaint);

        // 3. Tự vẽ ảnh chuẩn theo tỉ lệ (Không dùng super.onDraw để chống méo hình)
        Drawable drawable = getDrawable();
        if (drawable != null) {
            int photoLeft = sidePadding;
            int photoTop = topPadding;
            int photoRight = w - sidePadding;
            int photoBottom = h - bottomPadding;

            drawable.setBounds(photoLeft, photoTop, photoRight, photoBottom);
            drawable.draw(canvas);
        }

        // 4. Vẽ miếng dán Washi Tape lên mép trên
        if (showTape && tapeDrawable != null) {
            int centerX = w / 2;
            int centerY = tapeHeight / 2;

            canvas.save();
            canvas.rotate(-2.0f, centerX, centerY);

            int left = centerX - (tapeWidth / 2);
            int top = 0;
            tapeDrawable.setBounds(left, top, left + tapeWidth, top + tapeHeight);
            tapeDrawable.draw(canvas);

            canvas.restore();
        }
    }

    public void setTilt(int direction, float angle) {
        if (direction == 1) {
            setRotation(-Math.abs(angle));
        } else if (direction == 2) {
            setRotation(Math.abs(angle));
        } else {
            setRotation(0f);
        }
    }

    public void setShowTape(boolean showTape) {
        this.showTape = showTape;
        invalidate();
    }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density + 0.5f);
    }
}