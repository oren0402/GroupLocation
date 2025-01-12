package com.example.grouplocation;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import androidx.appcompat.widget.SearchView;
import android.widget.Toast;
import androidx.appcompat.widget.Toolbar;

import com.example.grouplocation.utilities.Constants;
import com.example.grouplocation.utilities.PreferenceManager;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

public class AddFriends extends Fragment {

    private PreferenceManager preferenceManager;

    private CollectionReference collectionReference;

    private RecyclerView friendsRecycler;
    private Toolbar toolbar;

    private FriendsAdapter adapter;
    private ArrayList<FriendsModel> friendsModelArrayList;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        setHasOptionsMenu(true);
        View rootView = inflater.inflate(R.layout.fragment_add_friends, container, false);

        preferenceManager = new PreferenceManager(getActivity());

        FirebaseFirestore dataBase = FirebaseFirestore.getInstance();
        collectionReference = dataBase.collection(Constants.KEY_COLLECTION_USERS);

        friendsRecycler = rootView.findViewById(R.id.idFriendsList);
        toolbar = rootView.findViewById(R.id.toolbar);

        // Set the Toolbar as the ActionBar
        if (getActivity() instanceof AppCompatActivity) {
            AppCompatActivity activity = (AppCompatActivity) getActivity();
            activity.setSupportActionBar(toolbar);
        }

        // calling method to build recycler view
        buildRecyclerView();

        return rootView;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, @NonNull MenuInflater inflater) {
        // get the MenuInflater
        inflater.inflate(R.menu.search_menu, menu);

        // get the search menu item
        MenuItem searchItem = menu.findItem(R.id.actionSearch);

        // get the SearchView from the menu item
        SearchView searchView = (SearchView) searchItem.getActionView();

        // set the on query text listener for the SearchView
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                // Perform search on submit
                filter(query);
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                // Update the filter as the text changes
                filter(newText);
                return false;
            }
        });

        super.onCreateOptionsMenu(menu, inflater);
    }

    // method to filter data based on query
    private void filter(String text) {
        // creating a new array list to filter data
        ArrayList<FriendsModel> filteredlist = new ArrayList<>();

        // running a for loop to compare elements
        for (FriendsModel item : friendsModelArrayList) {
            // checking if the entered string matches any item of our recycler view
            if (item.getPersonName().toLowerCase().contains(text.toLowerCase())) {
                // adding matched item to the filtered list
                filteredlist.add(item);
            }
        }

        if (filteredlist.isEmpty()) {
            // displaying a toast message if no data found
        } else {
            // passing the filtered list to the adapter class
            adapter.filterList(filteredlist);
        }
    }

    // method to build RecyclerView
    private void buildRecyclerView() {
        // creating a new array list
        friendsModelArrayList = new ArrayList<>();

        DocumentReference documentReference = collectionReference.document(preferenceManager.getString(Constants.KEY_USER_ID));
        documentReference.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task1) {
                if (task1.isSuccessful()) {
                    DocumentSnapshot document1 = task1.getResult();
                    if (document1.exists()) {
                        collectionReference.get()
                                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                                    @Override
                                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                                        if (task.isSuccessful()) {
                                            QuerySnapshot querySnapshot = task.getResult();
                                            if (querySnapshot != null) {
                                                List<String> friends = (List<String>) document1.get(Constants.KEY_FRIENDS);
                                                if (friends == null) {
                                                    friends = new ArrayList<>();
                                                }
                                                // Loop over the documents
                                                for (DocumentSnapshot document : querySnapshot.getDocuments()) {
                                                    String name = document.getString(Constants.KEY_NAME);
                                                    if (!name.equals(preferenceManager.getString(Constants.KEY_NAME))) {
                                                        byte[] bytes = Base64.decode(document.getString(Constants.KEY_IMAGE_BIG), Base64.DEFAULT);
                                                        Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                                                        if (friends.contains(name)) {
                                                            friendsModelArrayList.add(new FriendsModel(name, getCircularBitmap(bitmap), getResources().getDrawable(R.drawable.person_remove_24px), false));
                                                        } else {
                                                            friendsModelArrayList.add(new FriendsModel(name, getCircularBitmap(bitmap), getResources().getDrawable(R.drawable.baseline_person_add_alt_24), true));
                                                        }
                                                    }
                                                }

                                                adapter = new FriendsAdapter(friendsModelArrayList, getActivity());

                                                // adding layout manager to the RecyclerView
                                                LinearLayoutManager manager = new LinearLayoutManager(getActivity());
                                                friendsRecycler.setHasFixedSize(true);

                                                // setting layout manager to the RecyclerView
                                                friendsRecycler.setLayoutManager(manager);

                                                // setting adapter to the RecyclerView
                                                friendsRecycler.setAdapter(adapter);
                                            }
                                        }
                                    }
                                });

                    }
                }
            }
        });
    }

    protected Bitmap getCircularBitmap(Bitmap srcBitmap) {
        // Calculate the circular bitmap width with border
        int squareBitmapWidth = Math.min(srcBitmap.getWidth(), srcBitmap.getHeight());
        // Initialize a new instance of Bitmap
        Bitmap dstBitmap = Bitmap.createBitmap (
                squareBitmapWidth, // Width
                squareBitmapWidth, // Height
                Bitmap.Config.ARGB_8888 // Config
        );
        Canvas canvas = new Canvas(dstBitmap);
        // Initialize a new Paint instance
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        Rect rect = new Rect(0, 0, squareBitmapWidth, squareBitmapWidth);
        RectF rectF = new RectF(rect);
        canvas.drawOval(rectF, paint);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        // Calculate the left and top of copied bitmap
        float left = (squareBitmapWidth-srcBitmap.getWidth())/2;
        float top = (squareBitmapWidth-srcBitmap.getHeight())/2;
        canvas.drawBitmap(srcBitmap, left, top, paint);
        // Free the native object associated with this bitmap.
        srcBitmap.recycle();
        // Return the circular bitmap
        return dstBitmap;
    }
}