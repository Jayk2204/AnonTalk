package com.example.anontalk.activities;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.anontalk.R;
import com.example.anontalk.adapters.PostAdapter;
import com.example.anontalk.models.PostModel;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class FeedFragment extends Fragment {

    RecyclerView recyclerView;
    PostAdapter postAdapter;
    List<PostModel> postList;
    FirebaseFirestore db;

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        View view = inflater.inflate(R.layout.fragment_feed, container, false);

        recyclerView = view.findViewById(R.id.postRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        postList = new ArrayList<>();
        postAdapter = new PostAdapter(requireContext(), postList);
        recyclerView.setAdapter(postAdapter);

        db = FirebaseFirestore.getInstance();

        loadPosts();

        return view;
    }

    private void loadPosts() {
        db.collection("Posts")   // 🔥 FIXED: correct collection
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {

                    if (error != null || value == null) return;

                    postList.clear();

                    for (QueryDocumentSnapshot doc : value) {
                        PostModel model = doc.toObject(PostModel.class);
                        model.setPostId(doc.getId());
                        postList.add(model);
                    }

                    postAdapter.notifyDataSetChanged();
                });
    }

}
