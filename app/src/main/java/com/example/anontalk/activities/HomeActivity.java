package com.example.anontalk.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.example.anontalk.R;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.bottomsheet.BottomSheetDialog;

public class HomeActivity extends AppCompatActivity {

    BottomNavigationView bottomNav;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        bottomNav = findViewById(R.id.bottomNav);

        // Load default fragment
        if (savedInstanceState == null) {
            loadFragment(new FeedFragment());
        }

        bottomNav.setOnItemSelectedListener(item -> {

            int id = item.getItemId();

            if (id == R.id.nav_add) {
                showCreatePopup();
                return false; // Do not switch tab
            }

            Fragment fragment = null;

            if (id == R.id.nav_home) {
                fragment = new FeedFragment();
            } else if (id == R.id.nav_saved) {
                fragment = new SavedFragment();
            } else if (id == R.id.nav_trending) {
                fragment = new TrendingFragment();
            }

            if (fragment != null) {
                loadFragment(fragment);
                return true;
            }

            return false;
        });
    }

    private void loadFragment(@NonNull Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .commit();
    }

    // 🔥 Popup for + button
    private void showCreatePopup() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View view = getLayoutInflater().inflate(R.layout.bottomsheet_create, null);
        dialog.setContentView(view);

        view.findViewById(R.id.btnAddPost).setOnClickListener(v -> {
            dialog.dismiss();
            startActivity(new Intent(this, PostActivity.class));
        });

//        view.findViewById(R.id.btnAddPoll).setOnClickListener(v -> {
//            dialog.dismiss();
//            startActivity(new Intent(this, PollActivity.class));
//        });
//
//        view.findViewById(R.id.btnOpenDiscussion).setOnClickListener(v -> {
//            dialog.dismiss();
//            startActivity(new Intent(this, DiscussionActivity.class));
//        });

        dialog.show();
    }
}
