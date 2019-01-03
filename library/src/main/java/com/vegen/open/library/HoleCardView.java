package com.vegen.open.library;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathEffect;
import android.graphics.PorterDuffColorFilter;
import android.graphics.RectF;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicBlur;
import android.support.annotation.IntDef;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;
import android.widget.FrameLayout;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import static android.graphics.Bitmap.Config.ALPHA_8;
import static android.graphics.Color.BLACK;
import static android.graphics.Color.TRANSPARENT;
import static android.graphics.Paint.ANTI_ALIAS_FLAG;
import static android.graphics.PorterDuff.Mode.SRC_IN;

/**
 * @author Vegen
 * @creation_time 2018/12/25
 * @description 可挖孔、可圆角、可带分割线的卡片 ViewGroup，*** 注意：最多只能有一个直接子布局 ***
 */

public class HoleCardView extends FrameLayout {

    public static final String TAG = HoleCardView.class.getSimpleName();

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({Orientation.HORIZONTAL, Orientation.VERTICAL})
    public @interface Orientation {
        int HORIZONTAL = 0;
        int VERTICAL = 1;
    }

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({DividerType.NORMAL, DividerType.DASH})
    public @interface DividerType {
        int NORMAL = 0;
        int DASH = 1;
    }

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({CornerType.NORMAL, CornerType.ROUNDED})
    public @interface CornerType {
        int NORMAL = 0;
        int ROUNDED = 1;
        int SCALLOP = 2;
    }

    private Paint mBackgroundPaint = new Paint();
    private Paint mBorderPaint = new Paint();
    private Paint mDividerPaint = new Paint();
    private int mOrientation;
    private Path mPath = new Path();
    private boolean mDirty = true;
    private float mDividerStartX, mDividerStartY, mDividerStopX, mDividerStopY;
    private RectF mRect = new RectF();
    private RectF mRoundedCornerArc = new RectF();
    private RectF mScallopCornerArc = new RectF();
    private int mScallopHeight;
    private float mScallopPosition;
    private float mScallopPositionPercent;
    private int mBackgroundColor;
    private boolean mShowBorder;
    private int mBorderWidth;
    private int mBorderColor;
    private boolean mShowDivider;
    private int mScallopRadius;
    private int mDividerDashLength;
    private int mDividerDashGap;
    private int mDividerType;
    private int mDividerWidth;
    private int mDividerColor;
    private int mCornerType;
    private int mCornerRadius;
    private int mDividerPadding;
    private Bitmap mShadow;
    private final Paint mShadowPaint = new Paint(ANTI_ALIAS_FLAG);
    private float mShadowBlurRadius = 0f;

    public HoleCardView(Context context) {
        super(context);
        init(null);
    }

    public HoleCardView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs);
    }

    public HoleCardView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(attrs);
    }

    int absLeft;
    int absTop;
    int absRight;
    int absBottom;

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        //Log.e(TAG, "onMeasure-->width:" + getMeasuredWidth() + "  Height:" + getMeasuredHeight());

        int mWidth = MeasureSpec.getSize(widthMeasureSpec);
        int mWidthMode = MeasureSpec.getMode(widthMeasureSpec);
        int mHeight = MeasureSpec.getSize(heightMeasureSpec);
        int mHeightMode = MeasureSpec.getMode(heightMeasureSpec);

        //Log.e("mShadowBlurRadius", "mShadowBlurRadius:" + mShadowBlurRadius);
        final int count = getChildCount();
        if (count > 1) {
            throw new RuntimeException(TAG + "：最多只能有一个直接子布局，请检查布局");
        }
        if (count == 1) {
            final View child = getChildAt(0);
            if (child.getVisibility() != GONE) {

                int l = 0;
                int t = 0;
                int r = mWidth;//getWidth();
                int b = mHeight;//getHeight();
                int left = (int) (getPaddingLeft() + mShadowBlurRadius);
                int right = (int) (mWidth - getPaddingRight() - mShadowBlurRadius);
                int top = (int) (getPaddingTop() + (mShadowBlurRadius / 2));
                int bottom = (int) (mHeight - getPaddingBottom() - mShadowBlurRadius - (mShadowBlurRadius / 2));

                absLeft = Math.abs(left - l);
                absTop = Math.abs(top - t);
                absRight = Math.abs(right - r);
                absBottom = Math.abs(bottom - b);

                int measureSpecChildWidth = MeasureSpec.makeMeasureSpec(mWidth - absLeft - absRight, mWidthMode);
                int measureSpecChildHeight = MeasureSpec.makeMeasureSpec(mHeight - absTop - absBottom, mHeightMode);

                child.measure(measureSpecChildWidth, measureSpecChildHeight);
            }
        }
    }

    @Override
    protected void onLayout(boolean changed, int left0, int top0, int right0, int bottom0) {
        final int count = getChildCount();
        if (count > 1) {
            throw new RuntimeException(TAG + "：最多只能有一个直接子布局，请检查布局");
        }
        if (count == 1) {
            final View child = getChildAt(0);
            if (child.getVisibility() != GONE) {
                child.layout(absLeft, absTop, getMeasuredWidth() - absRight, getMeasuredHeight() - absBottom);
            }
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (mDirty) {
            doLayout();
        }
        if (mShadowBlurRadius > 0f && !isInEditMode()) {
            canvas.drawBitmap(mShadow, 0, mShadowBlurRadius / 2f, null);
        }

        canvas.clipPath(mPath);

        canvas.drawPath(mPath, mBackgroundPaint);

        if (mShowBorder) {
            canvas.drawPath(mPath, mBorderPaint);
        }
        if (mShowDivider) {
            canvas.drawLine(mDividerStartX, mDividerStartY, mDividerStopX, mDividerStopY, mDividerPaint);
        }
    }

    private void doLayout() {
        float offset;
        float left = getPaddingLeft() + mShadowBlurRadius;
        float right = getWidth() - getPaddingRight() - mShadowBlurRadius;
        float top = getPaddingTop() + (mShadowBlurRadius / 2);
        float bottom = getHeight() - getPaddingBottom() - mShadowBlurRadius - (mShadowBlurRadius / 2);
        //Log.e("doLayout-->", "mShadowBlurRadius:" + mShadowBlurRadius + "  left:" + left + "  right:" + right + "  top:" + top + "  bottom:" + bottom);
        mPath.reset();

        if (mOrientation == Orientation.HORIZONTAL) {
            offset = ((top + bottom) / mScallopPosition) - mScallopRadius;

            if (mCornerType == CornerType.ROUNDED) {
                mPath.arcTo(getTopLeftCornerRoundedArc(top, left), 180.0f, 90.0f, false);
                mPath.lineTo(left + mCornerRadius, top);

                mPath.lineTo(right - mCornerRadius, top);
                mPath.arcTo(getTopRightCornerRoundedArc(top, right), -90.0f, 90.0f, false);

            } else if (mCornerType == CornerType.SCALLOP) {
                mPath.arcTo(getTopLeftCornerScallopArc(top, left), 90.0f, -90.0f, false);
                mPath.lineTo(left + mCornerRadius, top);

                mPath.lineTo(right - mCornerRadius, top);
                mPath.arcTo(getTopRightCornerScallopArc(top, right), 180.0f, -90.0f, false);

            } else {
                mPath.moveTo(left, top);
                mPath.lineTo(right, top);
            }

            mRect.set(right - mScallopRadius, top + offset, right + mScallopRadius, mScallopHeight + offset + top);
            mPath.arcTo(mRect, 270, -180.0f, false);

            if (mCornerType == CornerType.ROUNDED) {

                mPath.arcTo(getBottomRightCornerRoundedArc(bottom, right), 0.0f, 90.0f, false);
                mPath.lineTo(right - mCornerRadius, bottom);

                mPath.lineTo(left + mCornerRadius, bottom);
                mPath.arcTo(getBottomLeftCornerRoundedArc(left, bottom), 90.0f, 90.0f, false);

            } else if (mCornerType == CornerType.SCALLOP) {

                mPath.arcTo(getBottomRightCornerScallopArc(bottom, right), 270.0f, -90.0f, false);
                mPath.lineTo(right - mCornerRadius, bottom);

                mPath.lineTo(left + mCornerRadius, bottom);
                mPath.arcTo(getBottomLeftCornerScallopArc(left, bottom), 0.0f, -90.0f, false);

            } else {
                mPath.lineTo(right, bottom);
                mPath.lineTo(left, bottom);
            }

            mRect.set(left - mScallopRadius, top + offset, left + mScallopRadius, mScallopHeight + offset + top);
            mPath.arcTo(mRect, 90.0f, -180.0f, false);
            mPath.close();

        } else {
            offset = (((right + left) / mScallopPosition) - mScallopRadius);

            if (mCornerType == CornerType.ROUNDED) {
                mPath.arcTo(getTopLeftCornerRoundedArc(top, left), 180.0f, 90.0f, false);
                mPath.lineTo(left + mCornerRadius, top);

            } else if (mCornerType == CornerType.SCALLOP) {

                mPath.arcTo(getTopLeftCornerScallopArc(top, left), 90.0f, -90.0f, false);
                mPath.lineTo(left + mCornerRadius, top);

            } else {
                mPath.moveTo(left, top);
            }

            mRect.set(left + offset, top - mScallopRadius, mScallopHeight + offset + left, top + mScallopRadius);
            mPath.arcTo(mRect, 180.0f, -180.0f, false);

            if (mCornerType == CornerType.ROUNDED) {

                mPath.lineTo(right - mCornerRadius, top);
                mPath.arcTo(getTopRightCornerRoundedArc(top, right), -90.0f, 90.0f, false);

                mPath.arcTo(getBottomRightCornerRoundedArc(bottom, right), 0.0f, 90.0f, false);
                mPath.lineTo(right - mCornerRadius, bottom);

            } else if (mCornerType == CornerType.SCALLOP) {

                mPath.lineTo(right - mCornerRadius, top);
                mPath.arcTo(getTopRightCornerScallopArc(top, right), 180.0f, -90.0f, false);

                mPath.arcTo(getBottomRightCornerScallopArc(bottom, right), 270.0f, -90.0f, false);
                mPath.lineTo(right - mCornerRadius, bottom);

            } else {
                mPath.lineTo(right, top);
                mPath.lineTo(right, bottom);
            }

            mRect.set(left + offset, bottom - mScallopRadius, mScallopHeight + offset + left, bottom + mScallopRadius);
            mPath.arcTo(mRect, 0.0f, -180.0f, false);

            if (mCornerType == CornerType.ROUNDED) {

                mPath.arcTo(getBottomLeftCornerRoundedArc(left, bottom), 90.0f, 90.0f, false);
                mPath.lineTo(left, bottom - mCornerRadius);

            } else if (mCornerType == CornerType.SCALLOP) {

                mPath.arcTo(getBottomLeftCornerScallopArc(left, bottom), 0.0f, -90.0f, false);
                mPath.lineTo(left, bottom - mCornerRadius);

            } else {
                mPath.lineTo(left, bottom);
            }
            mPath.close();
        }

        // divider
        if (mOrientation == Orientation.HORIZONTAL) {
            mDividerStartX = left + mScallopRadius + mDividerPadding;
            mDividerStartY = mScallopRadius + top + offset;
            mDividerStopX = right - mScallopRadius - mDividerPadding;
            mDividerStopY = mScallopRadius + top + offset;
        } else {
            mDividerStartX = mScallopRadius + left + offset;
            mDividerStartY = top + mScallopRadius + mDividerPadding;
            mDividerStopX = mScallopRadius + left + offset;
            mDividerStopY = bottom - mScallopRadius - mDividerPadding;
        }

        generateShadow();
        mDirty = false;
    }

    private void generateShadow() {
        if (isJellyBeanAndAbove() && !isInEditMode()) {
            if (mShadowBlurRadius == 0f) return;

            if (mShadow == null) {
                mShadow = Bitmap.createBitmap(getWidth(), getHeight(), ALPHA_8);
            } else {
                mShadow.eraseColor(TRANSPARENT);
            }
            Canvas c = new Canvas(mShadow);
            c.drawPath(mPath, mShadowPaint);
            if (mShowBorder) {
                c.drawPath(mPath, mShadowPaint);
            }
            RenderScript rs = RenderScript.create(getContext());
            ScriptIntrinsicBlur blur = ScriptIntrinsicBlur.create(rs, Element.U8(rs));
            Allocation input = Allocation.createFromBitmap(rs, mShadow);
            Allocation output = Allocation.createTyped(rs, input.getType());
            // Radius out of range (0 < r <= 25)
            blur.setRadius(mShadowBlurRadius);
            blur.setInput(input);
            blur.forEach(output);
            output.copyTo(mShadow);
            input.destroy();
            output.destroy();
            blur.destroy();
        }
    }

    private void init(AttributeSet attrs) {
        if (attrs != null) {
            TypedArray typedArray = getContext().obtainStyledAttributes(attrs, R.styleable.HoleCardView);
            mOrientation = typedArray.getInt(R.styleable.HoleCardView_holeOrientation, Orientation.HORIZONTAL);
            mBackgroundColor = typedArray.getColor(R.styleable.HoleCardView_holeBackgroundColor, getResources().getColor(android.R.color.white));
            mScallopRadius = typedArray.getDimensionPixelSize(R.styleable.HoleCardView_holeScallopRadius, dpToPx(20f, getContext()));
            mScallopPositionPercent = typedArray.getFloat(R.styleable.HoleCardView_holeScallopPositionPercent, 50);
            mShowBorder = typedArray.getBoolean(R.styleable.HoleCardView_holeShowBorder, false);
            mBorderWidth = typedArray.getDimensionPixelSize(R.styleable.HoleCardView_holeBorderWidth, dpToPx(2f, getContext()));
            mBorderColor = typedArray.getColor(R.styleable.HoleCardView_holeBorderColor, getResources().getColor(android.R.color.black));
            mShowDivider = typedArray.getBoolean(R.styleable.HoleCardView_holeShowDivider, false);
            mDividerType = typedArray.getInt(R.styleable.HoleCardView_holeDividerType, DividerType.DASH);
            mDividerWidth = typedArray.getDimensionPixelSize(R.styleable.HoleCardView_holeDividerWidth, dpToPx(2f, getContext()));
            mDividerColor = typedArray.getColor(R.styleable.HoleCardView_holeDividerColor, getResources().getColor(android.R.color.darker_gray));
            mDividerDashLength = typedArray.getDimensionPixelSize(R.styleable.HoleCardView_holeDividerDashLength, dpToPx(8f, getContext()));
            mDividerDashGap = typedArray.getDimensionPixelSize(R.styleable.HoleCardView_holeDividerDashGap, dpToPx(4f, getContext()));
            mCornerType = typedArray.getInt(R.styleable.HoleCardView_holeCornerType, CornerType.ROUNDED);
            mCornerRadius = typedArray.getDimensionPixelSize(R.styleable.HoleCardView_holeCornerRadius, dpToPx(10f, getContext()));
            mDividerPadding = typedArray.getDimensionPixelSize(R.styleable.HoleCardView_holeDividerPadding, dpToPx(10f, getContext()));
            float elevation = 0f;
            if (typedArray.hasValue(R.styleable.HoleCardView_holeElevation)) {
                elevation = typedArray.getDimension(R.styleable.HoleCardView_holeElevation, elevation);
            } else if (typedArray.hasValue(R.styleable.HoleCardView_android_elevation)) {
                elevation = typedArray.getDimension(R.styleable.HoleCardView_android_elevation, elevation);
            }
            if (elevation > 0f) {
                setShadowBlurRadius(elevation);
            }
            typedArray.recycle();
        }

        mShadowPaint.setColorFilter(new PorterDuffColorFilter(BLACK, SRC_IN));
        mShadowPaint.setAlpha(51); // 20%

        initElements();

        setLayerType(View.LAYER_TYPE_SOFTWARE, null);

        super.setBackgroundColor(getResources().getColor(android.R.color.transparent));
    }

    private void initElements() {

        if (mDividerWidth > mScallopRadius) {
            mDividerWidth = mScallopRadius;
        }

        mScallopPosition = 100 / mScallopPositionPercent;
        mScallopHeight = mScallopRadius * 2;

        setBackgroundPaint();
        setBorderPaint();
        setDividerPaint();

        mDirty = true;
        invalidate();
    }

    private void setBackgroundPaint() {
        mBackgroundPaint.setAlpha(0);
        mBackgroundPaint.setAntiAlias(true);
        mBackgroundPaint.setColor(mBackgroundColor);
        mBackgroundPaint.setStyle(Paint.Style.FILL);
    }

    private void setBorderPaint() {
        mBorderPaint.setAlpha(0);
        mBorderPaint.setAntiAlias(true);
        mBorderPaint.setColor(mBorderColor);
        mBorderPaint.setStrokeWidth(mBorderWidth);
        mBorderPaint.setStyle(Paint.Style.STROKE);
    }

    private void setDividerPaint() {
        mDividerPaint.setAlpha(0);
        mDividerPaint.setAntiAlias(true);
        mDividerPaint.setColor(mDividerColor);
        mDividerPaint.setStrokeWidth(mDividerWidth);

        if (mDividerType == DividerType.DASH)
            mDividerPaint.setPathEffect(new DashPathEffect(new float[]{(float) mDividerDashLength, (float) mDividerDashGap}, 0.0f));
        else
            mDividerPaint.setPathEffect(new PathEffect());
    }

    private RectF getTopLeftCornerRoundedArc(float top, float left) {
        mRoundedCornerArc.set(left, top, left + mCornerRadius * 2, top + mCornerRadius * 2);
        return mRoundedCornerArc;
    }

    private RectF getTopRightCornerRoundedArc(float top, float right) {
        mRoundedCornerArc.set(right - mCornerRadius * 2, top, right, top + mCornerRadius * 2);
        return mRoundedCornerArc;
    }

    private RectF getBottomLeftCornerRoundedArc(float left, float bottom) {
        mRoundedCornerArc.set(left, bottom - mCornerRadius * 2, left + mCornerRadius * 2, bottom);
        return mRoundedCornerArc;
    }

    private RectF getBottomRightCornerRoundedArc(float bottom, float right) {
        mRoundedCornerArc.set(right - mCornerRadius * 2, bottom - mCornerRadius * 2, right, bottom);
        return mRoundedCornerArc;
    }

    private RectF getTopLeftCornerScallopArc(float top, float left) {
        mScallopCornerArc.set(left - mCornerRadius, top - mCornerRadius, left + mCornerRadius, top + mCornerRadius);
        return mScallopCornerArc;
    }

    private RectF getTopRightCornerScallopArc(float top, float right) {
        mScallopCornerArc.set(right - mCornerRadius, top - mCornerRadius, right + mCornerRadius, top + mCornerRadius);
        return mScallopCornerArc;
    }

    private RectF getBottomLeftCornerScallopArc(float left, float bottom) {
        mScallopCornerArc.set(left - mCornerRadius, bottom - mCornerRadius, left + mCornerRadius, bottom + mCornerRadius);
        return mScallopCornerArc;
    }

    private RectF getBottomRightCornerScallopArc(float bottom, float right) {
        mScallopCornerArc.set(right - mCornerRadius, bottom - mCornerRadius, right + mCornerRadius, bottom + mCornerRadius);
        return mScallopCornerArc;
    }

    public int getOrientation() {
        return mOrientation;
    }

    public void setOrientation(int orientation) {
        this.mOrientation = orientation;
        initElements();
    }

    public float getScallopPositionPercent() {
        return mScallopPositionPercent;
    }

    public void setScallopPositionPercent(float scallopPositionPercent) {
        this.mScallopPositionPercent = scallopPositionPercent;
        initElements();
    }

    public int getBackgroundColor() {
        return mBackgroundColor;
    }

    public void setBackgroundColor(int backgroundColor) {
        this.mBackgroundColor = backgroundColor;
        initElements();
    }

    public boolean isShowBorder() {
        return mShowBorder;
    }

    public void setShowBorder(boolean showBorder) {
        this.mShowBorder = showBorder;
        initElements();
    }

    public int getBorderWidth() {
        return mBorderWidth;
    }

    public void setBorderWidth(int borderWidth) {
        this.mBorderWidth = borderWidth;
        initElements();
    }

    public int getBorderColor() {
        return mBorderColor;
    }

    public void setBorderColor(int borderColor) {
        this.mBorderColor = borderColor;
        initElements();
    }

    public boolean isShowDivider() {
        return mShowDivider;
    }

    public void setShowDivider(boolean showDivider) {
        this.mShowDivider = showDivider;
        initElements();
    }

    public int getScallopRadius() {
        return mScallopRadius;
    }

    public void setScallopRadius(int scallopRadius) {
        this.mScallopRadius = scallopRadius;
        initElements();
    }

    public int getDividerDashLength() {
        return mDividerDashLength;
    }

    public void setDividerDashLength(int dividerDashLength) {
        this.mDividerDashLength = dividerDashLength;
        initElements();
    }

    public int getDividerDashGap() {
        return mDividerDashGap;
    }

    public void setDividerDashGap(int dividerDashGap) {
        this.mDividerDashGap = dividerDashGap;
        initElements();
    }

    public int getDividerType() {
        return mDividerType;
    }

    public void setDividerType(int dividerType) {
        this.mDividerType = dividerType;
        initElements();
    }

    public int getDividerWidth() {
        return mDividerWidth;
    }

    public void setDividerWidth(int dividerWidth) {
        this.mDividerWidth = dividerWidth;
        initElements();
    }

    public int getDividerPadding() {
        return mDividerPadding;
    }

    public void setDividerPadding(int mDividerPadding) {
        this.mDividerPadding = mDividerPadding;
        initElements();
    }

    public int getDividerColor() {
        return mDividerColor;
    }

    public void setDividerColor(int dividerColor) {
        this.mDividerColor = dividerColor;
        initElements();
    }

    public int getCornerType() {
        return mCornerType;
    }

    public void setCornerType(int cornerType) {
        this.mCornerType = cornerType;
        initElements();
    }

    public int getCornerRadius() {
        return mCornerRadius;
    }

    public void setCornerRadius(int cornerRadius) {
        this.mCornerRadius = cornerRadius;
        initElements();
    }

    public void setTicketElevation(float elevation) {
        if (!isJellyBeanAndAbove()) {
            return;
        }
        setShadowBlurRadius(elevation);
        initElements();
    }

    private void setShadowBlurRadius(float elevation) {
        if (!isJellyBeanAndAbove()) {
            return;
        }

//        float maxElevation = dpToPx(24f, getContext());
//        mShadowBlurRadius = Math.min(25f * (elevation / maxElevation), 25f);

        float maxElevation = 25f;//dpToPx(24f, getContext());
        mShadowBlurRadius = elevation > maxElevation ? maxElevation : elevation;//Math.min(25f * (elevation / maxElevation), 25f);
    }

    private boolean isJellyBeanAndAbove() {
        return android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN_MR1;
    }

    private static int dpToPx(float dp, Context context) {
        return dpToPx(dp, context.getResources());
    }

    private static int dpToPx(float dp, Resources resources) {
        float px = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, resources.getDisplayMetrics());
        return (int) px;
    }
}
