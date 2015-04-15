package com.example.android.renderscriptimaging;


import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.support.v8.renderscript.*;

public class ImageFilter {

    /** Supported Image Filter Algorithms */
    public static final int FILTER_NONE = 0;
    public static final int FILTER_RIPPLE = 1;
    public static final int FILTER_BLUR = 2;
    public static final int FILTER_MONO = 3;
    public static final int FILTER_SHARPEN = 4;
    public static final int FILTER_LIGHTEN = 5;
    public static final int FILTER_DARKEN = 6;
    public static final int FILTER_EDGE = 7;
    public static final int FILTER_EMBOSS = 8;

    private RenderScript mRSContext;
    private Resources mResources;
    private Allocation mInputAllocation;
    private Bitmap mFilteredResult;

    public ImageFilter(Context context) {
        //Create the RenderScript context
        mRSContext = RenderScript.create(context);
        mResources = context.getResources();
    }

    public void destroy() {
        mRSContext.destroy();
    }

    /*
     * Called each time a new original is selected. Caching these
     * items reduces memory copies while the user picks their
     * appropriate filter.
     */
    public void setInputBitmap(Bitmap bitmap) {
        //Construct an allocation for the new original
        mInputAllocation = Allocation.createFromBitmap(mRSContext,
                bitmap,
                Allocation.MipmapControl.MIPMAP_NONE,
                Allocation.USAGE_SCRIPT);
        //Create a mutable instance for filtered results
        mFilteredResult = bitmap.copy(bitmap.getConfig(), true);
    }

    /*
     * Return the result bitmap back to match the original
     */
    public void clearFilter() {
        if (mInputAllocation == null) return;

        //Set back to the original value
        mInputAllocation.copyTo(mFilteredResult);
    }

    /*
     * Apply the selected filter algorithm with RenderScript
     */
    public void applyFilter(int filter) {
        if (mInputAllocation == null) return;

        final Allocation output = Allocation.createTyped(mRSContext,
                mInputAllocation.getType());

        switch (filter) {
            case FILTER_RIPPLE:
                //Our custom script defined in ripple.rs
                ScriptC_ripple scriptR = new ScriptC_ripple(mRSContext,
                        mResources, R.raw.ripple);

                //Set up ripple control values
                //Center at top-left of the image
                scriptR.set_centerX(0);
                scriptR.set_centerY(0);
                //Optional minimum inner radius
                scriptR.set_minRadius(0f);
                //Wave properties
                scriptR.set_scalar(0.75f);     //0.01f - 1.0f
                scriptR.set_damper(0.002f);    //0.0001f - 0.01f
                scriptR.set_frequency(0.075f); //0.01f - 0.5f

                //Run the script
                scriptR.forEach_root(mInputAllocation, output);
                break;
            case FILTER_BLUR:
                ScriptIntrinsicBlur scriptBlur =
                        ScriptIntrinsicBlur.create(mRSContext,
                                Element.U8_4(mRSContext));
                scriptBlur.setRadius(25f);
                scriptBlur.setInput(mInputAllocation);
                scriptBlur.forEach(output);
                break;
            case FILTER_MONO:
                ScriptIntrinsicColorMatrix scriptColor =
                        ScriptIntrinsicColorMatrix.create(mRSContext,
                                Element.U8_4(mRSContext));
                scriptColor.setGreyscale();
                scriptColor.forEach(mInputAllocation, output);
                break;
            case FILTER_SHARPEN:
            case FILTER_LIGHTEN:
            case FILTER_DARKEN:
            case FILTER_EDGE:
            case FILTER_EMBOSS:
                //Convolution filters
                ScriptIntrinsicConvolve3x3 scriptC =
                        ScriptIntrinsicConvolve3x3.create(mRSContext,
                                Element.U8_4(mRSContext));
                scriptC.setCoefficients(
                        ConvolutionFilter.getCoefficients(filter));
                scriptC.setInput(mInputAllocation);
                scriptC.forEach(output);
                break;
            default:
                //Do nothing
                return;
        }

        output.copyTo(mFilteredResult);
    }

    //External access to the result for display/save
    public Bitmap getBitmap() {
        return mFilteredResult;
    }
}
