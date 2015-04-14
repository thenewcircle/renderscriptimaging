package com.example.android.renderscriptimaging;

/**
 * Coefficients for Common Convolution Filters
 */
public class ConvolutionFilter {

    public static float[] getCoefficients(int filter) {
        switch (filter) {
            case ImageFilter.FILTER_SHARPEN:
                return new float[] {
                        0f, -1f, 0f,
                        -1f, 5f, -1f,
                        0f, -1f, 0f
                };
            case ImageFilter.FILTER_LIGHTEN:
                return new float[] {
                        0f, 0f, 0f,
                        0f, 1.5f, 0f,
                        0f, 0f, 0f
                };
            case ImageFilter.FILTER_DARKEN:
                return new float[] {
                        0f, 0f, 0f,
                        0f, 0.5f, 0f,
                        0f, 0f, 0f
                };
            case ImageFilter.FILTER_EDGE:
                return new float[] {
                        0f, 1f, 0f,
                        1f, -4f, 1f,
                        0f, 1f, 0f
                };
            case ImageFilter.FILTER_EMBOSS:
                return new float[] {
                        -2f, -1f, 0f,
                        -1f, 1f, 1f,
                        0f, 1f, 2f
                };
            default:
                throw new IllegalArgumentException("Unknown Convolution Filter");
        }
    }
}
