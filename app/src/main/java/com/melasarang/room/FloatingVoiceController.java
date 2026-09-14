package com.melasarang.room;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

/** A draggable, edge-snapping, circular hologram controller that stays above the WebView. */
public final class FloatingVoiceController extends FrameLayout {

    public interface Listener {
        void onMicTap();
        void onBack();
        void onHome();
        void onForward();
    }

    private static final String PREFS = "sarangbang_voice_overlay";
    private static final String PREF_X = "center_x_fraction";
    private static final String PREF_Y = "center_y_fraction";
    private static final int COLLAPSED_DP = 72;
    private static final int EXPANDED_WIDTH_DP = 196;
    private static final int EXPANDED_HEIGHT_DP = 142;
    private static final int ORB_DP = 72;

    private final SharedPreferences preferences;
    private final HologramOrbView orb;
    private final TextView back;
    private final TextView home;
    private final TextView forward;
    private final int touchSlop;
    private Listener listener;
    private boolean expanded;
    private boolean dragging;
    private float downRawX;
    private float downRawY;
    private float downTranslationX;
    private float downTranslationY;

    public FloatingVoiceController(Context context) {
        super(context);
        preferences = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        touchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        setClipChildren(false);
        setClipToPadding(false);
        setElevation(dp(18));

        orb = new HologramOrbView(context);
        orb.setContentDescription("사랑방 음성 마이크. 누르면 메뉴가 열리고, 길게 끌면 이동합니다.");
        addView(orb, new LayoutParams(dp(ORB_DP), dp(ORB_DP)));

        back = actionButton("이전", "이전 페이지");
        home = actionButton("홈", "멜라루카 사랑방 홈");
        forward = actionButton("다음", "다음 페이지");
        addView(back, new LayoutParams(dp(52), dp(42)));
        addView(home, new LayoutParams(dp(52), dp(42)));
        addView(forward, new LayoutParams(dp(52), dp(42)));
        setActionVisibility(false);

        back.setOnClickListener(v -> { if (listener != null) listener.onBack(); });
        home.setOnClickListener(v -> { if (listener != null) listener.onHome(); });
        forward.setOnClickListener(v -> { if (listener != null) listener.onForward(); });

        orb.setOnTouchListener((view, event) -> handleOrbTouch(event));
        setLayoutSize(COLLAPSED_DP, COLLAPSED_DP);
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    public void setListening(boolean listening) {
        orb.setListening(listening);
    }

    public void setAudioLevel(float level) {
        orb.setAudioLevel(level);
    }

    public void showController() {
        setVisibility(VISIBLE);
        post(this::restoreAndClampPosition);
    }

    public void hideController() {
        setVisibility(GONE);
    }

    public void collapse() {
        if (expanded) setExpanded(false);
    }

    public void onHostBoundsChanged() {
        post(this::restoreAndClampPosition);
    }

    private boolean handleOrbTouch(MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                getParent().requestDisallowInterceptTouchEvent(true);
                dragging = false;
                downRawX = event.getRawX();
                downRawY = event.getRawY();
                downTranslationX = getTranslationX();
                downTranslationY = getTranslationY();
                orb.setPressed(true);
                return true;
            case MotionEvent.ACTION_MOVE:
                float dx = event.getRawX() - downRawX;
                float dy = event.getRawY() - downRawY;
                if (!dragging && Math.hypot(dx, dy) > touchSlop) dragging = true;
                if (dragging) {
                    setTranslationX(downTranslationX + dx);
                    setTranslationY(downTranslationY + dy);
                    clampToParent(false);
                }
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                getParent().requestDisallowInterceptTouchEvent(false);
                orb.setPressed(false);
                if (dragging) {
                    snapToNearestEdge();
                    savePosition();
                } else if (event.getActionMasked() == MotionEvent.ACTION_UP) {
                    performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
                    setExpanded(!expanded);
                    if (listener != null) listener.onMicTap();
                }
                return true;
            default:
                return false;
        }
    }

    private void setExpanded(boolean value) {
        if (expanded == value) return;
        float centerX = getTranslationX() + orbCenterX();
        float centerY = getTranslationY() + orbCenterY();
        expanded = value;
        setActionVisibility(value);
        setLayoutSize(value ? EXPANDED_WIDTH_DP : COLLAPSED_DP,
            value ? EXPANDED_HEIGHT_DP : COLLAPSED_DP);
        requestLayout();
        post(() -> {
            setTranslationX(centerX - orbCenterX());
            setTranslationY(centerY - orbCenterY());
            clampToParent(false);
        });
    }

    private void setActionVisibility(boolean show) {
        int visibility = show ? VISIBLE : GONE;
        back.setVisibility(visibility);
        home.setVisibility(visibility);
        forward.setVisibility(visibility);
    }

    private void setLayoutSize(int widthDp, int heightDp) {
        ViewGroup.LayoutParams current = getLayoutParams();
        if (current == null) current = new FrameLayout.LayoutParams(dp(widthDp), dp(heightDp));
        current.width = dp(widthDp);
        current.height = dp(heightDp);
        setLayoutParams(current);
    }

    @Override protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);
        setMeasuredDimension(width, height);
        int orbSpec = MeasureSpec.makeMeasureSpec(dp(ORB_DP), MeasureSpec.EXACTLY);
        orb.measure(orbSpec, orbSpec);
        int buttonWidth = MeasureSpec.makeMeasureSpec(dp(52), MeasureSpec.EXACTLY);
        int buttonHeight = MeasureSpec.makeMeasureSpec(dp(42), MeasureSpec.EXACTLY);
        back.measure(buttonWidth, buttonHeight);
        home.measure(buttonWidth, buttonHeight);
        forward.measure(buttonWidth, buttonHeight);
    }

    @Override protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        int orbLeft = expanded ? (getMeasuredWidth() - dp(ORB_DP)) / 2 : 0;
        int orbTop = 0;
        orb.layout(orbLeft, orbTop, orbLeft + dp(ORB_DP), orbTop + dp(ORB_DP));
        if (expanded) {
            int buttonTop = dp(92);
            int gap = dp(6);
            int total = dp(52 * 3) + gap * 2;
            int start = (getMeasuredWidth() - total) / 2;
            back.layout(start, buttonTop, start + dp(52), buttonTop + dp(42));
            home.layout(start + dp(52) + gap, buttonTop, start + dp(104) + gap, buttonTop + dp(42));
            forward.layout(start + dp(104) + gap * 2, buttonTop,
                start + dp(156) + gap * 2, buttonTop + dp(42));
        }
    }

    private TextView actionButton(String label, String description) {
        TextView view = new TextView(getContext());
        view.setText(label);
        view.setTextSize(13);
        view.setTextColor(Color.WHITE);
        view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        view.setGravity(Gravity.CENTER);
        view.setContentDescription(description);
        GradientDrawable background = new GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            new int[]{Color.rgb(17, 73, 135), Color.rgb(2, 19, 49)}
        );
        background.setShape(GradientDrawable.RECTANGLE);
        background.setCornerRadius(dp(21));
        background.setStroke(dp(1), Color.rgb(80, 245, 255));
        view.setBackground(background);
        view.setElevation(dp(8));
        return view;
    }

    private float orbCenterX() {
        return expanded ? getWidth() / 2f : dp(ORB_DP) / 2f;
    }

    private float orbCenterY() {
        return dp(ORB_DP) / 2f;
    }

    private void restoreAndClampPosition() {
        if (!(getParent() instanceof View)) return;
        View parent = (View) getParent();
        if (parent.getWidth() <= 0 || parent.getHeight() <= 0) return;
        float xFraction = preferences.getFloat(PREF_X, 0.90f);
        float yFraction = preferences.getFloat(PREF_Y, 0.72f);
        setTranslationX(xFraction * parent.getWidth() - orbCenterX());
        setTranslationY(yFraction * parent.getHeight() - orbCenterY());
        clampToParent(false);
    }

    private void savePosition() {
        if (!(getParent() instanceof View)) return;
        View parent = (View) getParent();
        if (parent.getWidth() <= 0 || parent.getHeight() <= 0) return;
        float centerX = getTranslationX() + orbCenterX();
        float centerY = getTranslationY() + orbCenterY();
        preferences.edit()
            .putFloat(PREF_X, centerX / parent.getWidth())
            .putFloat(PREF_Y, centerY / parent.getHeight())
            .apply();
    }

    private void snapToNearestEdge() {
        if (!(getParent() instanceof View)) return;
        View parent = (View) getParent();
        int margin = dp(8);
        float currentCenter = getTranslationX() + orbCenterX();
        float targetOrbCenter = currentCenter < parent.getWidth() / 2f
            ? margin + dp(ORB_DP) / 2f
            : parent.getWidth() - margin - dp(ORB_DP) / 2f;
        animate().translationX(targetOrbCenter - orbCenterX()).setDuration(180).withEndAction(() -> {
            clampToParent(false);
            savePosition();
        }).start();
    }

    private void clampToParent(boolean save) {
        if (!(getParent() instanceof View)) return;
        View parent = (View) getParent();
        int margin = dp(8);
        float minX = margin;
        float maxX = Math.max(minX, parent.getWidth() - getWidth() - margin);
        float minY = margin;
        float maxY = Math.max(minY, parent.getHeight() - getHeight() - margin);
        setTranslationX(Math.max(minX, Math.min(maxX, getTranslationX())));
        setTranslationY(Math.max(minY, Math.min(maxY, getTranslationY())));
        if (save) savePosition();
    }

    private int dp(float value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    /** Draws the mic, glow and three rotating 멜라루카 사랑방 satellites with circular geometry. */
    private static final class HologramOrbView extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint line = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final ValueAnimator rotation;
        private float angle;
        private float audioLevel;
        private boolean listening;

        HologramOrbView(Context context) {
            super(context);
            setLayerType(View.LAYER_TYPE_SOFTWARE, null);
            line.setStyle(Paint.Style.STROKE);
            line.setStrokeWidth(dp(1.5f));
            rotation = ValueAnimator.ofFloat(0f, 360f);
            rotation.setDuration(7200);
            rotation.setRepeatCount(ValueAnimator.INFINITE);
            rotation.setInterpolator(input -> input);
            rotation.addUpdateListener(value -> {
                angle = (float) value.getAnimatedValue();
                invalidate();
            });
        }

        @Override protected void onAttachedToWindow() {
            super.onAttachedToWindow();
            rotation.start();
        }

        @Override protected void onDetachedFromWindow() {
            rotation.cancel();
            super.onDetachedFromWindow();
        }

        void setListening(boolean value) {
            listening = value;
            invalidate();
        }

        void setAudioLevel(float value) {
            audioLevel = Math.max(0f, Math.min(1f, value));
            invalidate();
        }

        @Override protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float cx = getWidth() / 2f;
            float cy = getHeight() / 2f;
            float radius = Math.min(getWidth(), getHeight()) * 0.32f;
            float pulse = listening ? dp(2.5f) + audioLevel * dp(3f) : 0f;

            paint.setShader(new RadialGradient(cx, cy, radius + pulse,
                new int[]{Color.rgb(41, 239, 255), Color.rgb(18, 97, 190), Color.rgb(2, 16, 47)},
                new float[]{0f, .48f, 1f}, Shader.TileMode.CLAMP));
            paint.setShadowLayer(dp(8 + audioLevel * 8), 0, 0, Color.rgb(62, 245, 255));
            canvas.drawCircle(cx, cy, radius + pulse, paint);
            paint.clearShadowLayer();
            paint.setShader(null);

            line.setColor(Color.argb(190, 80, 245, 255));
            canvas.drawCircle(cx, cy, radius + dp(7), line);
            line.setColor(Color.argb(90, 100, 176, 255));
            canvas.drawCircle(cx, cy, radius + dp(12), line);

            drawMic(canvas, cx, cy, radius * .72f);

            paint.setShader(null);
            paint.setColor(Color.WHITE);
            paint.setTextAlign(Paint.Align.CENTER);
            paint.setTypeface(Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD));
            paint.setTextSize(dp(6.8f));
            paint.setLetterSpacing(.08f);
            canvas.drawText("사랑방", cx, cy + radius * .88f, paint);

            float orbitRadius = radius + dp(10);
            for (int i = 0; i < 3; i++) {
                double theta = Math.toRadians(angle + i * 120f);
                float sx = cx + (float) Math.cos(theta) * orbitRadius;
                float sy = cy + (float) Math.sin(theta) * orbitRadius;
                paint.setColor(i == 0 ? Color.WHITE : Color.rgb(80, 245, 255));
                paint.setShadowLayer(dp(5), 0, 0, Color.CYAN);
                canvas.drawCircle(sx, sy, dp(i == 0 ? 3.2f : 2.4f), paint);
                paint.clearShadowLayer();
            }
        }

        private void drawMic(Canvas canvas, float cx, float cy, float size) {
            line.setColor(Color.WHITE);
            line.setStrokeWidth(dp(3));
            line.setStrokeCap(Paint.Cap.ROUND);
            line.setStyle(Paint.Style.STROKE);
            float top = cy - size * .65f;
            float bottom = cy + size * .15f;
            canvas.drawRoundRect(cx - size * .25f, top, cx + size * .25f, bottom,
                size * .24f, size * .24f, line);
            Path cup = new Path();
            cup.moveTo(cx - size * .48f, cy);
            cup.quadTo(cx - size * .42f, cy + size * .50f, cx, cy + size * .50f);
            cup.quadTo(cx + size * .42f, cy + size * .50f, cx + size * .48f, cy);
            canvas.drawPath(cup, line);
            canvas.drawLine(cx, cy + size * .50f, cx, cy + size * .78f, line);
            canvas.drawLine(cx - size * .28f, cy + size * .78f, cx + size * .28f, cy + size * .78f, line);
        }

        private float dp(float value) {
            return value * getResources().getDisplayMetrics().density;
        }
    }
}
