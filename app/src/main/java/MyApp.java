package com.example.anontalk;

import android.app.Application;

import com.vanniktech.emoji.EmojiManager;
import com.vanniktech.emoji.google.GoogleEmojiProvider;

public class MyApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        // ✅ Install emoji provider (REQUIRED)
        EmojiManager.install(new GoogleEmojiProvider());
    }
}
