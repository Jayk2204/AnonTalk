package com.example.anontalk.activities;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.anontalk.R;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PollActivity extends AppCompatActivity {

    private static final int MAX_OPTIONS = 6;

    private EditText etQuestion;
    private LinearLayout optionsContainer;
    private Button btnCreatePoll;
    private TextView btnAddOption;

    private FirebaseFirestore db;
    private FirebaseAuth auth;

    private final List<EditText> optionInputs = new ArrayList<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_poll);

        etQuestion = findViewById(R.id.etQuestion);
        optionsContainer = findViewById(R.id.optionsContainer);
        btnAddOption = findViewById(R.id.btnAddOption);
        btnCreatePoll = findViewById(R.id.btnCreatePoll);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        // Default 2 options (UX standard)
        addOptionField();
        addOptionField();

        btnAddOption.setOnClickListener(v -> {
            if (optionInputs.size() >= MAX_OPTIONS) {
                Toast.makeText(this, "Maximum " + MAX_OPTIONS + " options allowed", Toast.LENGTH_SHORT).show();
            } else {
                addOptionField();
            }
        });

        btnCreatePoll.setOnClickListener(v -> createPoll());
    }

    // =======================
    // ➕ ADD OPTION (UI POLISH)
    // =======================
    private void addOptionField() {

        EditText option = new EditText(this);
        option.setHint("Option " + (optionInputs.size() + 1));
        option.setHintTextColor(getColor(android.R.color.darker_gray));
        option.setTextColor(getColor(android.R.color.white));
        option.setBackground(null);
        option.setPadding(0, 20, 0, 20);
        option.setTextSize(15f);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 12, 0, 0);
        option.setLayoutParams(params);

        optionsContainer.addView(option);
        optionInputs.add(option);
    }

    // =======================
    // 🗳 CREATE POLL (UNCHANGED LOGIC)
    // =======================
    private void createPoll() {

        String question = etQuestion.getText().toString().trim();
        if (TextUtils.isEmpty(question)) {
            Toast.makeText(this, "Enter poll question", Toast.LENGTH_SHORT).show();
            return;
        }

        List<String> options = new ArrayList<>();
        for (EditText et : optionInputs) {
            String text = et.getText().toString().trim();
            if (!TextUtils.isEmpty(text)) {
                options.add(text);
            }
        }

        if (options.size() < 2) {
            Toast.makeText(this, "Add at least two options", Toast.LENGTH_SHORT).show();
            return;
        }

        String uid = auth.getCurrentUser() != null
                ? auth.getCurrentUser().getUid()
                : "anonymous";

        // ⏰ Expire after 24 hours
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.HOUR, 24);
        Timestamp expiresAt = new Timestamp(cal.getTime());

        Map<String, Object> pollMap = new HashMap<>();
        pollMap.put("question", question);
        pollMap.put("createdBy", uid);
        pollMap.put("createdAt", Timestamp.now());
        pollMap.put("expiresAt", expiresAt);
        pollMap.put("totalVotes", 0);

        db.collection("polls")
                .add(pollMap)
                .addOnSuccessListener(pollRef -> {

                    for (String opt : options) {
                        Map<String, Object> optMap = new HashMap<>();
                        optMap.put("text", opt);
                        optMap.put("votes", 0);
                        pollRef.collection("options").add(optMap);
                    }

                    Toast.makeText(this, "Poll created successfully", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }

    // =======================
    // 🔐 ONE VOTE PER USER (UNCHANGED)
    // =======================
    public void voteOnPoll(String pollId, String optionId) {

        String uid = auth.getCurrentUser().getUid();

        DocumentReference voteRef = db.collection("polls")
                .document(pollId)
                .collection("votes")
                .document(uid);

        voteRef.get().addOnSuccessListener(snapshot -> {
            if (snapshot.exists()) {
                Toast.makeText(this, "You have already voted", Toast.LENGTH_SHORT).show();
            } else {

                Map<String, Object> voteMap = new HashMap<>();
                voteMap.put("optionId", optionId);
                voteMap.put("votedAt", Timestamp.now());

                voteRef.set(voteMap);

                DocumentReference optionRef = db.collection("polls")
                        .document(pollId)
                        .collection("options")
                        .document(optionId);

                DocumentReference pollRef = db.collection("polls").document(pollId);

                db.runTransaction(transaction -> {

                    DocumentSnapshot optionSnap = transaction.get(optionRef);
                    Long currentVotes = optionSnap.getLong("votes");
                    if (currentVotes == null) currentVotes = 0L;

                    transaction.update(optionRef, "votes", currentVotes + 1);

                    DocumentSnapshot pollSnap = transaction.get(pollRef);
                    Long totalVotes = pollSnap.getLong("totalVotes");
                    if (totalVotes == null) totalVotes = 0L;

                    transaction.update(pollRef, "totalVotes", totalVotes + 1);

                    return null;
                }).addOnSuccessListener(unused ->
                        Toast.makeText(this, "Vote submitted", Toast.LENGTH_SHORT).show()
                ).addOnFailureListener(e ->
                        Toast.makeText(this, "Vote failed: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
            }
        });
    }

    // =======================
    // ⛔ POLL EXPIRY CHECK (UNCHANGED)
    // =======================
    public boolean isPollExpired(Timestamp expiresAt) {
        return Timestamp.now().compareTo(expiresAt) > 0;
    }
}
