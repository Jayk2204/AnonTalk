package com.example.anontalk.activities;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.anontalk.R;
import com.example.anontalk.adapters.PostAdapter;
import com.example.anontalk.models.PostModel;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

public class HomeActivity extends AppCompatActivity {

    private RecyclerView rvPosts;
    private PostAdapter adapter;
    private List<PostModel> list;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        rvPosts = findViewById(R.id.rvPosts);
        rvPosts.setLayoutManager(new LinearLayoutManager(this));

        list = new ArrayList<>();
        adapter = new PostAdapter(list);
        rvPosts.setAdapter(adapter);

        loadPosts();
        FloatingActionButton fabAddPost = findViewById(R.id.fabAddPost);

        fabAddPost.setOnClickListener(v -> {
            startActivity(new Intent(HomeActivity.this, PostActivity.class));
        });

    }

    private void loadPosts() {
        FirebaseFirestore.getInstance()
                .collection("posts")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null || value == null) return;

                    list.clear();
                    for (var doc : value.getDocuments()) {
                        PostModel post = doc.toObject(PostModel.class);
                        if (post != null) {
                            post.setId(doc.getId());   // 🔥 SET DOCUMENT ID
                            list.add(post);
                        }
                    }
                    adapter.notifyDataSetChanged();

                });

    }

}
