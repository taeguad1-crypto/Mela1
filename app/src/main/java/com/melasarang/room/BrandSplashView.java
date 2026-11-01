package com.melasarang.room;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.view.View;

/** Two quiet nature scenes; text stays native and readable at every screen size. */
public final class BrandSplashView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final Bitmap forest;
    private final Bitmap water;
    private int scene;

    public BrandSplashView(Context context) {
        super(context);
        forest = BitmapFactory.decodeResource(getResources(), R.drawable.splash_forest);
        water = BitmapFactory.decodeResource(getResources(), R.drawable.splash_water);
        setContentDescription("멜라루카 사랑방 · 숲과 맑은 물 시작 화면");
    }

    public void showWaterScene() {
        scene = 1;
        invalidate();
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float w = getWidth(), h = getHeight();
        if (w <= 0 || h <= 0) return;
        Bitmap photo = scene == 0 ? forest : water;
        canvas.drawColor(Color.rgb(15, 59, 43));
        if (photo != null) {
            float scale = Math.max(w / photo.getWidth(), h / photo.getHeight());
            float pw = photo.getWidth() * scale, ph = photo.getHeight() * scale;
            paint.setShader(null);
            canvas.drawBitmap(photo, null, new RectF((w-pw)/2, (h-ph)/2, (w+pw)/2, (h+ph)/2), paint);
        }
        paint.setShader(new LinearGradient(0, h*.35f, 0, h,
            new int[]{Color.TRANSPARENT, Color.argb(230, 4, 35, 25)}, null, Shader.TileMode.CLAMP));
        canvas.drawRect(0, 0, w, h, paint);
        paint.setShader(null);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setColor(Color.rgb(229, 240, 201));
        paint.setTypeface(Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL));
        paint.setTextSize(Math.min(w*.03f, dp(14)));
        canvas.drawText("MELALEUCA · SARANGBANG", w/2, h*.715f, paint);
        paint.setColor(Color.WHITE);
        paint.setTypeface(Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD));
        paint.setTextSize(Math.min(w*.087f, dp(38)));
        canvas.drawText("멜라루카 사랑방", w/2, h*.78f, paint);
        paint.setTypeface(Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL));
        paint.setTextSize(Math.min(w*.039f, dp(18)));
        canvas.drawText(scene == 0 ? "자연과 함께하는 건강한 일상" : "더 나은 생활을 함께 나누는 곳", w/2, h*.825f, paint);
        paint.setColor(Color.argb(220, 255, 255, 255));
        canvas.drawRoundRect(w*.42f, h*.9f, w*.58f, h*.9f+dp(2), dp(1), dp(1), paint);
        paint.setColor(Color.rgb(219, 220, 163));
        float left = scene == 0 ? w*.42f : w*.50f;
        canvas.drawRoundRect(left, h*.9f, left+w*.08f, h*.9f+dp(2), dp(1), dp(1), paint);
    }

    private float dp(float value) { return value * getResources().getDisplayMetrics().density; }
}
