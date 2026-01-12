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
import com.example.anontalk.models.FeedItem;
import com.example.anontalk.models.PollModel;
import com.example.anontalk.models.PostModel;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FeedFragment extends Fragment {

    RecyclerView recyclerView;
    PostAdapter postAdapter;
    List<FeedItem> feedList;
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

        feedList = new ArrayList<>();
        postAdapter = new PostAdapter(requireContext(), feedList);
        recyclerView.setAdapter(postAdapter);

        db = FirebaseFirestore.getInstance();

        loadPostsAndPolls();

        return view;
    }

    private void loadPostsAndPolls() {

        // 🔥 LOAD POSTS
        db.collection("posts")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((postSnap, error) -> {

                    if (error != null || postSnap == null) return;

                    feedList.clear();

                    for (QueryDocumentSnapshot doc : postSnap) {
                        PostModel model = doc.toObject(PostModel.class);
                        model.setPostId(doc.getId());

                        // Wrap PostModel inside FeedItem
                        feedList.add(new FeedItem("POST", model, null));
                    }

                    loadPolls(); // load polls after posts
                });
    }

    // 🔥 LOAD POLLS
    private void loadPolls() {

        db.collection("polls")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener((pollSnap, error) -> {

                    if (error != null || pollSnap == null) return;

                    // Remove old polls to avoid duplication
                    List<FeedItem> onlyPosts = new ArrayList<>();
                    for (FeedItem item : feedList) {
                        if (item.getType().equals("POST")) {
                            onlyPosts.add(item);
                        }
                    }

                    feedList.clear();
                    feedList.addAll(onlyPosts);

                    for (QueryDocumentSnapshot doc : pollSnap) {
                        PollModel poll = doc.toObject(PollModel.class);
                        poll.setPollId(doc.getId());

                        feedList.add(new FeedItem("POLL", null, poll));
                    }

                    // 🔃 SORT BY TIMESTAMP (POST + POLL)
                    Collections.sort(feedList, (o1, o2) -> {
                        if (o1.getTimestamp() == null || o2.getTimestamp() == null) return 0;
                        return o2.getTimestamp().compareTo(o1.getTimestamp());
                    });

                    postAdapter.notifyDataSetChanged();
                });
    }
}
