package com.example.anontalk.activities;

import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.example.anontalk.R;
import com.google.firebase.database.collection.BuildConfig;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.File;

public class SplashActivity extends AppCompatActivity {

    private Handler handler;
    private ImageView icon;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        icon = findViewById(R.id.imgSplashIcon);
        handler = new Handler(Looper.getMainLooper());

        // 🔥 First check for update
        checkForUpdate();
    }

    // ==========================================
    // 🔄 FIRESTORE UPDATE CHECK
    // ==========================================
    private void checkForUpdate() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("app_update")
                .document("latest")
                .get()
                .addOnSuccessListener(documentSnapshot -> {

                    if (!documentSnapshot.exists()) {
                        startSplashAnimation();
                        return;
                    }

                    long latestVersion = documentSnapshot.getLong("versionCode");
                    String apkUrl = documentSnapshot.getString("apkUrl");
                    boolean force = Boolean.TRUE.equals(documentSnapshot.getBoolean("force"));
                    String changelog = documentSnapshot.getString("changelog");

                    int currentVersion = BuildConfig.VERSION_CODE;

                    if (latestVersion > currentVersion) {
                        showUpdateDialog(apkUrl, force, changelog);
                    } else {
                        startSplashAnimation();
                    }
                })
                .addOnFailureListener(e -> {
                    // Agar Firestore fail ho jaye to bhi app chale
                    startSplashAnimation();
                });
    }

    // ==========================================
    // 🔔 UPDATE DIALOG
    // ==========================================
    private void showUpdateDialog(String apkUrl, boolean force, String changelog) {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Update Required");
        builder.setMessage("You must update AnonTalk to continue.\n\nWhat's new:\n" + changelog);
        builder.setCancelable(false);   // 🔒 User back press nahi kar sakta

        builder.setPositiveButton("Update Now", (dialog, which) -> {
            downloadAndInstallApk(apkUrl);
        });

        // ❌ No "Later" button when force = true
        builder.show();
    }


    // ==========================================
    // ⬇️ DOWNLOAD APK
    // ==========================================
    private void downloadAndInstallApk(String apkUrl) {
        try {
            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(apkUrl));
            request.setTitle("AnonTalk Update");
            request.setDescription("Downloading latest version...");
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "AnonTalk_Update.apk");

            DownloadManager manager = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
            long downloadId = manager.enqueue(request);

            BroadcastReceiver receiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
                    if (downloadId == id) {
                        installApk();
                        unregisterReceiver(this);
                    }
                }
            };

            registerReceiver(receiver, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));

        } catch (Exception e) {
            Toast.makeText(this, "Download failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    // ==========================================
    // 📦 INSTALL APK
    // ==========================================
    private void installApk() {
        File file = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOWNLOADS), "AnonTalk_Update.apk");

        if (!file.exists()) {
            Toast.makeText(this, "APK not found!", Toast.LENGTH_SHORT).show();
            return;
        }

        Uri apkUri = FileProvider.getUriForFile(
                this,
                getPackageName() + ".provider",
                file
        );

        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(apkUri, "application/vnd.android.package-archive");
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(intent);
    }

    // ==========================================
    // 🎬 YOUR ORIGINAL SPLASH ANIMATION (UNCHANGED)
    // ==========================================
    private void startSplashAnimation() {

        // Story icons (order matters)
        int[] icons = {
                R.drawable.ic_talk,          // Talk
                R.drawable.ic_conffesion,    // Confession
                R.drawable.ic_dark_humor,    // Dark humor
                R.drawable.ic_problem,       // Real-life problems
                R.drawable.ic_splash_logo    // Final AnonTalk logo
        };

        Animation anim = AnimationUtils.loadAnimation(this, R.anim.fade_scale);

        long stepDelay = 400; // ms between each symbol

        for (int i = 0; i < icons.length; i++) {
            int index = i;
            handler.postDelayed(() -> {
                icon.setImageResource(icons[index]);
                icon.setAlpha(1f);
                icon.startAnimation(anim);
            }, i * stepDelay);
        }

        // Move to Auth after full story
        handler.postDelayed(() -> {
            startActivity(new Intent(
                    SplashActivity.this,
                    AuthActivity.class
            ));
            finish();
        }, icons.length * stepDelay + 200);
    }

}
