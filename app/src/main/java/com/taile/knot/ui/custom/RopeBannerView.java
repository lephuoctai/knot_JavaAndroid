package com.taile.knot.ui.custom;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;
import com.taile.knot.R;

/** Reuses the existing vertical rope texture along the entry screen's horizontal rope. */
public class RopeBannerView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private final float density;
    private final BitmapShader shader;
    public RopeBannerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        density = getResources().getDisplayMetrics().density;
        Bitmap texture = BitmapFactory.decodeResource(getResources(), R.drawable.rope);
        shader = new BitmapShader(texture, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT);
        Matrix matrix = new Matrix();
        matrix.setRotate(90);
        float scale = 5 * density / texture.getWidth();
        matrix.postScale(scale, scale);
        shader.setLocalMatrix(matrix);
    }
    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float y = getHeight() * .35f;
        path.reset();
        path.moveTo(0, y);
        path.cubicTo(getWidth() * .3f, y, getWidth() * .4f, y + 14 * density,
                getWidth(), y + 3 * density);
        paint.setShader(shader);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(5 * density);
        canvas.drawPath(path, paint);
        paint.setShader(null);
        paint.setColor(getResources().getColor(R.color.pink300, null));
        paint.setStrokeWidth(3 * density);
        canvas.drawCircle(getWidth() / 2f, y + 6 * density, 7 * density, paint);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(getResources().getColor(R.color.background, null));
        canvas.drawCircle(getWidth() / 2f, y + 6 * density, 5.5f * density, paint);
    }
}
