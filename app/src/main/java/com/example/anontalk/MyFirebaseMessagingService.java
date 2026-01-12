package com.example.anontalk;

import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.HashMap;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    @Override
    public void onNewToken(String token) {
        super.onNewToken(token);
        saveToken(token);
    }

    private void saveToken(String token) {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;

        HashMap<String, Object> map = new HashMap<>();
        map.put("fcmToken", token);

        FirebaseFirestore.getInstance()
                .collection("users")
                .document(uid)
                .set(map, com.google.firebase.firestore.SetOptions.merge());
    }

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);
        // Firebase automatically shows notification if payload contains "notification"
    }
}
