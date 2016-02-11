package com.example.android.renderscriptimaging.image;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;

/**
 * This class handles all the background threading of working with the
 * images. Loading files from disk and processing RenderScript commands
 * are too time intensive to handle on the main thread.
 */
public class ImageProcessor implements Handler.Callback {
    private static final String TAG = ImageProcessor.class.getSimpleName();

    /* Message types to distinguish commands in the Handler */
    private static final int MESSAGE_LOAD = 1;
    private static final int MESSAGE_FILTER = 2;

    /* Callback interface for image results */
    public interface OnImageCompletedListener {
        /**
         * Callback invoked when the attached ImageProcessor has a new
         * Bitmap available for display.
         *
         * @param image Bitmap to be displayed.
         * @param duration Time taken for the operation to complete.
         */
        void onImageAvailable(Bitmap image, long duration);
    }

    /* Local message handlers and background Looper thread */
    private HandlerThread mHandlerThread;
    private Handler mBackgroundHandler;
    private Handler mCallbackHandler;

    /* Local reference to the RenderScript filters */
    private ImageFilter mImageFilter;
    private Resources mResources;

    /* Local callback for asynchronous results */
    private OnImageCompletedListener mImageAvailableListener;

    public ImageProcessor(Context context, OnImageCompletedListener listener) {
        mImageFilter = new ImageFilter(context);
        mResources = context.getResources();

        mImageAvailableListener = listener;

        //Create a background Looper thread to process log requests
        mHandlerThread = new HandlerThread(TAG,
                Process.THREAD_PRIORITY_BACKGROUND);
        mHandlerThread.start();

        //Attach a handler to the new thread, with this object as the callback
        mBackgroundHandler = new Handler(mHandlerThread.getLooper(), this);
        //Create a handler to the main thread for callbacks
        mCallbackHandler = new Handler(Looper.getMainLooper());
    }

    /* Callbacks invoked on the background Looper thread for blocking work */
    @Override
    public boolean handleMessage(Message msg) {
        switch (msg.what) {
            case MESSAGE_LOAD:
                //Request to load file from assets
                String filename = (String) msg.obj;
                handleImageLoad(filename);
                break;
            case MESSAGE_FILTER:
                //Request to apply filter to current image
                int filter = msg.arg1;
                handleImageFilter(filter);
                break;
            default:
                //We can't handle this message
                return false;
        }
        return true;
    }

    /* Perform the blocking work associated with an image load */
    private void handleImageLoad(String filename) {
        try {
            long now = System.currentTimeMillis();
            Bitmap decoded = decodeScaledImage(filename);
            mImageFilter.setInputBitmap(decoded);
            long duration = System.currentTimeMillis() - now;
            postImageCallback(decoded, duration);
        } catch (Exception e) {
            Log.w(TAG, "Unable to decode selected image", e);
            postImageCallback(null, 0);
        }
    }

    /* Perform the blocking work associated with an image filter */
    private void handleImageFilter(int filter) {
        long now = System.currentTimeMillis();
        if (ImageFilter.FILTER_NONE == filter) {
            mImageFilter.clearFilter();
        } else {
            mImageFilter.applyFilter(filter);
        }
        long duration = System.currentTimeMillis() - now;
        postImageCallback(mImageFilter.getBitmap(), duration);
    }

    /* Trigger the result callback on the application's main thread */
    private void postImageCallback(final Bitmap image, final long duration) {
        mCallbackHandler.post(new Runnable() {
            @Override
            public void run() {
                mImageAvailableListener.onImageAvailable(image, duration);
            }
        });
    }

    /**
     * Request the given file be loaded from assets into memory.
     * The result will be delivered via
     * {@link OnImageCompletedListener#onImageAvailable(Bitmap, long)}
     *
     * @param filename Name of the file stored in application assets.
     */
    public void loadImageAsset(String filename) {
        Message msg = mBackgroundHandler.obtainMessage(MESSAGE_LOAD, filename);
        msg.sendToTarget();
    }

    /**
     * Request the given filter algorithm be applied to the currently loaded
     * image using RenderScript. The result will be delivered via
     * {@link OnImageCompletedListener#onImageAvailable(Bitmap, long)}
     *
     * @param filter Filter algorithm constant defined by {@link ImageFilter}
     */
    public void applyImageFilter(int filter) {
        Message msg = mBackgroundHandler.obtainMessage(MESSAGE_FILTER, filter, 0);
        msg.sendToTarget();
    }

    /**
     * Call this method to tear down the {@link ImageProcessor} and its
     * associated thread.
     *
     * @return true if successful in asking the underlying Looper to quit
     */
    public boolean destroy() {
        mImageAvailableListener = null;
        mImageFilter.destroy();

        return mHandlerThread.quit();
    }

    /* Down-sample and decode a user-selected image. */
    private Bitmap decodeScaledImage(String filename) throws IOException {
        BitmapFactory.Options options = new BitmapFactory.Options();

        //First, obtain the image size
        options.inJustDecodeBounds = true;
        InputStream in = mResources.getAssets().open(filename);
        BitmapFactory.decodeStream(in, null, options);
        in.close();

        //Scale image down if larger than the display
        int scaledWidth = options.outWidth /
                mResources.getDisplayMetrics().widthPixels;
        if (scaledWidth > 1) {
            //This sets the downsample scale factor
            options.inSampleSize = scaledWidth;
        }
        //Now decode the image for real
        options.inJustDecodeBounds = false;
        in = mResources.getAssets().open(filename);
        Bitmap image = BitmapFactory
                .decodeStream(in, null, options);
        in.close();

        Log.d(TAG, "Decoded " + image.getWidth() + "x" + image.getHeight()
                + " image");
        return image;
    }
}
