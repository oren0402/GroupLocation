package com.example.grouplocation;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

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

public class friendsRecycler extends Fragment {

    private ArrayList<FriendsModel> friendsModelArrayList;
    private CollectionReference collectionReference;
    private PreferenceManager preferenceManager;
    private friendsMapsAdapter adapter;
    private RecyclerView friendsRecycler;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_friends_recycler, container, false);

        FirebaseFirestore dataBase = FirebaseFirestore.getInstance();
        preferenceManager = new PreferenceManager(getActivity());
        friendsRecycler = rootView.findViewById(R.id.idFriendsList);
        collectionReference = dataBase.collection(Constants.KEY_COLLECTION_USERS);
        buildRecyclerView();
        // Inflate the layout for this fragment
        return rootView;
    }

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
                                                            friendsModelArrayList.add(new FriendsModel(name, getCircularBitmap(bitmap), getResources().getDrawable(R.drawable.baseline_person_add_alt_24), "none"));
                                                        }
                                                    }
                                                }

                                                adapter = new friendsMapsAdapter(friendsModelArrayList, getActivity());

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