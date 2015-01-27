package com.ai.ringbutton;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup.LayoutParams;

/**
 * Displays RingButton to the user.
 * <p>
 * <b>XML attributes</b>
 * <p>
 * See also android.R.styleable#View View Attributes
 *
 * @attr ref R.styleable#RingButton_externalDiameter
 * @attr ref R.styleable#RingButton_internalDiameter
 * @attr ref R.styleable#RingButton_ringBackgroundColor
 * @attr ref R.styleable#RingButton_android_text
 * @attr ref R.styleable#RingButton_android_textColor
 * @attr ref R.styleable#RingButton_android_textSize
 * @attr ref R.styleable#RingButton_android_fontFamily
 * @attr ref R.styleable#RingButton_android_typeface
 * @attr ref R.styleable#RingButton_android_textStyle
 * @attr ref R.styleable#RingButton_android_shadowColor
 * @attr ref R.styleable#RingButton_android_shadowDx
 * @attr ref R.styleable#RingButton_android_shadowDy
 * @attr ref R.styleable#RingButton_android_shadowRadius
 * @attr ref R.styleable#RingButton_android_elegantTextHeight
 * @attr ref R.styleable#RingButton_android_letterSpacing
 * @attr ref R.styleable#RingButton_android_fontFeatureSettings
 * @attr ref R.styleable#RingButtonAppearance_android_textAppearance
 */

/* Not implemented attributes:
 * android:allCaps
 * android:textColorHighlight
 * android:textColorHint
 * android:textColorLink
 */

public class RingButton extends View implements View.OnClickListener {

    private static final String TAG = "RingButton";

    private static final float INTERNAL_DIAMETER_FRACTION = 0.25f;
    private static final String ROBOTO_PATH = "fonts/Roboto-Regular.ttf";
    private static final int DEFAULT_VIEW_SIZE = LayoutParams.MATCH_PARENT;
    private static final int DEFAULT_STYLE_ATTR_NAME =
            R.attr.ringButtonStyle;
    private static final int DEFAULT_TEXT_SIZE = 15;
    private static final int DEFAULT_RING_COLOR = 0xFFFFBB33;
    private static final int DEFAULT_TEXT_COLOR = 0xFF33B5E5;

    private static Typeface customRoboto;

    private int mExternalDiameter = DEFAULT_VIEW_SIZE;
    private int mInternalDiameter = -1;
    private String mRingText;
    private float mShadowRadius, mShadowDx, mShadowDy;
    private int mShadowColor;

    private final Paint mCirclePaint = new Paint();
    private final TextPaint mTextPaint = new TextPaint();
    private final Path mTextPath = new Path();

    private int mXclick, mYclick;
    private int mSavedRingColor;
    private int mSavedTextColor;

    private Context mContext;

    /*
     *  COPIED FROM TextView SOURCE
     *  Kick-start the font cache for the zygote process (to pay the cost of
     *  initializing freetype for our default font only once).
    */
    static {
        Paint p = new Paint();
        p.setAntiAlias(true);
        // We don't care about the result, just the side-effect of measuring.
        p.measureText("H");
    }

    // For using it from java code
    public RingButton(Context context) {
        this(context, null);
    }

    // Invoked by layout inflater
    public RingButton(Context context, AttributeSet attrs) {
        // Delegate this to a more general method.
        // We have default style for RingButton named ringButtonStyle
        // (see styles.xml).
        this(context, attrs, DEFAULT_STYLE_ATTR_NAME);
    }

    // Meant for derived classes to call if they care about defStyleAttr
    // Not called by the layout inflater
    public RingButton(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public RingButton(Context context, AttributeSet attrs,
                      int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr);
        this.mContext = context;
        initRingButton(context, attrs, defStyleAttr, defStyleRes);
    }

    private void initRingButton(Context context, AttributeSet attrs,
                                int defStyleAttr, int defStyleRes) {
        this.setOnClickListener(this);
        mCirclePaint.setAntiAlias(true);
        mCirclePaint.setStyle(Paint.Style.STROKE);
        mTextPaint.setAntiAlias(true);
        mTextPaint.setTextAlign(Paint.Align.CENTER);

        // Text Appearance
        ColorStateList textCol = null;
        int textSize = DEFAULT_TEXT_SIZE;
        String fontFamily = null;
        int typefaceIndex = -1, styleIndex = -1;
        int shadowColor = mShadowColor;
        float dx = mShadowDx, dy = mShadowDy, r = mShadowRadius;
        boolean elegant = false;
        float letterSpacing = 0;
        String fontFeatureSettings = null;
        /*
         * PARTLY COPIED FROM TextView SOURCE
         * Look the appearance up without checking first if it exists because
         * it greatly simplifies the logic to be able to parse the appearance
         * first and then let specific tags for this View override it.
         */
        final Resources.Theme theme = context.getTheme();
        TypedArray a = theme.obtainStyledAttributes(attrs,
                R.styleable.RingButtonAppearance, defStyleAttr, 0);
        TypedArray appearance = null;
        int ap = a.getResourceId(
                R.styleable.RingButtonAppearance_android_textAppearance, -1);
        a.recycle();
        if (ap != -1) {
            appearance = theme.obtainStyledAttributes(ap, R.styleable.TextAppearance);
        }
        if (appearance != null) {
            int n = appearance.getIndexCount();
            for (int i = 0; i < n; i++) {
                int attr = appearance.getIndex(i);
                switch (attr) {
                    case R.styleable.TextAppearance_android_elegantTextHeight:
                        elegant = appearance.getBoolean(attr, false);
                        break;

                    case R.styleable.TextAppearance_android_fontFamily:
                        fontFamily = appearance.getString(attr);
                        break;

                    case R.styleable.TextAppearance_android_fontFeatureSettings:
                        fontFeatureSettings = appearance.getString(attr);
                        break;

                    case R.styleable.TextAppearance_android_letterSpacing:
                        letterSpacing = appearance.getFloat(attr, 0);
                        break;

                    case R.styleable.TextAppearance_android_shadowColor:
                        shadowColor = appearance.getInt(attr, 0);
                        break;

                    case R.styleable.TextAppearance_android_shadowDx:
                        dx = appearance.getFloat(attr, 0);
                        break;

                    case R.styleable.TextAppearance_android_shadowDy:
                        dy = appearance.getFloat(attr, 0);
                        break;

                    case R.styleable.TextAppearance_android_shadowRadius:
                        r = appearance.getFloat(attr, 0);
                        break;

                    case R.styleable.TextAppearance_android_textColor:
                        textCol = appearance.getColorStateList(attr);
                        break;

                    case R.styleable.TextAppearance_android_textSize:
                        textSize = appearance.getDimensionPixelSize(attr, textSize);
                        break;

                    case R.styleable.TextAppearance_android_textStyle:
                        styleIndex = appearance.getInt(attr, -1);
                        break;

                    case R.styleable.TextAppearance_android_typeface:
                        typefaceIndex = appearance.getInt(attr, -1);
                        break;
                    default:
                        break;
                    /*
                    Not implemented attributes:
                    android:allCaps
                    android:textColorHighlight
                    android:textColorHint
                    android:textColorLink
                    */
                }
            }
            appearance.recycle();
        }

        String text = "attribute android.text is not set";
        int ringColor = DEFAULT_RING_COLOR;
        int externalDiameter = mExternalDiameter;
        int internalDiameter = mInternalDiameter;
        int defaultTextColor = DEFAULT_TEXT_COLOR;

        /*
         * PARTLY COPIED FROM TextView SOURCE
         * Appearance parsed, now let specific tags for this View override it.
         */
        a = theme.obtainStyledAttributes(
                attrs, R.styleable.RingButton, defStyleAttr, defStyleRes);

        int n = a.getIndexCount();
        for (int i = 0; i < n; i++) {
            int attr = a.getIndex(i);
            switch (attr) {
                case R.styleable.RingButton_externalDiameter:
                    externalDiameter = a.getDimensionPixelSize(attr, mExternalDiameter);
                    break;

                case R.styleable.RingButton_internalDiameter:
                    internalDiameter = a.getDimensionPixelSize(attr, mInternalDiameter);
                    break;

                case R.styleable.RingButton_ringBackgroundColor:
                    ringColor = a.getColor(attr, ringColor);
                    break;

                case R.styleable.RingButton_android_text:
                    text = a.getString(attr);
                    break;

                case R.styleable.RingButton_android_textColor:
                    textCol = a.getColorStateList(attr);
                    break;

                case R.styleable.RingButton_android_textSize:
                    textSize = a.getDimensionPixelSize(attr, textSize);
                    break;

                case R.styleable.RingButton_android_fontFamily:
                    fontFamily = a.getString(attr);
                    break;

                case R.styleable.RingButton_android_typeface:
                    typefaceIndex = a.getInt(attr, typefaceIndex);
                    break;

                case R.styleable.RingButton_android_textStyle:
                    styleIndex = a.getInt(attr, styleIndex);
                    break;

                case R.styleable.RingButton_android_shadowColor:
                    shadowColor = a.getInt(attr, 0);
                    break;

                case R.styleable.RingButton_android_shadowDx:
                    dx = a.getFloat(attr, 0);
                    break;

                case R.styleable.RingButton_android_shadowDy:
                    dy = a.getFloat(attr, 0);
                    break;

                case R.styleable.RingButton_android_shadowRadius:
                    r = a.getFloat(attr, 0);
                    break;

                case R.styleable.RingButton_android_elegantTextHeight:
                    elegant = a.getBoolean(attr, false);
                    break;

                case R.styleable.RingButton_android_letterSpacing:
                    letterSpacing = a.getFloat(attr, 0);
                    break;

                case R.styleable.RingButton_android_fontFeatureSettings:
                    fontFeatureSettings = a.getString(attr);
                    break;
                default:
                    break;
            }
        }
        a.recycle();
        setExternalDiameter(externalDiameter);
        setInternalDiameter(internalDiameter);
        setRingBackgroundColor(ringColor);
        setTextColor(textCol != null ? textCol : ColorStateList.valueOf(defaultTextColor));
        setRawTextSize(textSize);
        setElegantTextHeight(elegant);
        setLetterSpacing(letterSpacing);
        setFontFeatureSettings(fontFeatureSettings);
        if (fontFamily != null || typefaceIndex != -1 || styleIndex != -1) {
            setTypefaceFromAttrs(fontFamily, typefaceIndex, styleIndex);
        } else {
            // set Roboto font
            setDefaultFont();
        }
        if (shadowColor != 0) {
            setShadowLayer(r, dx, dy, shadowColor);
        }
        setText(text);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        Log.i(TAG, "Entered onMeasure");
        setMeasuredDimension(getRevisedDefaultSize(widthMeasureSpec),
                getRevisedDefaultSize(heightMeasureSpec));
    }

    private int getRevisedDefaultSize(int sizeMeasureSpec) {
        int result;
//      int specMode = MeasureSpec.getMode(measureSpec);
        int size = MeasureSpec.getSize(sizeMeasureSpec);

        // attribute externalDiameter is not set in layout specification
        // (therefore default value match_parent is applied)
        // or it is set as <=0
        if (mExternalDiameter <= 0) {
            result = size;
        } else {
            result = mExternalDiameter;
        }

        // doesn't matter in this case
/*      switch (specMode) {
            case MeasureSpec.UNSPECIFIED:
                result = size;
                break;
            case MeasureSpec.AT_MOST:
            case MeasureSpec.EXACTLY:
                result = specSize;
                break;
        }
*/
        return result;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        Log.i(TAG, "Entered onDraw");
        int w = this.getWidth();
        int h = this.getHeight();
        int ox = w / 2;
        int oy = h / 2;
        int extDiameter = Math.min(ox * 2, oy * 2);
        float intDiameter;
        if (mInternalDiameter < 0) {
            intDiameter = extDiameter * INTERNAL_DIAMETER_FRACTION;
        } else {
            intDiameter = mInternalDiameter;
        }
        float strokeWidth = (extDiameter - intDiameter) / 2.0f;
        float circleMagicRadius = (intDiameter + strokeWidth) / 2.0f;

        mCirclePaint.setStrokeWidth(strokeWidth);
        canvas.drawCircle(ox, oy, circleMagicRadius, mCirclePaint);

        mTextPath.addCircle(ox, oy, circleMagicRadius, Path.Direction.CW);
        float vOffset = -mTextPaint.descent()
                + (mTextPaint.descent() - mTextPaint.ascent()) / 2.0f;
        canvas.save();
        canvas.rotate(90, ox, oy);
        canvas.drawTextOnPath(mRingText, mTextPath, 0, vOffset, mTextPaint);
        canvas.restore();
        mTextPath.rewind();
    }

    @Override
    public boolean onTouchEvent(@NonNull MotionEvent event) {
        Log.i(TAG, "Entered onTouchEvent");

        // acquire coordinates to correctly handle onClick event
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_MOVE:
                mXclick = (int) event.getX();
                mYclick = (int) event.getY();
            default:
                return super.onTouchEvent(event);
        }
    }

    @Override
    public void onClick(View v) {
        Log.i(TAG, "Entered onClick");

        int ox = v.getWidth() / 2;
        int oy = v.getHeight() / 2;
        long extRadius = Math.min(ox, oy);
        long extRadiusSquared = extRadius * extRadius;
        long dx = mXclick - ox;
        long dxSquared = dx * dx;
        long dy = mYclick - oy;
        long dySquared = dy * dy;

        // check if onClick happened inside RingButton
        if (dxSquared + dySquared <= extRadiusSquared) {
            int tmpColor;
            tmpColor = mCirclePaint.getColor();
            mCirclePaint.setColor(mTextPaint.getColor());
            mTextPaint.setColor(tmpColor);
            // Request re-draw
            invalidate();
        }
    }

    /**
     * Sets default font (Roboto) for text inside the RingButton.
     */
    public void setDefaultFont() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mTextPaint.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
        } else {
            try {
                if (customRoboto == null) {
                    customRoboto = Typeface.createFromAsset(mContext.getAssets(),
                            ROBOTO_PATH);
                }
                mTextPaint.setTypeface(customRoboto);
            } catch (Exception e) {
                Log.e(TAG, "File: " + ROBOTO_PATH + " not found");
            }
        }
        invalidate();
    }

    // partly copied from TextView

    /**
     * Sets the text value to display inside the RingButton.
     *
     * @see #getText()
     *
     * @attr ref R.styleable#RingButton_android_text
     */
    public void setText(String text) {
        if (!text.equals(mRingText)) {
            mRingText = text;
            invalidate();
        }
    }

    /**
     * Return the text the RingButton is displaying.
     *
     * @see #setText(String)
     *
     * @attr ref R.styleable#RingButton_android_text
     */
    public String getText() {
        return mRingText;
    }

    /**
     * Sets the text color.
     *
     * @see #setTextColor(ColorStateList)
     * @see #getTextColor()
     *
     * @attr ref R.styleable#RingButton_android_textColor
     */
    public void setTextColor(int color) {
        if (color != mTextPaint.getColor()) {
            mTextPaint.setColor(color);
            invalidate();
        }
    }

    /**
     * Sets the text color.
     *
     * @see #setTextColor(int)
     * @see #getTextColor()
     *
     * @attr ref R.styleable#RingButton_android_textColor
     */
    public void setTextColor(ColorStateList colors) {
        if (colors == null) {
            throw new NullPointerException();
        }

        // Both RingButton states (color change inversion) are saved in
        // separate attributes: textColor and ringBackgroundColor.
        // Each of these attributes can have only 1 state,
        // therefore we don't need multistate colors and it is
        // acceptable if ColorStateList has just defaultColor.
        int color = colors.getDefaultColor();
        setTextColor(color);
    }

    /**
     * Gets the text color of the RingButton.
     *
     * @see #setTextColor(ColorStateList)
     * @see #setTextColor(int)
     *
     * @attr ref R.styleable#RingButton_android_textColor
     */
    public int getTextColor() {
        return mTextPaint.getColor();
    }

    @SuppressWarnings("unused")
    public float getTextSize() {
        return mTextPaint.getTextSize();
    }

    /**
     * Set the default text size to the given value, interpreted as "scaled
     * pixel" units.  This size is adjusted based on the current density and
     * user font size preference.
     *
     * @param size The scaled pixel size.
     *
     * @attr ref R.styleable#RingButton_android_textSize
     */
    @SuppressWarnings("unused")
    public void setTextSize(float size) {
        setTextSize(TypedValue.COMPLEX_UNIT_SP, size);
    }

    /**
     * Set the default text size to a given unit and value. See {
     * TypedValue} for the possible dimension units.
     *
     * @param unit The desired dimension unit.
     * @param size The desired size in the given units.
     *
     * @attr ref R.styleable#RingButton_android_textSize
     */
    public void setTextSize(int unit, float size) {
        Context c = getContext();
        Resources r;

        if (c == null) { r = Resources.getSystem(); }
        else { r = c.getResources(); }

        setRawTextSize(TypedValue.applyDimension(
                unit, size, r.getDisplayMetrics()));
    }

    private void setRawTextSize(float size) {
        if (size != mTextPaint.getTextSize()) {
            mTextPaint.setTextSize(size);
            invalidate();
        }
    }

    /**
     * Set the RingButtons's elegant height metrics flag. This setting selects font
     * variants that have not been compacted to fit Latin-based vertical
     * metrics, and also increases top and bottom bounds to provide more space.
     *
     * @param elegant set the paint's elegant metrics flag.
     *
     * @attr ref R.styleable#RingButton_android_elegantTextHeight
     */
    public void setElegantTextHeight(boolean elegant) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mTextPaint.setElegantTextHeight(elegant);
        }
    }

    /**
     * @return the extent by which text is currently being letter-spaced.
     * This will normally be 0.
     *
     * @see #setLetterSpacing(float)
     * @see Paint#setLetterSpacing
     */
    public float getLetterSpacing() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            return mTextPaint.getLetterSpacing();
        }
        return 0;
    }

    /**
     * Sets text letter-spacing.  The value is in 'EM' units.  Typical values
     * for slight expansion will be around 0.05.  Negative values tighten text.
     *
     * @see #getLetterSpacing()
     * @see Paint#getLetterSpacing
     *
     * @attr ref R.styleable#RingButton_android_letterSpacing
     */
    public void setLetterSpacing(float letterSpacing) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (letterSpacing != mTextPaint.getLetterSpacing()) {
                mTextPaint.setLetterSpacing(letterSpacing);
                invalidate();
            }
        }
    }

    /**
     * @return the currently set font feature settings.  Default is null.
     *
     * @see #setFontFeatureSettings(String)
     * @see Paint#setFontFeatureSettings
     */
    @Nullable
    public String getFontFeatureSettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            return mTextPaint.getFontFeatureSettings();
        }
        return null;
    }

    /**
     * Sets font feature settings. The format is the same as the CSS
     * font-feature-settings attribute:
     * http://dev.w3.org/csswg/css-fonts/#propdef-font-feature-settings
     *
     * @param fontFeatureSettings font feature settings represented as CSS compatible string
     *
     * @see #getFontFeatureSettings()
     * @see Paint#getFontFeatureSettings
     *
     * @attr ref R.styleable#RingButton_android_fontFeatureSettings
     */
    public void setFontFeatureSettings(@Nullable String fontFeatureSettings) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if ((fontFeatureSettings != null)
                    && (!fontFeatureSettings.equals(mTextPaint.getFontFeatureSettings()))) {
                mTextPaint.setFontFeatureSettings(fontFeatureSettings);
                invalidate();
            }
        }
    }

    // copied from TextView
    private void setTypefaceFromAttrs(String familyName, int typefaceIndex,
                                      int styleIndex) {
        // Enum for the "typeface" XML parameter.
        final int SANS = 1;
        final int SERIF = 2;
        final int MONOSPACE = 3;

        Typeface tf = null;
        if (familyName != null) {
            tf = Typeface.create(familyName, styleIndex);
            if (tf != null) {
                setTypeface(tf);
                return;
            }
        }
        switch (typefaceIndex) {
            case SANS:
                tf = Typeface.SANS_SERIF;
                break;

            case SERIF:
                tf = Typeface.SERIF;
                break;

            case MONOSPACE:
                tf = Typeface.MONOSPACE;
                break;
            default:
                break;
        }

        setTypeface(tf, styleIndex);
    }

    // COPIED FROM TextView
    /**
     * Sets the typeface and style in which the text should be displayed,
     * and turns on the fake bold and italic bits in the Paint if the
     * Typeface that you provided does not have all the bits in the
     * style that you specified.
     *
     * @attr ref R.styleable#RingButton_android_typeface
     * @attr ref R.styleable#RingButton_android_textStyle
     */
    public void setTypeface(Typeface tf, int style) {
        if (style > 0) {
            if (tf == null) {
                tf = Typeface.defaultFromStyle(style);
            } else {
                tf = Typeface.create(tf, style);
            }

            setTypeface(tf);
            // now compute what (if any) algorithmic styling is needed
            int typefaceStyle = tf != null ? tf.getStyle() : 0;
            int need = style & ~typefaceStyle;
            mTextPaint.setFakeBoldText((need & Typeface.BOLD) != 0);
            mTextPaint.setTextSkewX((need & Typeface.ITALIC) != 0 ? -0.25f : 0);
        } else {
            mTextPaint.setFakeBoldText(false);
            mTextPaint.setTextSkewX(0);
            setTypeface(tf);
        }
    }

    /**
     * Sets the typeface and style in which the text should be displayed.
     * Note that not all Typeface families actually have bold and italic
     * variants, so you may need to use
     * {@link #setTypeface(Typeface, int)} to get the appearance
     * that you actually want.
     *
     * @see #getTypeface()
     *
     * @attr ref R.styleable#RingButton_android_fontFamily
     * @attr ref R.styleable#RingButton_android_typeface
     * @attr ref R.styleable#RingButton_android_textStyle
     */
    public void setTypeface(Typeface tf) {
        if (mTextPaint.getTypeface() != tf) {
            mTextPaint.setTypeface(tf);
            invalidate();
        }
    }

    /**
     * @return the current typeface and style in which the text is being
     * displayed.
     *
     * @see #setTypeface(Typeface)
     *
     * @attr ref R.styleable#RingButton_android_fontFamily
     * @attr ref R.styleable#RingButton_android_typeface
     * @attr ref R.styleable#RingButton_android_textStyle
     */
    public Typeface getTypeface() {
        return mTextPaint.getTypeface();
    }

    /**
     * Gives the text a shadow of the specified blur radius and color, the specified
     * distance from its drawn position.
     * <p>
     * The text shadow produced does not interact with the properties on view
     * that are responsible for real time shadows,
     * {@link View#getElevation() elevation} and
     * {@link View#getTranslationZ() translationZ}.
     *
     * @see Paint#setShadowLayer(float, float, float, int)
     *
     * @attr ref R.styleable#RingButton_android_shadowColor
     * @attr ref R.styleable#RingButton_android_shadowDx
     * @attr ref R.styleable#RingButton_android_shadowDy
     * @attr ref R.styleable#RingButton_android_shadowRadius
     */
    public void setShadowLayer(float radius, float dx, float dy, int color) {
        mTextPaint.setShadowLayer(radius, dx, dy, color);

        mShadowRadius = radius;
        mShadowDx = dx;
        mShadowDy = dy;
        mShadowColor = color;
        invalidate();
    }

    /**
     * Gets the radius of the shadow layer.
     *
     * @return the radius of the shadow layer. If 0, the shadow layer is not visible
     *
     * @see #setShadowLayer(float, float, float, int)
     *
     * @attr ref R.styleable#RingButton_android_shadowRadius
     */
    @SuppressWarnings("unused")
    public float getShadowRadius() {
        return mShadowRadius;
    }

    /**
     * @return the horizontal offset of the shadow layer
     *
     * @see #setShadowLayer(float, float, float, int)
     *
     * @attr ref R.styleable#RingButton_android_shadowDx
     */
    @SuppressWarnings("unused")
    public float getShadowDx() {
        return mShadowDx;
    }

    /**
     * @return the vertical offset of the shadow layer
     *
     * @see #setShadowLayer(float, float, float, int)
     *
     * @attr ref R.styleable#RingButton_android_shadowDy
     */
    @SuppressWarnings("unused")
    public float getShadowDy() {
        return mShadowDy;
    }

    /**
     * @return the color of the shadow layer
     *
     * @see #setShadowLayer(float, float, float, int)
     *
     * @attr ref R.styleable#RingButton_android_shadowColor
     */
    @SuppressWarnings("unused")
    public int getShadowColor() {
        return mShadowColor;
    }

    /**
     * Sets the text color, size, style, hint color, and highlight color
     * from the specified TextAppearance resource.
     *
     * @attr ref R.styleable#RingButtonAppearance_android_textAppearance
     */
    @SuppressWarnings("unused")
    public void setTextAppearance(Context context, int resid) {
        TypedArray appearance =
                context.obtainStyledAttributes(resid,
                        R.styleable.TextAppearance);
        ColorStateList colors;
        int ts;

        colors = appearance.getColorStateList(
                R.styleable.TextAppearance_android_textColor);
        if (colors != null) {
            setTextColor(colors);
        }

        ts = appearance.getDimensionPixelSize(
                R.styleable.TextAppearance_android_textSize, 0);
        if (ts != 0) {
            setRawTextSize(ts);
        }

        String familyName;
        int typefaceIndex, styleIndex;

        familyName = appearance.getString(
                R.styleable.TextAppearance_android_fontFamily);
        typefaceIndex = appearance.getInt(
                R.styleable.TextAppearance_android_typeface, -1);
        styleIndex = appearance.getInt(
                R.styleable.TextAppearance_android_textStyle, -1);

        setTypefaceFromAttrs(familyName, typefaceIndex, styleIndex);

        int shadowcolor = appearance.getInt(
                R.styleable.TextAppearance_android_shadowColor, 0);
        if (shadowcolor != 0) {
            float dx = appearance.getFloat(
                    R.styleable.TextAppearance_android_shadowDx, 0);
            float dy = appearance.getFloat(
                    R.styleable.TextAppearance_android_shadowDy, 0);
            float r = appearance.getFloat(
                    R.styleable.TextAppearance_android_shadowRadius, 0);

            setShadowLayer(r, dx, dy, shadowcolor);
        }

        if (appearance.hasValue(
                R.styleable.TextAppearance_android_elegantTextHeight)) {
            setElegantTextHeight(appearance.getBoolean(
                    R.styleable.TextAppearance_android_elegantTextHeight, false));
        }

        if (appearance.hasValue(
                R.styleable.TextAppearance_android_fontFeatureSettings)) {
            setFontFeatureSettings(appearance.getString(
                    R.styleable.TextAppearance_android_fontFeatureSettings));
        }

        if (appearance.hasValue(R.styleable.TextAppearance_android_letterSpacing)) {
            setLetterSpacing(appearance.getFloat(
                    R.styleable.TextAppearance_android_letterSpacing, 0));
        }

        /*
        Not implemented attributes:
        android:allCaps
        android:textColorHighlight
        android:textColorHint
        android:textColorLink
        */

        appearance.recycle();
    }

    /**
     * @return the base paint used for the text.  Please use this only to
     * consult the Paint's properties and not to change them.
     */
    @SuppressWarnings("unused")
    public TextPaint getPaint() {
        return mTextPaint;
    }

    /**
     * Sets flags on the Paint being used to display the text and
     * reflows the text if they are different from the old flags.
     *
     * @see Paint#setFlags
     */
    @SuppressWarnings("unused")
    public void setPaintFlags(int flags) {
        if (mTextPaint.getFlags() != flags) {
            mTextPaint.setFlags(flags);
            invalidate();
        }
    }

    /**
     * @return the flags on the Paint being used to display the text.
     * @see Paint#getFlags
     */
    @SuppressWarnings("unused")
    public int getPaintFlags() {
        return mTextPaint.getFlags();
    }

    /**
     * @return external diameter of RingButton
     *
     * @attr ref R.styleable#RingButton_externalDiameter
     */
    @SuppressWarnings("unused")
    public int getExternalDiameter() {
        return mExternalDiameter;
    }

    /**
     * Sets external diameter of RingButton.
     *
     * @attr ref R.styleable#RingButton_externalDiameter
     */
    public void setExternalDiameter(int externalDiameter) {
        if (externalDiameter != mExternalDiameter) {
            mExternalDiameter = externalDiameter;
            requestLayout();
            invalidate();
        }
    }

    /**
     * @return internal diameter of RingButton
     *
     * @attr ref R.styleable#RingButton_internalDiameter
     */
    @SuppressWarnings("unused")
    public int getInternalDiameter() {
        return mInternalDiameter;
    }

    /**
     * Sets internal diameter of RingButton.
     *
     * @attr ref R.styleable#RingButton_internalDiameter
     */
    public void setInternalDiameter(int internalDiameter) {
        if (internalDiameter != mInternalDiameter) {
            mInternalDiameter = internalDiameter;
            invalidate();
        }
    }

    /**
     * @return ring background color of RingButton
     *
     * @attr ref R.styleable#RingButton_ringBackgroundColor
     */
    @SuppressWarnings("unused")
    public int getRingBackgroundColor() {
        return mCirclePaint.getColor();
    }

    /**
     * Sets ring background color of RingButton.
     *
     * @attr ref R.styleable#RingButton_ringBackgroundColor
     */
    public void setRingBackgroundColor(int ringBackgroundColor) {
        if (ringBackgroundColor != mCirclePaint.getColor()) {
            mCirclePaint.setColor(ringBackgroundColor);
            invalidate();
        }
    }

    /*
    * ***************************************************************
    * Save and restore work
    * BaseSavedState Pattern
    * from Expert Android (Komatineni S., MacLean D.)
    * ***************************************************************
    */

    private void restoreColors() {
        Log.i(TAG, "Entered restoreColors()");
        mCirclePaint.setColor(mSavedRingColor);
        mTextPaint.setColor(mSavedTextColor);
    }


    @Override
    protected void onRestoreInstanceState(Parcelable p) {
        Log.i(TAG, "Entered onRestoreInstanceState()");
        this.onRestoreInstanceStateStandard(p);
        this.restoreColors();
    }
    @Override
    protected Parcelable onSaveInstanceState() {
        Log.i(TAG, "Entered onSaveInstanceState()");
        return this.onSaveInstanceStateStandard();
    }

    private void onRestoreInstanceStateStandard(Parcelable state) {
        //If it is not yours doesn't mean it is BaseSavedState
        //You may have a parent in your hierarchy that has their own
        //state derived from BaseSavedState
        //It is like peeling an onion or a Russian doll
        if (!(state instanceof SavedState)) {
            super.onRestoreInstanceState(state);
            return;
        }
        //it is our state
        SavedState ss = (SavedState) state;
        //Peel it and give the child to the super class
        super.onRestoreInstanceState(ss.getSuperState());
        mSavedRingColor = ss.ringBackgroundColor;
        mSavedTextColor = ss.textColor;
    }
    private Parcelable onSaveInstanceStateStandard() {
        Parcelable superState = super.onSaveInstanceState();
        SavedState ss = new SavedState(superState);
        ss.ringBackgroundColor = mCirclePaint.getColor();
        ss.textColor = mTextPaint.getColor();
        return ss;
    }

    /*
    * ***************************************************************
    * Saved State inner static class
    * ***************************************************************
    */
    public static class SavedState extends BaseSavedState {
        int ringBackgroundColor;
        int textColor;

        SavedState(Parcelable superState) {
            super(superState);
        }

        @Override
        public void writeToParcel(@NonNull Parcel out, int flags) {
            super.writeToParcel(out, flags);
            out.writeInt(ringBackgroundColor);
            out.writeInt(textColor);
        }

        //Read back the values
        private SavedState(Parcel in) {
            super(in);
            ringBackgroundColor = in.readInt();
            textColor = in.readInt();
        }

        @Override
        public String toString() {
            return "RingButton ringBackgroundColor: " + ringBackgroundColor
                    + " , textColor: " + textColor;
        }

        @SuppressWarnings("hiding")
        public static final Parcelable.Creator<SavedState> CREATOR
                = new Parcelable.Creator<SavedState>() {
            public SavedState createFromParcel(Parcel in) {
                return new SavedState(in);
            }
            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };
    }
}