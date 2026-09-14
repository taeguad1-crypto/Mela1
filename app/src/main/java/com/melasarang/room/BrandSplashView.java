package com.melasarang.room;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.view.View;

/** Exact, code-rendered 멜라루카 사랑방 splash branding so the brand spelling cannot be altered. */
public final class BrandSplashView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint line = new Paint(Paint.ANTI_ALIAS_FLAG);

    public BrandSplashView(Context context) {
        super(context);
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        setContentDescription("멜라루카 사랑방 시작 화면");
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float width = getWidth();
        float height = getHeight();
        float cx = width / 2f;
        float cy = height * .43f;

        paint.setShader(new LinearGradient(0, 0, width, height,
            new int[]{Color.rgb(236, 255, 255), Color.rgb(72, 212, 224), Color.rgb(22, 82, 164)},
            new float[]{0f, .48f, 1f}, Shader.TileMode.CLAMP));
        canvas.drawRect(0, 0, width, height, paint);
        paint.setShader(null);

        float glowRadius = Math.min(width, height) * .18f;
        paint.setShader(new RadialGradient(cx, cy, glowRadius * 1.6f,
            new int[]{Color.argb(240, 255, 255, 255), Color.argb(155, 80, 245, 255), Color.TRANSPARENT},
            new float[]{0f, .48f, 1f}, Shader.TileMode.CLAMP));
        canvas.drawCircle(cx, cy, glowRadius * 1.6f, paint);
        paint.setShader(null);

        line.setStyle(Paint.Style.STROKE);
        line.setStrokeWidth(dp(2));
        line.setColor(Color.argb(190, 255, 255, 255));
        canvas.drawCircle(cx, cy, glowRadius, line);
        line.setColor(Color.argb(125, 80, 245, 255));
        canvas.drawCircle(cx, cy, glowRadius * 1.22f, line);
        canvas.drawCircle(cx, cy, glowRadius * 1.46f, line);

        drawSarangbangMark(canvas, cx, cy, glowRadius * .58f);

        paint.setShader(null);
        paint.setColor(Color.WHITE);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTypeface(Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD));
        paint.setTextSize(Math.min(width * .095f, dp(42)));
        paint.setLetterSpacing(.02f);
        paint.setShadowLayer(dp(10), 0, dp(2), Color.rgb(11, 99, 190));
        canvas.drawText("멜라루카 사랑방", cx, cy + glowRadius * 1.95f, paint);
        paint.clearShadowLayer();

        paint.setTypeface(Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL));
        paint.setTextSize(Math.min(width * .047f, dp(18)));
        paint.setLetterSpacing(.04f);
        paint.setColor(Color.argb(235, 255, 255, 255));
        canvas.drawText("사진으로 보고, 말로 찾는 사업자 사랑방", cx, cy + glowRadius * 2.38f, paint);
    }

    private void drawSarangbangMark(Canvas canvas, float cx, float cy, float size) {
        line.setStrokeCap(Paint.Cap.ROUND);
        line.setStrokeJoin(Paint.Join.ROUND);
        line.setStyle(Paint.Style.STROKE);
        line.setStrokeWidth(size * .24f);
        line.setColor(Color.WHITE);
        line.setShadowLayer(dp(9), 0, 0, Color.rgb(50, 240, 255));
        Path mark = new Path();
        mark.moveTo(cx - size * .72f, cy - size * .25f);
        mark.cubicTo(cx - size * .55f, cy - size * .86f, cx, cy - size * .52f, cx, cy - size * .10f);
        mark.cubicTo(cx, cy - size * .52f, cx + size * .55f, cy - size * .86f, cx + size * .72f, cy - size * .25f);
        mark.cubicTo(cx + size * .88f, cy + size * .35f, cx + size * .24f, cy + size * .72f, cx, cy + size * .92f);
        mark.cubicTo(cx - size * .24f, cy + size * .72f, cx - size * .88f, cy + size * .35f, cx - size * .72f, cy - size * .25f);
        canvas.drawPath(mark, line);
        line.clearShadowLayer();
    }

    private float dp(float value) {
        return value * getResources().getDisplayMetrics().density;
    }
}
