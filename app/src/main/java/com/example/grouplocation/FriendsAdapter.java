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

import java.util.ArrayList;

public class FriendsAdapter extends RecyclerView.Adapter<FriendsAdapter.ViewHolder> {

    // creating a variable for array list and context.
    private ArrayList<FriendsModel> courseModelArrayList;
    private Context context;
    private Boolean addOrRemoveBoolean;

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
        FriendsModel model = courseModelArrayList.get(position);
        holder.courseNameTV.setText(model.getPersonName());
        holder.courseDescTV.setImageBitmap(model.getPersonImage());
        holder.button.setImageDrawable(model.getAddOrRemove());
        addOrRemoveBoolean = model.getAddOrRemoveBool();
        holder.button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (addOrRemoveBoolean) {
                    Log.d("dicky5", "add");
                } else {
                    Log.d("dicky5", "remove");
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

