package com.example.grouplocation;

import android.content.Context;
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
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;

public class friendsMapsAdapter extends RecyclerView.Adapter<friendsMapsAdapter.ViewHolder> {

    private ArrayList<FriendsModel> courseModelArrayList;
    private Context context;

    // creating a constructor for our variables.
    public friendsMapsAdapter(ArrayList<FriendsModel> courseModelArrayList, Context context) {
        this.courseModelArrayList = courseModelArrayList;
        this.context = context;
    }

    // method for filtering our recyclerview items.

    @NonNull
    @Override
    public friendsMapsAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // below line is to inflate our layout.
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.friends_maps_item, parent, false);
        return new friendsMapsAdapter.ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull friendsMapsAdapter.ViewHolder holder, int position) {
        // setting data to our views of recycler view.
        FriendsModel model = courseModelArrayList.get(position);
        holder.courseNameTV.setText(model.getPersonName());
        holder.courseDescTV.setImageBitmap(model.getPersonImage());
        holder.button.setImageDrawable(model.getAddOrRemove());
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
