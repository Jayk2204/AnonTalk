package com.example.anontalk.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.Spannable;
import android.text.TextWatcher;
import android.text.style.ForegroundColorSpan;
import android.util.Base64;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.anontalk.R;
import com.example.anontalk.adapters.PostImageAdapter;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.vanniktech.emoji.EmojiPopup;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class PostActivity extends AppCompatActivity {

    private static final int PICK_IMAGES = 101;
    private static final int MAX_CHARS = 500;
    private static final String DRAFT_PREF = "post_draft";

    TextInputEditText etPostText;
    TextView tvCounter, btnPost;
    ImageView btnBack, btnAddImage, btnEmoji;
    RecyclerView rvImages;

    ArrayList<Uri> imageList = new ArrayList<>();
    PostImageAdapter imageAdapter;

    FirebaseFirestore db;
    FirebaseAuth auth;
    SharedPreferences prefs;
    EmojiPopup emojiPopup;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_post);

        etPostText = findViewById(R.id.etPostText);
        tvCounter = findViewById(R.id.tvCounter);
        btnPost = findViewById(R.id.btnPost);
        btnBack = findViewById(R.id.btnBack);
        btnAddImage = findViewById(R.id.btnAddImage);
        btnEmoji = findViewById(R.id.btnEmoji);
        rvImages = findViewById(R.id.rvImages);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        prefs = getSharedPreferences(DRAFT_PREF, MODE_PRIVATE);

        setupRecycler();
        setupEmoji();
        loadDraft();
        setupTextWatcher();

        btnAddImage.setOnClickListener(v -> openGallery());
        btnBack.setOnClickListener(v -> finish());
        btnPost.setOnClickListener(v -> uploadPost());
    }

    private void setupRecycler() {
        rvImages.setLayoutManager(
                new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        imageAdapter = new PostImageAdapter(this, imageList);
        rvImages.setAdapter(imageAdapter);
    }

    private void setupEmoji() {
        View rootView = findViewById(R.id.rootLayout);
        emojiPopup = new EmojiPopup(rootView, etPostText);

        btnEmoji.setOnClickListener(v -> {
            if (emojiPopup.isShowing()) {
                emojiPopup.dismiss();
            } else {
                emojiPopup.show();
            }
        });
    }

    private void setupTextWatcher() {
        etPostText.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void afterTextChanged(Editable s) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                tvCounter.setText(s.length() + "/" + MAX_CHARS);
                highlightHashtags(etPostText.getText());
                saveDraft();
            }
        });
    }

    private void highlightHashtags(Editable editable) {
        try {
            ForegroundColorSpan[] spans = editable.getSpans(0, editable.length(), ForegroundColorSpan.class);
            for (ForegroundColorSpan span : spans) {
                editable.removeSpan(span);
            }

            String text = editable.toString();
            int index = 0;
            String[] words = text.split("\\s+");

            for (String word : words) {
                if (word.startsWith("#")) {
                    int start = index;
                    int end = index + word.length();

                    if (start >= 0 && end <= editable.length()) {
                        editable.setSpan(
                                new ForegroundColorSpan(getColor(R.color.teal_700)),
                                start,
                                end,
                                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                        );
                    }
                }
                index += word.length() + 1;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        startActivityForResult(intent, PICK_IMAGES);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGES && resultCode == RESULT_OK && data != null) {
            if (data.getClipData() != null) {
                int count = data.getClipData().getItemCount();
                for (int i = 0; i < count; i++) {
                    Uri uri = data.getClipData().getItemAt(i).getUri();
                    imageList.add(uri);
                }
            } else if (data.getData() != null) {
                imageList.add(data.getData());
            }
            imageAdapter.notifyDataSetChanged();
        }
    }

    // 🔥 MAIN POST LOGIC
    private void uploadPost() {
        String text = etPostText.getText().toString().trim();

        if (text.isEmpty()) {
            Toast.makeText(this, "Write something first", Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(this, "Uploading...", Toast.LENGTH_SHORT).show();

        if (imageList.isEmpty()) {
            savePost(text, new ArrayList<>());
        } else {
            uploadImages(text);
        }
    }

    // 🔥 MULTI IMAGE UPLOAD
    private void uploadImages(String text) {
        ArrayList<String> imageUrls = new ArrayList<>();

        for (int i = 0; i < imageList.size(); i++) {
            Uri uri = imageList.get(i);

            try {
                InputStream inputStream = getContentResolver().openInputStream(uri);
                byte[] bytes = getBytes(inputStream);
                String base64Image = Base64.encodeToString(bytes, Base64.DEFAULT);

                uploadToImgbb(base64Image, imageUrls, text);

            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(this, "Image read failed", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // 🔥 SAVE TO FIRESTORE (FIXED FOR NOTIFICATIONS + RULES)
    private void savePost(String text, ArrayList<String> imageUrls) {

        if (auth.getCurrentUser() == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        HashMap<String, Object> map = new HashMap<>();
        map.put("text", text);
        map.put("images", imageUrls);
        map.put("timestamp", System.currentTimeMillis());
        map.put("likeCount", 0);
        map.put("commentCount", 0);
        map.put("userId", auth.getCurrentUser().getUid()); // 🔥 REQUIRED

        db.collection("posts")
                .add(map)
                .addOnSuccessListener(documentReference -> {
                    clearDraft();
                    Toast.makeText(this, "Post uploaded successfully", Toast.LENGTH_SHORT).show();

                    Intent intent = new Intent(PostActivity.this, HomeActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Post failed: " + e.getMessage(), Toast.LENGTH_LONG).show()
                );
    }

    private void saveDraft() {
        prefs.edit().putString("text", etPostText.getText().toString()).apply();
    }

    private void loadDraft() {
        String draft = prefs.getString("text", "");
        if (!draft.isEmpty()) {
            etPostText.setText(draft);
        }
    }

    private void clearDraft() {
        prefs.edit().clear().apply();
    }

    // 🔥 IMGBB UPLOAD
    private void uploadToImgbb(String base64Image, ArrayList<String> imageUrls, String text) {

        String apiKey = "ede61915513be11f40d19070b26786f2"; // your key

        RequestBody body = new FormBody.Builder()
                .add("key", apiKey)
                .add("image", base64Image)
                .build();

        Request request = new Request.Builder()
                .url("https://api.imgbb.com/1/upload")
                .post(body)
                .build();

        OkHttpClient client = new OkHttpClient();
        client.newCall(request).enqueue(new Callback() {

            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() ->
                        Toast.makeText(PostActivity.this, "Image upload failed", Toast.LENGTH_SHORT).show()
                );
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    String res = response.body().string();
                    JSONObject json = new JSONObject(res);
                    String imageUrl = json.getJSONObject("data").getString("url");

                    imageUrls.add(imageUrl);

                    if (imageUrls.size() == imageList.size()) {
                        runOnUiThread(() -> savePost(text, imageUrls));
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private byte[] getBytes(InputStream inputStream) throws IOException {
        ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();
        int bufferSize = 1024;
        byte[] buffer = new byte[bufferSize];

        int len;
        while ((len = inputStream.read(buffer)) != -1) {
            byteBuffer.write(buffer, 0, len);
        }
        return byteBuffer.toByteArray();
    }
}
