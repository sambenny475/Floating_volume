package com.floatingvolume;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.TextView;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

public class MainActivity extends Activity {

    private ActivityResultLauncher<Intent> permissionLauncher;
    private TextView statusView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button btn = findViewById(R.id.btnStart);
        statusView = findViewById(R.id.tvStatus);

        // Register the activity result launcher for overlay permission
        permissionLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (Settings.canDrawOverlays(MainActivity.this)) {
                        startFloatingService();
                        statusView.setText("Floating knob is active!\nYou can close this screen.");
                    }
                }
        );

        // Check initial permission state
        if (Settings.canDrawOverlays(this)) {
            statusView.setText("Floating knob is active!\nYou can close this screen.");
            startFloatingService();
        } else {
            statusView.setText("Grant overlay permission to use the floating volume knob.");
        }

        btn.setOnClickListener(v -> {
            if (!Settings.canDrawOverlays(MainActivity.this)) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + getPackageName()));
                permissionLauncher.launch(intent);
            } else {
                startFloatingService();
                statusView.setText("Floating knob is active!\nYou can close this screen.");
            }
        });
    }

    private void startFloatingService() {
        Intent intent = new Intent(this, FloatingVolumeService.class);
        startService(intent);
    }
}
