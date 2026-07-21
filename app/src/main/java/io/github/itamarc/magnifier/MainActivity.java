package io.github.itamarc.magnifier;

import android.Manifest;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ImageView;
import android.widget.SeekBar;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraControl;
import androidx.camera.core.CameraInfo;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.core.ZoomState;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.lifecycle.LiveData;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.common.util.concurrent.ListenableFuture;

import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {
    private boolean flashlightOn = false;
    private Camera camera;
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    private ImageCapture imageCapture;
    private ImageProxy frozenImage;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        checkPermissions();

        // Camera and interface setup
        cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                bindPreview(cameraProvider);
                setUpButtons();
            } catch (ExecutionException | InterruptedException e) {
                // No errors need to be handled for this Future.
                // This should never be reached.
                System.err.println("### The impossible crash occurred!");
            }
        }, ContextCompat.getMainExecutor(this));

    }

    private void checkPermissions() {
        int CAMERA_PERMISSION_REQUEST_CODE = 1;
        String[] permissions = {Manifest.permission.CAMERA};
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, permissions, CAMERA_PERMISSION_REQUEST_CODE);
        }
    }

    void bindPreview(@NonNull ProcessCameraProvider cameraProvider) {
        PreviewView camView = findViewById(R.id.cameraView);
        camView.setScaleType(PreviewView.ScaleType.FILL_CENTER);

        imageCapture = new ImageCapture.Builder().build();

        Preview preview = new Preview.Builder().build();
        preview.setSurfaceProvider(camView.getSurfaceProvider());

        camera = cameraProvider.bindToLifecycle(this, CameraSelector.DEFAULT_BACK_CAMERA,
                preview, imageCapture);
    }

    private void setUpButtons() {
        CameraInfo cameraInfo = camera.getCameraInfo();
        CameraControl cameraControl = camera.getCameraControl();

        FloatingActionButton btnSave = findViewById(R.id.btnSave);
        FloatingActionButton fabFlashlight = findViewById(R.id.btnFlashlight);
        FloatingActionButton fabFreeze = findViewById(R.id.btnFreeze);
        FloatingActionButton fabAbout = findViewById(R.id.btnAbout);
        FloatingActionButton fabExit = findViewById(R.id.btnExit);
        SeekBar camZoomBar = findViewById(R.id.cameraZoomBar);

        // Save button
        btnSave.setOnClickListener(v -> {
            if (frozenImage != null) {
                saveImage();
            } else {
                // Capture and save live feed if no frozen image
                captureImageAndSave();
            }
        });

        // Freeze image button
        fabFreeze.setOnClickListener(v -> {
            if (frozenImage != null) { // There is already an image frozen in the screen
                unfreeze();
            } else { // Let's capture and freeze an image
                freeze(null);
            }
        });

        // Flashlight on/off button
        if (cameraInfo.hasFlashUnit()) {
            fabFlashlight.setOnClickListener(v -> {
                if (flashlightOn) { // If already on, turn it off
                    fabFlashlight.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.baseline_flashlight_off_24, getTheme()));
                    fabFlashlight.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.amber_200, getTheme())));
                    cameraControl.enableTorch(false);
                    flashlightOn = false;
                } else { // If it's off, turn on
                    fabFlashlight.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.baseline_flashlight_on_24, getTheme()));
                    fabFlashlight.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.amber_700, getTheme())));
                    cameraControl.enableTorch(true);
                    flashlightOn = true;
                }
            });
        } else {
            fabFlashlight.setVisibility(View.GONE);
        }

        // About button
        String versionString = getString(R.string.app_name_with_version, BuildConfig.VERSION_NAME);
        Context context = this;
        fabAbout.setOnClickListener(v -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setView(R.layout.about_dialog);
            builder.setTitle(versionString);
            builder.setPositiveButton("Close", (dialog, which) -> dialog.dismiss());
            builder.show();
        });

        // Exit button
        fabExit.setOnClickListener(v -> System.exit(0));

        // Zoom bar
        camZoomBar.setMin(1);
        LiveData<ZoomState> zoomState = cameraInfo.getZoomState();
        camZoomBar.setMax((int)(Objects.requireNonNull(zoomState.getValue()).getMaxZoomRatio()*10));
        camZoomBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                cameraControl.setZoomRatio((float)progress/10.0f);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }

    private void captureImageAndFreeze(Runnable onComplete) {
        imageCapture.takePicture(Executors.newSingleThreadExecutor(),
                new ImageCapture.OnImageCapturedCallback() {
            @Override
            public void onCaptureSuccess(@NonNull ImageProxy image) {
                frozenImage = image;
                mainHandler.post(() -> {
                    ImageView frozenView = findViewById(R.id.frozenView);
                    Bitmap bitmap;
                    int rotation = image.getImageInfo().getRotationDegrees();
                    if (rotation == 0) {
                        bitmap = image.toBitmap();
                    } else {
                        Bitmap tempBitmap = image.toBitmap();
                        Matrix matrix = new Matrix();
                        matrix.postRotate(rotation);
                        bitmap = Bitmap.createBitmap(tempBitmap, 0, 0, tempBitmap.getWidth(), tempBitmap.getHeight(), matrix, true);
                    }
                    frozenView.setImageBitmap(bitmap);
                    frozenView.setVisibility(View.VISIBLE);
                    if (onComplete != null) {
                        onComplete.run();
                    }
                });
            }
        });
    }

    private void freeze(Runnable onComplete) {
        CameraInfo cameraInfo = camera.getCameraInfo();
        CameraControl cameraControl = camera.getCameraControl();
        FloatingActionButton fabFreeze = findViewById(R.id.btnFreeze);

        fabFreeze.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.baseline_lock_24, getTheme()));
        fabFreeze.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.amber_700, getTheme())));
        captureImageAndFreeze(onComplete);
        if (flashlightOn && cameraInfo.hasFlashUnit()) {
            mainHandler.postDelayed(() -> cameraControl.enableTorch(false), 1000);
        }
    }

    private void unfreeze() {
        CameraInfo cameraInfo = camera.getCameraInfo();
        CameraControl cameraControl = camera.getCameraControl();
        FloatingActionButton fabFreeze = findViewById(R.id.btnFreeze);

        fabFreeze.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.baseline_lock_open_24, getTheme()));
        fabFreeze.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.amber_200, getTheme())));
        ImageView frozenView = findViewById(R.id.frozenView);
        frozenView.setVisibility(View.GONE);
        if (frozenImage != null) {
            frozenImage.close();
            frozenImage = null;
        }
        if (flashlightOn && cameraInfo.hasFlashUnit()) {
            cameraControl.enableTorch(true);
        }
    }

    private void captureImageAndSave() {
        freeze(() -> saveImage(this::unfreeze));
    }

    private void saveImage() {
        saveImage(null);
    }

    private void saveImage(Runnable onDismiss) {
        ImageView frozenView = findViewById(R.id.frozenView);
        Bitmap bitmap = ((BitmapDrawable) frozenView.getDrawable()).getBitmap();

        String fileName = getNewImageName();

        Uri uri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, getContentValues(fileName));

        if (uri != null) {
            try (OutputStream outputStream = getContentResolver().openOutputStream(uri)) {
                if (outputStream != null) {
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream);
                    showSaveSuccess(fileName, onDismiss);
                } else {
                    throw new IOException("Failed to open output stream.");
                }
            } catch (IOException e) {
                showSaveError(e.getLocalizedMessage(), onDismiss);
            }
        } else {
            mainHandler.post(() -> showAlert(
                    getString(R.string.error),
                    "Failed to create MediaStore entry.",
                    getString(R.string.close),
                    onDismiss));
        }
    }

    private String getNewImageName() {
        return "Magnifier_" + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
    }

    private ContentValues getContentValues(String fileName) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
        contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg");
        contentValues.put(MediaStore.Images.Media.RELATIVE_PATH, "DCIM/Magnifier");
        return contentValues;
    }

    private void showSaveSuccess(String fileName, Runnable onDismiss) {
        mainHandler.post(() -> showAlert(
                getString(R.string.info),
                "File saved to Gallery under 'Magnifier' folder with name '" + fileName + "'.",
                getString(R.string.close),
                onDismiss));
    }

    private void showSaveError(String error, Runnable onDismiss) {
        mainHandler.post(() -> showAlert(
                getString(R.string.error),
                getString(R.string.error_saving, error),
                getString(R.string.close),
                onDismiss));
    }

    private void showAlert(String title, String message, String buttonText, Runnable onDismiss) {
        AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this).create();
        alertDialog.setTitle(title);
        alertDialog.setMessage(message);
        alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, buttonText, (dialog, which) -> {
            dialog.dismiss();
            if (onDismiss != null) {
                onDismiss.run();
            }
        });
        alertDialog.show();
    }
}