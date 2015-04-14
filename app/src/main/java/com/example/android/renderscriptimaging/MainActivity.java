package com.example.android.renderscriptimaging;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.Spinner;

import java.io.IOException;
import java.io.InputStream;

public class MainActivity extends Activity
        implements AdapterView.OnItemSelectedListener {
    private static final String TAG = MainActivity.class.getSimpleName();
    private static final int REQUEST_PICK = 42;

    private ImageView mImagePreview;
    private Spinner mFilterSelector;
    private ImageFilter mImageFilter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mImagePreview = (ImageView) findViewById(R.id.image_preview);
        mFilterSelector = (Spinner) findViewById(R.id.filter_selector);

        mImageFilter = new ImageFilter(this);

        ArrayAdapter<CharSequence> adapter = ArrayAdapter
                .createFromResource(this, R.array.filters,
                        android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item);
        mFilterSelector.setAdapter(adapter);

        mFilterSelector.setOnItemSelectedListener(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mImageFilter.destroy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (REQUEST_PICK == requestCode
                && RESULT_OK == resultCode
                && data.getData() != null) {
            try {
                Bitmap decoded = decodeScaledImage(data.getData());
                mImageFilter.setInputBitmap(decoded);
                mImagePreview.setImageBitmap(decoded);
            } catch (Exception e) {
                Log.w(TAG, "Unable to decode selected image", e);
            }

            //Reset the filter selector on a new image
            mFilterSelector.setSelection(0);
        }
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view,
                               int position, long id) {
        if (ImageFilter.FILTER_NONE == position) {
            mImageFilter.clearFilter();
        } else {
            mImageFilter.applyFilter(position);
        }

        mImagePreview.setImageBitmap(mImageFilter.getBitmap());
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) { }

    //Pick an image from the device library
    public void onPickImageClick(View v) {
        Intent pickIntent = new Intent(Intent.ACTION_GET_CONTENT);
        pickIntent.setType("image/*");
        startActivityForResult(pickIntent, REQUEST_PICK);
    }

    /*
     * Down-sample and decode a user-selected image.
     */
    private Bitmap decodeScaledImage(Uri contentUri) throws IOException {
        BitmapFactory.Options options = new BitmapFactory.Options();

        //First, obtain the image size
        options.inJustDecodeBounds = true;
        InputStream in = getContentResolver()
                .openInputStream(contentUri);
        BitmapFactory.decodeStream(in, null, options);
        in.close();

        //Scale image down if larger than the display
        int scaledWidth = options.outWidth /
                getResources().getDisplayMetrics().widthPixels;
        if (scaledWidth > 1) {
            //This sets the downsample scale factor
            options.inSampleSize = scaledWidth;
        }
        //Now decode the image for real
        options.inJustDecodeBounds = false;
        in = getContentResolver().openInputStream(contentUri);
        Bitmap image = BitmapFactory
                .decodeStream(in, null, options);

        Log.d(TAG, "Decoded " + image.getWidth() + "x" + image.getHeight()
                + " image");
        return image;
    }
}
