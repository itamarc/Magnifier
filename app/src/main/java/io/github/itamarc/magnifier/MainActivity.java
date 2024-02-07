package io.github.itamarc.magnifier;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;

import android.content.pm.PackageManager;
import android.os.Bundle;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.ExecutionException;

public class MainActivity extends AppCompatActivity {
    private boolean flashlightOn = false;
    private boolean imageFrozen = false;
    private Camera camera;
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        checkPermissions();

        // Camera setup
        cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                bindPreview(cameraProvider);
            } catch (ExecutionException | InterruptedException e) {
                // No errors need to be handled for this Future.
                // This should never be reached.
            }
        }, ContextCompat.getMainExecutor(this));

        setUpButtons();
    }

    private void setUpButtons() {
        FloatingActionButton fabFreeze = findViewById(R.id.btnFreeze);
        fabFreeze.setOnClickListener(v -> {
            if (imageFrozen) {
                fabFreeze.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.baseline_lock_open_24, getTheme()));
                // TODO unfreeze image
                imageFrozen = false;
            } else {
                fabFreeze.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.baseline_lock_24, getTheme()));
                // TODO freeze image
                imageFrozen = true;
            }
        });

        FloatingActionButton fabFlashlight = findViewById(R.id.btnFlashlight);
        fabFlashlight.setOnClickListener(v -> {
            if (flashlightOn) {
                fabFlashlight.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.baseline_flashlight_off_24, getTheme()));
                flashlightOn = false;
                camera.getCameraControl().enableTorch(flashlightOn);
            } else {
                fabFlashlight.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.baseline_flashlight_on_24, getTheme()));
                flashlightOn = true;
                camera.getCameraControl().enableTorch(flashlightOn);
            }
        });

        FloatingActionButton fabExit = findViewById(R.id.btnExit);
        fabExit.setOnClickListener(v -> System.exit(0));
    }

    private void checkPermissions() {
        int CAMERA_STORAGE_PERMISSION_REQUEST_CODE = 1;
//        String[] permissions = {Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE};
        String[] permissions = {"android.permission.CAMERA", "android.permission.WRITE_EXTERNAL_STORAGE"};
        boolean allGranted = true;
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                allGranted = false;
                break;
            }
        }

        if (!allGranted) {
            ActivityCompat.requestPermissions(this, permissions, CAMERA_STORAGE_PERMISSION_REQUEST_CODE);
        }
    }

    void bindPreview(@NonNull ProcessCameraProvider cameraProvider) {
        PreviewView camView = findViewById(R.id.cameraView);

        Preview preview = new Preview.Builder().build();

        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build();

        camView.setScaleType(PreviewView.ScaleType.FILL_CENTER);
        preview.setSurfaceProvider(camView.getSurfaceProvider());
        camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview);
    }
}