package com.floatingvolume;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends Activity {

    private static final int OVERLAY_PERMISSION_REQ = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button btn = findViewById(R.id.btnStart);
        TextView status = findViewById(R.id.tvStatus);

        if (Settings.canDrawOverlays(this)) {
            status.setText("Floating knob is active!\nYou can close this screen.");
            startFloatingService();
        } else {
            status.setText("Grant overlay permission to use the floating volume knob.");
        }

        btn.setOnClickListener(v -> {
            if (!Settings.canDrawOverlays(this)) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, OVERLAY_PERMISSION_REQ);
            } else {
                startFloatingService();
                status.setText("Floating knob is active!\nYou can close this screen.");
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == OVERLAY_PERMISSION_REQ) {
            if (Settings.canDrawOverlays(this)) {
                startFloatingService();
                TextView status = findViewById(R.id.tvStatus);
                status.setText("Floating knob is active!\nYou can close this screen.");
            }
        }
    }

    private void startFloatingService() {
        Intent intent = new Intent(this, FloatingVolumeService.class);
        startService(intent);
    }
}
