package liege.counter;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

import java.util.List;
import java.util.Random;

/**
 * LuckyWheelView — a custom View that draws and animates a spinning lucky wheel.
 *
 * Segments are drawn proportionally based on each item's weight.
 * A triangle pointer at the top indicates which segment is selected when the wheel stops.
 */
public class LuckyWheelView extends View {

    private List<ItemManager.WheelItem> items;
    private float[] segmentAngles;   // sweep angle for each segment
    private float[] segmentStarts;   // cumulative start angle for each segment
    private float totalWeight;

    private float currentAngle = 0f;
    private ValueAnimator spinAnimator;
    private OnSpinCompleteListener listener;
    private final Random random = new Random();

    private final Paint segmentPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pointerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint centerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint dotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint glowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF ovalRect = new RectF();

    public interface OnSpinCompleteListener {
        void onSpinComplete(int selectedIndex);
    }

    public LuckyWheelView(Context context) {
        super(context);
        init();
    }

    public LuckyWheelView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public LuckyWheelView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(28f);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setFakeBoldText(true);

        borderPaint.setColor(0x44000000);
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(2f);

        pointerPaint.setColor(Color.WHITE);
        pointerPaint.setStyle(Paint.Style.FILL);

        centerPaint.setColor(0xFF1E1E2E);
        centerPaint.setStyle(Paint.Style.FILL);

        dotPaint.setStyle(Paint.Style.FILL);

        glowPaint.setStyle(Paint.Style.STROKE);
        glowPaint.setStrokeWidth(8f);
        glowPaint.setColor(0x557C4DFF);
    }

    public void setItems(List<ItemManager.WheelItem> items) {
        this.items = items;
        computeSegmentAngles();
        invalidate();
    }

    /** Pre-compute segment angles based on item weights. */
    private void computeSegmentAngles() {
        if (items == null || items.isEmpty()) return;
        int n = items.size();
        segmentAngles = new float[n];
        segmentStarts = new float[n];
        totalWeight = 0;
        for (ItemManager.WheelItem item : items) {
            totalWeight += (float) item.weight;
        }
        float cumulative = 0;
        for (int i = 0; i < n; i++) {
            segmentStarts[i] = cumulative;
            segmentAngles[i] = ((float) items.get(i).weight / totalWeight) * 360f;
            cumulative += segmentAngles[i];
        }
    }

    public void setOnSpinCompleteListener(OnSpinCompleteListener listener) {
        this.listener = listener;
    }

    /**
     * Starts the spin animation. The targetIndex is the pre-determined winning segment.
     */
    public void spin(int targetIndex) {
        if (items == null || items.isEmpty() || segmentAngles == null) return;

        // Calculate the target angle so the pointer (top, at 270° in canvas coordinates)
        // lands in the middle of the target segment.
        float targetMid = segmentStarts[targetIndex] + segmentAngles[targetIndex] / 2f;
        float landingAngle = ((270f - targetMid) % 360f + 360f) % 360f;

        // Add multiple full rotations for visual effect (5-8 spins)
        int extraSpins = 5 + random.nextInt(3);
        float totalRotation = extraSpins * 360f + landingAngle;

        // Ensure we rotate in positive direction from current angle
        float startAngle = currentAngle % 360f;
        float endAngle = startAngle + totalRotation;

        spinAnimator = ValueAnimator.ofFloat(startAngle, endAngle);
        spinAnimator.setDuration(4000 + (long) (random.nextInt(1000))); // 4-5 seconds
        spinAnimator.setInterpolator(new DecelerateInterpolator(2.5f));
        spinAnimator.addUpdateListener(animation -> {
            currentAngle = (float) animation.getAnimatedValue();
            invalidate();
        });
        spinAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if (listener != null) {
                    listener.onSpinComplete(targetIndex);
                }
            }
        });
        spinAnimator.start();
    }

    public boolean isSpinning() {
        return spinAnimator != null && spinAnimator.isRunning();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (items == null || items.isEmpty() || segmentAngles == null) return;

        int width = getWidth();
        int height = getHeight();
        float cx = width / 2f;
        float cy = height / 2f;
        float radius = Math.min(cx, cy) - 36f;

        ovalRect.set(cx - radius, cy - radius, cx + radius, cy + radius);

        int segmentCount = items.size();

        // Draw outer glow ring
        canvas.drawCircle(cx, cy, radius + 4f, glowPaint);

        canvas.save();
        canvas.rotate(currentAngle, cx, cy);

        // Draw segments proportionally
        for (int i = 0; i < segmentCount; i++) {
            float startAngle = segmentStarts[i];
            float sweepAngle = segmentAngles[i];

            // Segment fill — alternate slightly darker for adjacent segments
            int baseColor = items.get(i).color;
            segmentPaint.setColor(baseColor);
            segmentPaint.setStyle(Paint.Style.FILL);
            canvas.drawArc(ovalRect, startAngle, sweepAngle, true, segmentPaint);

            // Subtle inner highlight on top half of segment
            int highlightColor = blendColor(baseColor, 0x22FFFFFF);
            segmentPaint.setColor(highlightColor);
            RectF innerRect = new RectF(
                    cx - radius * 0.98f, cy - radius * 0.98f,
                    cx + radius * 0.98f, cy + radius * 0.98f);
            canvas.drawArc(innerRect, startAngle, sweepAngle, true, segmentPaint);

            // Segment border line
            canvas.drawArc(ovalRect, startAngle, sweepAngle, true, borderPaint);

            // Draw emoji in the middle of each segment (only if segment is large enough)
            if (sweepAngle > 8f) {
                float midAngle = (float) Math.toRadians(startAngle + sweepAngle / 2f);
                float textRadius = radius * 0.65f;
                float tx = cx + (float) (textRadius * Math.cos(midAngle));
                float ty = cy + (float) (textRadius * Math.sin(midAngle));

                float emojiSize = Math.min(28f, sweepAngle * 0.8f);
                textPaint.setTextSize(Math.max(14f, emojiSize));

                canvas.save();
                canvas.rotate(startAngle + sweepAngle / 2f, tx, ty);
                canvas.drawText(items.get(i).emoji, tx, ty + emojiSize * 0.35f, textPaint);
                canvas.restore();
            }
        }

        canvas.restore();

        // Draw decorative dots around the outer ring
        drawDecorationDots(canvas, cx, cy, radius);

        // Draw outer ring
        Paint outerRing = new Paint(Paint.ANTI_ALIAS_FLAG);
        outerRing.setColor(0xFF9C6CFF);
        outerRing.setStyle(Paint.Style.STROKE);
        outerRing.setStrokeWidth(5f);
        canvas.drawCircle(cx, cy, radius, outerRing);

        // Draw second outer ring (double border)
        Paint outerRing2 = new Paint(Paint.ANTI_ALIAS_FLAG);
        outerRing2.setColor(0xFF6236FF);
        outerRing2.setStyle(Paint.Style.STROKE);
        outerRing2.setStrokeWidth(3f);
        canvas.drawCircle(cx, cy, radius + 6f, outerRing2);

        // Draw center circle with gradient-like effect
        float centerRadius = radius * 0.14f;
        centerPaint.setColor(0xFF1E1E2E);
        canvas.drawCircle(cx, cy, centerRadius, centerPaint);

        // Center highlight ring
        Paint centerHighlight = new Paint(Paint.ANTI_ALIAS_FLAG);
        centerHighlight.setColor(0xFF9C6CFF);
        centerHighlight.setStyle(Paint.Style.STROKE);
        centerHighlight.setStrokeWidth(4f);
        canvas.drawCircle(cx, cy, centerRadius, centerHighlight);

        // Center inner dot
        Paint centerDot = new Paint(Paint.ANTI_ALIAS_FLAG);
        centerDot.setColor(0xFFB388FF);
        centerDot.setStyle(Paint.Style.FILL);
        canvas.drawCircle(cx, cy, centerRadius * 0.35f, centerDot);

        // Draw pointer triangle at the top
        drawPointer(canvas, cx, cy - radius - 8f);
    }

    /** Draw decorative dots/bulbs around the wheel perimeter. */
    private void drawDecorationDots(Canvas canvas, float cx, float cy, float radius) {
        int dotCount = 24;
        float dotRadius = 4f;
        float dotRingRadius = radius + 14f;
        for (int i = 0; i < dotCount; i++) {
            float angle = (float) Math.toRadians((360f / dotCount) * i);
            float dx = cx + (float) (dotRingRadius * Math.cos(angle));
            float dy = cy + (float) (dotRingRadius * Math.sin(angle));
            dotPaint.setColor(i % 2 == 0 ? 0xFFFFD600 : 0xFFB388FF);
            canvas.drawCircle(dx, dy, dotRadius, dotPaint);
        }
    }

    private void drawPointer(Canvas canvas, float cx, float tipY) {
        Path pointer = new Path();
        float pointerSize = 26f;
        pointer.moveTo(cx, tipY + 4f);                                    // tip (slightly into wheel)
        pointer.lineTo(cx - pointerSize, tipY - pointerSize * 1.6f);      // left
        pointer.lineTo(cx + pointerSize, tipY - pointerSize * 1.6f);      // right
        pointer.close();

        // Pointer shadow
        Paint shadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        shadowPaint.setColor(0x44000000);
        shadowPaint.setStyle(Paint.Style.FILL);
        Path shadowPointer = new Path();
        shadowPointer.moveTo(cx + 2f, tipY + 6f);
        shadowPointer.lineTo(cx - pointerSize + 2f, tipY - pointerSize * 1.6f + 2f);
        shadowPointer.lineTo(cx + pointerSize + 2f, tipY - pointerSize * 1.6f + 2f);
        shadowPointer.close();
        canvas.drawPath(shadowPointer, shadowPaint);

        // Main pointer
        pointerPaint.setColor(0xFFFFD600);
        pointerPaint.setStyle(Paint.Style.FILL);
        canvas.drawPath(pointer, pointerPaint);

        // Pointer border
        Paint pointerBorder = new Paint(Paint.ANTI_ALIAS_FLAG);
        pointerBorder.setColor(0xFFFF8F00);
        pointerBorder.setStyle(Paint.Style.STROKE);
        pointerBorder.setStrokeWidth(2.5f);
        canvas.drawPath(pointer, pointerBorder);
    }

    /** Blend two ARGB colors together. */
    private int blendColor(int base, int overlay) {
        int aO = (overlay >> 24) & 0xFF;
        int rO = (overlay >> 16) & 0xFF;
        int gO = (overlay >> 8) & 0xFF;
        int bO = overlay & 0xFF;

        int rB = (base >> 16) & 0xFF;
        int gB = (base >> 8) & 0xFF;
        int bB = base & 0xFF;

        float alpha = aO / 255f;
        int r = (int) (rB * (1 - alpha) + rO * alpha);
        int g = (int) (gB * (1 - alpha) + gO * alpha);
        int b = (int) (bB * (1 - alpha) + bO * alpha);

        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int size = Math.min(
                MeasureSpec.getSize(widthMeasureSpec),
                MeasureSpec.getSize(heightMeasureSpec)
        );
        // Ensure minimum size
        size = Math.max(size, 300);
        setMeasuredDimension(size, size);
    }
}
