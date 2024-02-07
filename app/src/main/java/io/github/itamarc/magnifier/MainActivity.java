package io.github.itamarc.magnifier;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.res.ResourcesCompat;

import android.os.Bundle;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class MainActivity extends AppCompatActivity {
    private boolean flashlightOn = false;
    private boolean imageFrozen = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        FloatingActionButton fabFreeze = findViewById(R.id.btnFreeze);
        fabFreeze.setOnClickListener(v -> {
            // android:src="@drawable/baseline_flashlight_off_24" />
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
            // android:src="@drawable/baseline_flashlight_off_24" />
            if (flashlightOn) {
                fabFlashlight.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.baseline_flashlight_off_24, getTheme()));
                // TODO turn flashlight off
                flashlightOn = false;
            } else {
                fabFlashlight.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.baseline_flashlight_on_24, getTheme()));
                // TODO turn flashlight on
                flashlightOn = true;
            }
        });

        FloatingActionButton fabExit = findViewById(R.id.btnExit);
        fabExit.setOnClickListener(v -> System.exit(0));
    }
}