package com.example.android.renderscriptimaging;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;

import com.example.android.renderscriptimaging.image.ImageProcessor;

public class MainActivity extends AppCompatActivity implements
        AdapterView.OnItemSelectedListener,
        DialogInterface.OnClickListener,
        ImageProcessor.OnImageCompletedListener {
    private static final String TAG = MainActivity.class.getSimpleName();

    private ImageView mImagePreview;
    private Spinner mFilterSelector;
    private ProgressBar mProgress;
    private TextView mResultText;

    private ImageProcessor mImageProcessor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mImagePreview = (ImageView) findViewById(R.id.image_preview);
        mFilterSelector = (Spinner) findViewById(R.id.filter_selector);
        mProgress = (ProgressBar) findViewById(R.id.progress);
        mResultText = (TextView) findViewById(R.id.text_result);

        mImageProcessor = new ImageProcessor(this, this);

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
        mImageProcessor.destroy();
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view,
                               int position, long id) {
        mImageProcessor.applyImageFilter(position);
        mProgress.setVisibility(View.VISIBLE);
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) { }

    /* Pick an image from the assets folder */
    public void onPickImageClick(View v) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.title_pick)
                .setItems(R.array.images, this)
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    /* Load the selected assets file */
    private void setSelectedImage(String filename) {
        mImageProcessor.loadImageAsset(filename);
        mProgress.setVisibility(View.VISIBLE);

        //Reset the filter selector on a new image
        mFilterSelector.setSelection(0);
    }

    /* Triggered by selecting an item in the image dialog */
    @Override
    public void onClick(DialogInterface dialog, int which) {
        String[] imageFiles = getResources()
                .getStringArray(R.array.image_files);

        setSelectedImage(imageFiles[which]);
    }

    /* Callback invoked when the ImageProcessor has a new image to display */
    @Override
    public void onImageAvailable(Bitmap image, long duration) {
        mProgress.setVisibility(View.INVISIBLE);
        mImagePreview.setImageBitmap(image);
        mResultText.setText(String.format("Request completed in %.3fs",
                duration / 1000f));
    }
}
