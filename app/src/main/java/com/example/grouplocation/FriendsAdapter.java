package com.example.grouplocation;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.grouplocation.utilities.Constants;
import com.example.grouplocation.utilities.PreferenceManager;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;

public class FriendsAdapter extends RecyclerView.Adapter<FriendsAdapter.ViewHolder> {

    // creating a variable for array list and context.
    private ArrayList<FriendsModel> courseModelArrayList;
    private Context context;

    // creating a constructor for our variables.
    public FriendsAdapter(ArrayList<FriendsModel> courseModelArrayList, Context context) {
        this.courseModelArrayList = courseModelArrayList;
        this.context = context;
    }

    // method for filtering our recyclerview items.
    public void filterList(ArrayList<FriendsModel> filterlist) {
        // below line is to add our filtered
        // list in our course array list.
        courseModelArrayList = filterlist;
        // below line is to notify our adapter
        // as change in recycler view data.
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // below line is to inflate our layout.
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.friend_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        // setting data to our views of recycler view.
        PreferenceManager preferenceManager = new PreferenceManager(context);
        FirebaseFirestore dataBase = FirebaseFirestore.getInstance();
        FriendsModel model = courseModelArrayList.get(position);
        holder.courseNameTV.setText(model.getPersonName());
        holder.courseDescTV.setImageBitmap(model.getPersonImage());
        holder.button.setImageDrawable(model.getAddOrRemove());
        holder.button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (model.getType().equals("add")) {
                    dataBase.collection(Constants.KEY_COLLECTION_REQUESTS)
                            .whereEqualTo(Constants.KEY_NAME, preferenceManager.getString(Constants.KEY_NAME))
                            .whereEqualTo(Constants.KEY_USER_REQUESTED, model.getPersonName())
                            .get()
                            .addOnCompleteListener(task -> {
                                if(task.isSuccessful() && task.getResult() != null
                                        && task.getResult().getDocuments().size() > 0) {
                                    dataBase.collection(Constants.KEY_COLLECTION_REQUESTS).document(task.getResult().getDocuments().get(0).getId()).delete();
                                    dataBase.collection(Constants.KEY_COLLECTION_USERS).document(preferenceManager.getString(Constants.KEY_USER_ID))
                                            .update(Constants.KEY_FRIENDS, FieldValue.arrayUnion(model.getPersonName()))
                                            .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                @Override
                                                public void onComplete(@NonNull Task<Void> task) {
                                                    if (task.isSuccessful()) {
                                                        dataBase.collection(Constants.KEY_COLLECTION_USERS)
                                                                .whereEqualTo(Constants.KEY_NAME, model.getPersonName())
                                                                .get()
                                                                .addOnSuccessListener(querySnapshot -> {
                                                                    // Loop through the documents
                                                                    for (DocumentSnapshot document : querySnapshot.getDocuments()) {
                                                                        // Update the field (e.g., update 'status' to 'active')
                                                                        document.getReference().update(Constants.KEY_FRIENDS, FieldValue.arrayUnion(preferenceManager.getString(Constants.KEY_NAME)));
                                                                        holder.button.setImageDrawable(context.getResources().getDrawable(R.drawable.person_remove_24px));
                                                                        model.setType("remove");
                                                                    }
                                                                });
                                                    }
                                                }
                                            });
                                } else if (task.isSuccessful() && task.getResult().getDocuments().size() == 0) {
                                    HashMap<String, Object> user = new HashMap<>();
                                    user.put(Constants.KEY_USER_REQUESTED, preferenceManager.getString(Constants.KEY_NAME));
                                    user.put(Constants.KEY_NAME, model.getPersonName());
                                    dataBase.collection(Constants.KEY_COLLECTION_REQUESTS).document(preferenceManager.getString(Constants.KEY_USER_ID)).set(user);
                                }
                            });

                } else if (model.getType().equals("remove")){
                    dataBase.collection(Constants.KEY_COLLECTION_USERS).document(preferenceManager.getString(Constants.KEY_USER_ID))
                            .update(Constants.KEY_FRIENDS, FieldValue.arrayRemove(model.getPersonName()))
                            .addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    if (task.isSuccessful()) {
                                        dataBase.collection(Constants.KEY_COLLECTION_USERS)
                                                .whereEqualTo(Constants.KEY_NAME, model.getPersonName())
                                                .get()
                                                .addOnSuccessListener(querySnapshot -> {
                                                    // Loop through the documents
                                                    for (DocumentSnapshot document : querySnapshot.getDocuments()) {
                                                        // Update the field (e.g., update 'status' to 'active')
                                                        document.getReference().update(Constants.KEY_FRIENDS, FieldValue.arrayRemove(preferenceManager.getString(Constants.KEY_NAME)));
                                                        holder.button.setImageDrawable(context.getResources().getDrawable(R.drawable.baseline_person_add_alt_24));
                                                        model.setType("add");
                                                    }
                                                });
                                    }
                                }
                            });
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        // returning the size of array list.
        return courseModelArrayList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        // creating variables for our views.
        private final TextView courseNameTV;
        private final ImageView courseDescTV;
        private final ImageView button;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            // initializing our views with their ids.
            courseNameTV = itemView.findViewById(R.id.PersonName);
            courseDescTV = itemView.findViewById(R.id.PersonImage);
            button = itemView.findViewById(R.id.addFriend);
        }
    }
}

