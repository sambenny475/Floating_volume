package com.floatingvolume;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.media.AudioManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;

public class FloatingVolumeService extends Service {

    private WindowManager windowManager;
    private KnobView knobView;
    private WindowManager.LayoutParams params;
    private AudioManager audioManager;

    private static final long AUTO_HIDE_DELAY = 3000; // 3 seconds
    private Handler handler = new Handler(Looper.getMainLooper());
    private Runnable hideRunnable;

    @Override
    public IBinder onBind(Intent intent) { return null; }

    @Override
    public void onCreate() {
        super.onCreate();

        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

        knobView = new KnobView(this);

        params = new WindowManager.LayoutParams(
                130, 130,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                android.graphics.PixelFormat.TRANSLUCENT
        );
        params.gravity = Gravity.TOP | Gravity.START;
        params.x = 30;
        params.y = 300;

        if (windowManager != null) {
            windowManager.addView(knobView, params);
        }

        // Start hidden — show a small dot
        knobView.setExpanded(false);

        hideRunnable = () -> knobView.setExpanded(false);

        setupTouchAndDrag();
    }

    private void setupTouchAndDrag() {
        final int[] lastX = {0};
        final int[] lastY = {0};
        final float[] startTouchX = {0};
        final float[] startTouchY = {0};
        final int[] startParamsX = {0};
        final int[] startParamsY = {0};
        final boolean[] isDragging = {false};

        knobView.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    startTouchX[0] = event.getRawX();
                    startTouchY[0] = event.getRawY();
                    startParamsX[0] = params.x;
                    startParamsY[0] = params.y;
                    isDragging[0] = false;
                    break;

                case MotionEvent.ACTION_MOVE:
                    float dx = event.getRawX() - startTouchX[0];
                    float dy = event.getRawY() - startTouchY[0];
                    if (Math.abs(dx) > 8 || Math.abs(dy) > 8) {
                        isDragging[0] = true;
                    }
                    if (isDragging[0] && windowManager != null) {
                        params.x = startParamsX[0] + (int) dx;
                        params.y = startParamsY[0] + (int) dy;
                        windowManager.updateViewLayout(knobView, params);
                    }
                    break;

                case MotionEvent.ACTION_UP:
                    if (!isDragging[0]) {
                        handleTap(event);
                    }
                    break;
            }
            return true;
        });
    }

    private void handleTap(MotionEvent event) {
        if (!knobView.isExpanded()) {
            // Show knob
            knobView.setExpanded(true);
            resetHideTimer();
        } else {
            // Check if tap is on volume area (top half = up, bottom half = down)
            float tapY = event.getY();
            float midY = knobView.getHeight() / 2f;
            if (tapY < midY) {
                changeVolume(+1);
            } else {
                changeVolume(-1);
            }
            resetHideTimer();
            knobView.invalidate();
        }
    }

    private void changeVolume(int direction) {
        if (audioManager != null) {
            audioManager.adjustStreamVolume(
                    AudioManager.STREAM_MUSIC,
                    direction > 0 ? AudioManager.ADJUST_RAISE : AudioManager.ADJUST_LOWER,
                    0
            );
        }
    }

    private void resetHideTimer() {
        handler.removeCallbacks(hideRunnable);
        handler.postDelayed(hideRunnable, AUTO_HIDE_DELAY);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Clean up handler to prevent memory leaks
        if (handler != null) {
            handler.removeCallbacks(hideRunnable);
        }
        // Safe removal of view with null checks
        if (knobView != null && windowManager != null) {
            try {
                windowManager.removeView(knobView);
            } catch (IllegalArgumentException e) {
                // View already removed or service already destroyed
            }
        }
        knobView = null;
        windowManager = null;
        handler = null;
    }

    // ─── Inner View ────────────────────────────────────────────────
    class KnobView extends View {

        private boolean expanded = false;
        private Paint dotPaint, knobPaint, arcPaint, textPaint, arrowPaint;
        private AudioManager am;

        public KnobView(Context context) {
            super(context);
            am = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
            initPaints();
        }

        private void initPaints() {
            dotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            dotPaint.setColor(Color.parseColor("#BB000000"));

            knobPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            knobPaint.setColor(Color.parseColor("#CC1A1A2E"));

            arcPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            arcPaint.setColor(Color.parseColor("#FF00D4FF"));
            arcPaint.setStyle(Paint.Style.STROKE);
            arcPaint.setStrokeWidth(7f);
            arcPaint.setStrokeCap(Paint.Cap.ROUND);

            textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            textPaint.setColor(Color.WHITE);
            textPaint.setTextSize(22f);
            textPaint.setTextAlign(Paint.Align.CENTER);
            textPaint.setFakeBoldText(true);

            arrowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            arrowPaint.setColor(Color.parseColor("#AAFFFFFF"));
            arrowPaint.setTextSize(18f);
            arrowPaint.setTextAlign(Paint.Align.CENTER);
        }

        public void setExpanded(boolean expand) {
            this.expanded = expand;
            invalidate();
        }

        public boolean isExpanded() { return expanded; }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            int w = getWidth(), h = getHeight();
            float cx = w / 2f, cy = h / 2f;

            if (!expanded) {
                // Small glowing dot
                dotPaint.setColor(Color.parseColor("#BB00D4FF"));
                canvas.drawCircle(cx, cy, 14, dotPaint);
            } else {
                // Dark knob background
                canvas.drawCircle(cx, cy, cx - 4, knobPaint);

                // Volume arc
                if (am != null) {
                    int maxVol = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
                    int curVol = am.getStreamVolume(AudioManager.STREAM_MUSIC);
                    float percent = maxVol > 0 ? (float) curVol / maxVol : 0;
                    float sweep = percent * 270f;
                    RectF oval = new RectF(12, 12, w - 12, h - 12);
                    // Background arc (dim)
                    arcPaint.setColor(Color.parseColor("#44FFFFFF"));
                    canvas.drawArc(oval, 135, 270, false, arcPaint);
                    // Foreground arc (cyan)
                    arcPaint.setColor(Color.parseColor("#FF00D4FF"));
                    canvas.drawArc(oval, 135, sweep, false, arcPaint);

                    // Volume % text
                    int volPercent = (int)(percent * 100);
                    canvas.drawText(volPercent + "%", cx, cy + 8, textPaint);
                }

                // Up/down hint arrows
                arrowPaint.setTextSize(13f);
                canvas.drawText("▲ up", cx, cy - 26, arrowPaint);
                canvas.drawText("▼ dn", cx, cy + 30, arrowPaint);
            }
        }
    }
}
