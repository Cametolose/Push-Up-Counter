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
 * Each segment is colored with the item's color. A triangle pointer at the top
 * indicates which segment is selected when the wheel stops.
 */
public class LuckyWheelView extends View {

    private List<ItemManager.WheelItem> items;
    private float currentAngle = 0f;
    private ValueAnimator spinAnimator;
    private OnSpinCompleteListener listener;
    private final Random random = new Random();

    private final Paint segmentPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pointerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint centerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
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

        borderPaint.setColor(0xFF333333);
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(3f);

        pointerPaint.setColor(Color.WHITE);
        pointerPaint.setStyle(Paint.Style.FILL);

        centerPaint.setColor(0xFF2A2A2A);
        centerPaint.setStyle(Paint.Style.FILL);
    }

    public void setItems(List<ItemManager.WheelItem> items) {
        this.items = items;
        invalidate();
    }

    public void setOnSpinCompleteListener(OnSpinCompleteListener listener) {
        this.listener = listener;
    }

    /**
     * Starts the spin animation. The targetIndex is the pre-determined winning segment.
     */
    public void spin(int targetIndex) {
        if (items == null || items.isEmpty()) return;

        int segmentCount = items.size();
        float segmentAngle = 360f / segmentCount;

        // Calculate the target angle so the pointer (top) lands in the middle of the target segment.
        // The pointer is at the top (270° in standard canvas coords).
        // Segment i starts at i * segmentAngle from the current rotation.
        // We want the middle of segment targetIndex to align with the top.
        float targetMid = targetIndex * segmentAngle + segmentAngle / 2f;
        // We want 360 - targetMid to be the angle at the top (pointer at 0° = top)
        float landingAngle = 360f - targetMid;

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
        if (items == null || items.isEmpty()) return;

        int width = getWidth();
        int height = getHeight();
        float cx = width / 2f;
        float cy = height / 2f;
        float radius = Math.min(cx, cy) - 30f;

        ovalRect.set(cx - radius, cy - radius, cx + radius, cy + radius);

        int segmentCount = items.size();
        float segmentAngle = 360f / segmentCount;

        canvas.save();
        canvas.rotate(currentAngle, cx, cy);

        // Draw segments
        for (int i = 0; i < segmentCount; i++) {
            float startAngle = i * segmentAngle;
            segmentPaint.setColor(items.get(i).color);
            canvas.drawArc(ovalRect, startAngle, segmentAngle, true, segmentPaint);

            // Draw segment border
            canvas.drawArc(ovalRect, startAngle, segmentAngle, true, borderPaint);

            // Draw emoji text in the middle of each segment
            float midAngle = (float) Math.toRadians(startAngle + segmentAngle / 2f);
            float textRadius = radius * 0.65f;
            float tx = cx + (float) (textRadius * Math.cos(midAngle));
            float ty = cy + (float) (textRadius * Math.sin(midAngle));

            canvas.save();
            canvas.rotate(startAngle + segmentAngle / 2f, tx, ty);
            canvas.drawText(items.get(i).emoji, tx, ty + 10, textPaint);
            canvas.restore();
        }

        canvas.restore();

        // Draw center circle
        canvas.drawCircle(cx, cy, radius * 0.12f, centerPaint);
        Paint centerBorder = new Paint(Paint.ANTI_ALIAS_FLAG);
        centerBorder.setColor(0xFF7C4DFF);
        centerBorder.setStyle(Paint.Style.STROKE);
        centerBorder.setStrokeWidth(4f);
        canvas.drawCircle(cx, cy, radius * 0.12f, centerBorder);

        // Draw outer ring
        Paint outerRing = new Paint(Paint.ANTI_ALIAS_FLAG);
        outerRing.setColor(0xFF7C4DFF);
        outerRing.setStyle(Paint.Style.STROKE);
        outerRing.setStrokeWidth(6f);
        canvas.drawCircle(cx, cy, radius, outerRing);

        // Draw pointer triangle at the top
        drawPointer(canvas, cx, cy - radius - 5f);
    }

    private void drawPointer(Canvas canvas, float cx, float tipY) {
        Path pointer = new Path();
        float pointerSize = 24f;
        pointer.moveTo(cx, tipY);                                     // tip
        pointer.lineTo(cx - pointerSize, tipY - pointerSize * 1.8f);  // left
        pointer.lineTo(cx + pointerSize, tipY - pointerSize * 1.8f);  // right
        pointer.close();

        pointerPaint.setColor(0xFFFFD600);
        pointerPaint.setStyle(Paint.Style.FILL);
        canvas.drawPath(pointer, pointerPaint);

        // Pointer border
        Paint pointerBorder = new Paint(Paint.ANTI_ALIAS_FLAG);
        pointerBorder.setColor(0xFF333333);
        pointerBorder.setStyle(Paint.Style.STROKE);
        pointerBorder.setStrokeWidth(2f);
        canvas.drawPath(pointer, pointerBorder);
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
