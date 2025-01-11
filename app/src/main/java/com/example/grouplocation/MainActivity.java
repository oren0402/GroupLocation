package com.example.grouplocation;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.view.MotionEvent;
import android.Manifest;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import com.example.grouplocation.activities.MapsActivity;
import com.example.grouplocation.activities.SignInActivity;
import com.example.grouplocation.utilities.Constants;
import com.example.grouplocation.utilities.PreferenceManager;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.makeramen.roundedimageview.RoundedImageView;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Random;

public class MainActivity extends AppCompatActivity implements NewGroup.OnFragmentInteractionListener{

    private PreferenceManager preferenceManager;

    private ProgressBar progressBar;

    private static final int REQUEST_CODE_PERMISSION = 1;

    private ConstraintLayout constraintLayout;

    private TextView textView;

    Uri imageUri;

    private DocumentReference docRef;

    private RoundedImageView image;

    private String encodedImage;
    private String encodedImageBig;
    private View fragment;

    private Button button;

    private ImageView imageProfile;

    private FusedLocationProviderClient fusedLocationClient;


    private View parentLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        preferenceManager = new PreferenceManager(this);
        image = findViewById(R.id.imageProfile);
        progressBar = findViewById(R.id.progressBar1);
        parentLayout = findViewById(R.id.constraint);
        textView = findViewById(R.id.textName);
        fragment = findViewById(R.id.fragmentView);
        constraintLayout = findViewById(R.id.constraint);
        button = findViewById(R.id.newGroup);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        // Check for permissions and get location
        if (!(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED)) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_CODE_PERMISSION);
        }
        loadUserDetails();
        setListeners();
        checkIfGroup();
    }

    @SuppressLint("ClickableViewAccessibility")
    private void setListeners() {
        image.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                pickImage.launch(intent);
            }
        });
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                fragment.setVisibility(View.VISIBLE);
                NewGroup fragment = new NewGroup();
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragmentView, fragment)
                        .commit();

            }
        });
        findViewById(R.id.imageSignOut).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                preferenceManager.clear();
                preferenceManager.putBoolean("Not First Time", true);
                Intent serviceIntent = new Intent(getApplicationContext(), LocationService.class);
                stopService(serviceIntent);
                startActivity(new Intent(getApplicationContext(), SignInActivity.class));
                finish();
            }
        });
        parentLayout.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.fragmentView);
                if (fragment != null) {
                    // Get touch coordinates
                    float x = event.getX();
                    float y = event.getY();

                    // Check if the touch is outside the fragment container
                    if (!isPointInsideView(x, y, findViewById(R.id.fragmentView))) {
                        // Handle the click outside of the FragmentContainerView
                        findViewById(R.id.fragmentView).setVisibility(View.GONE);
                        getSupportFragmentManager().beginTransaction()
                                .remove(fragment)
                                .commit();
                        if (preferenceManager.getBoolean(Constants.KEY_HAS_GROUP)) {
                            startActivity(new Intent(this, MapsActivity.class));
                        }
                    }
                }
            }
            return true; // Return true to consume the event
        });
    }

    private final ActivityResultLauncher<Intent> pickImage = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if(result.getResultCode() == RESULT_OK) {
                    if(result.getData() != null) {
                        imageUri = result.getData().getData();
                        try {
                            InputStream inputStream = getContentResolver().openInputStream(imageUri);
                            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                            encodedImage = encodedImage(bitmap);
                            encodedImageBig = encodedImageBig(bitmap);
                            image.setImageBitmap(getCircularBitmap(bitmap));
                            preferenceManager.putString(Constants.KEY_IMAGE, encodedImage);
                            preferenceManager.putString(Constants.KEY_IMAGE_BIG, encodedImageBig);
                            FirebaseFirestore dataBase = FirebaseFirestore.getInstance();
                            if (preferenceManager.getBoolean(Constants.KEY_HAS_GROUP)) {
                                docRef = dataBase.collection(Constants.KEY_COLLECTION_GROUPS).document(preferenceManager.getString(Constants.KEY_GROUP_NAME));
                                docRef.get().addOnSuccessListener(documentSnapshot -> {
                                    if (documentSnapshot.exists()) {
                                        // Get the current array (assuming it is under the field "locations")
                                        HashMap<String, Object> user = (HashMap<String, Object>) documentSnapshot.get(preferenceManager.getString(Constants.KEY_NAME));

                                        if (user != null) {
                                            // Iterate through the array to find and update the map in the array
                                            user.put(Constants.KEY_IMAGE, encodedImage);

                                            // Update the document with the modified array
                                            docRef.update(preferenceManager.getString(Constants.KEY_NAME), user);
                                        }
                                    }
                                });
                            }
                            DocumentReference document = dataBase.collection(Constants.KEY_COLLECTION_USERS).document(preferenceManager.getString(Constants.KEY_USER_ID));
                            HashMap<String, Object> user = new HashMap<>();
                            user.put(Constants.KEY_IMAGE, encodedImage);
                            user.put(Constants.KEY_IMAGE_BIG, encodedImageBig);
                            document.update(user).addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void unused) {
                                    Log.d("dicky5", "hi");
                                }
                            });
                        }catch (FileNotFoundException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
    );

    private boolean isPointInsideView(float x, float y, View view) {
        int[] location = new int[2];
        view.getLocationOnScreen(location);
        int left = location[0];
        int top = location[1];
        int right = left + view.getWidth();
        int bottom = top + view.getHeight();

        return x > left && x < right && y > top && y < bottom;
    }

    private void loadUserDetails() {
        byte[] bytes = Base64.decode(preferenceManager.getString(Constants.KEY_IMAGE_BIG), Base64.DEFAULT);
        Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        image.setImageBitmap(getCircularBitmap(bitmap));
        textView.setText(preferenceManager.getString(Constants.KEY_NAME));
        progressBar.setVisibility(View.INVISIBLE);
        constraintLayout.setVisibility(View.VISIBLE);
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

    private void checkIfGroup() {
        if (preferenceManager.getBoolean(Constants.KEY_HAS_GROUP)) {
            FirebaseFirestore dataBase = FirebaseFirestore.getInstance();
            docRef = dataBase.collection(Constants.KEY_COLLECTION_GROUPS).document(preferenceManager.getString(Constants.KEY_GROUP_NAME));
            docRef.get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            DocumentSnapshot document = task.getResult();
                            if (document.exists()) {
                                // Assuming the name is stored in a field called "name"
                                HashMap<String,Object> documentName = (HashMap<String,Object>) document.get(preferenceManager.getString(Constants.KEY_NAME));

                                if (documentName == null) {
                                    HashMap<String, Object> user = new HashMap<>();
                                    user.put(Constants.KEY_NAME_HOST, "false");
                                    user.put(Constants.KEY_IMAGE, preferenceManager.getString(Constants.KEY_IMAGE));
                                    user.put(Constants.KEY_LATITUDE, "0");
                                    user.put(Constants.KEY_LONGITUDE, "0");
                                    user.put(Constants.KEY_COLOR, String.valueOf(getRandomColor()));
                                    docRef.update(preferenceManager.getString(Constants.KEY_NAME), user).addOnSuccessListener(new OnSuccessListener<Void>() {
                                        @Override
                                        public void onSuccess(Void unused) {
                                            startActivity(new Intent(getApplicationContext(), MapsActivity.class));
                                        }
                                    });
                                } else {
                                    startActivity(new Intent(getApplicationContext(), MapsActivity.class));
                                }
                            }
                        }
                    });
        }
    }

    private int getRandomColor() {
        Random random = new Random();
        int red = random.nextInt(256);   // 0-255
        int green = random.nextInt(256); // 0-255
        int blue = random.nextInt(256);  // 0-255

        // Combine into a color (ARGB format)
        return 0xFF000000 | (red << 16) | (green << 8) | blue;
    }

    private String encodedImage(Bitmap bitmap) {
        int previewWidth = 150;
        int previewHeight = bitmap.getHeight() * previewWidth / bitmap.getWidth();
        Bitmap previewBitmap = Bitmap.createScaledBitmap(bitmap, previewWidth, previewHeight, false);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        previewBitmap.compress(Bitmap.CompressFormat.JPEG, 50, byteArrayOutputStream);
        byte[] bytes = byteArrayOutputStream.toByteArray();
        return Base64.encodeToString(bytes, Base64.DEFAULT);
    }

    private String encodedImageBig(Bitmap bitmap) {
        int previewWidth = 500;
        int previewHeight = 800;
        Bitmap previewBitmap = Bitmap.createScaledBitmap(bitmap, previewWidth, previewHeight, false);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        previewBitmap.compress(Bitmap.CompressFormat.JPEG, 50, byteArrayOutputStream);
        byte[] bytes = byteArrayOutputStream.toByteArray();
        return Base64.encodeToString(bytes, Base64.DEFAULT);
    }
    public void onFragmentEvent(String data) {
        // Handle the event from the fragment
        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.fragmentView);
        findViewById(R.id.fragmentView).setVisibility(View.GONE);
        getSupportFragmentManager().beginTransaction()
                .remove(fragment)
                .commit();
        if (preferenceManager.getBoolean(Constants.KEY_HAS_GROUP)) {
            startActivity(new Intent(this, MapsActivity.class));
        }
    }

    public void onBackPressed() {
        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.fragmentView);
        if (fragment != null) {
            findViewById(R.id.fragmentView).setVisibility(View.GONE);
            getSupportFragmentManager().beginTransaction()
                    .remove(fragment)
                    .commit();
        } else {
            super.onBackPressed();
        }
    }
}