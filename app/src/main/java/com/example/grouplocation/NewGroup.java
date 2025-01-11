package com.example.grouplocation;

import static androidx.core.content.ContextCompat.getSystemService;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import android.text.Editable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.grouplocation.utilities.Constants;
import com.example.grouplocation.utilities.PreferenceManager;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Random;

public class NewGroup extends Fragment {

    private Button button;

    private Button continueOn;

    private TextView textView;

    private ImageView imageView;

    private PreferenceManager preferenceManager;
    private EditText editText;

    private OnFragmentInteractionListener mListener;

    // Define the interface
    public interface OnFragmentInteractionListener {
        void onFragmentEvent(String data); // Example event method
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_new_group, container, false);

        button = rootView.findViewById(R.id.button3);
        preferenceManager = new PreferenceManager(getActivity());
        editText = rootView.findViewById(R.id.nameOfGroup);
        imageView = rootView.findViewById(R.id.copy);
        continueOn = rootView.findViewById(R.id.continueOn);
        textView = rootView.findViewById(R.id.link);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!editText.getText().toString().trim().isEmpty()) {
                    createGroup(editText.getText().toString());
                } else {
                    editText.setError("must input the name");
                }

            }
        });
        continueOn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                notifyActivity("hello");
            }
        });
        imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ClipboardManager clipboard = ContextCompat.getSystemService(getActivity(), ClipboardManager.class);

                // Create a ClipData object with the text you want to copy
                ClipData clip = ClipData.newPlainText("link", textView.getText());

                // Set the clip to the clipboard
                clipboard.setPrimaryClip(clip);

                // Optionally, show a confirmation message
                Toast.makeText(getActivity(), "Text copied to clipboard", Toast.LENGTH_SHORT).show();
            }
        });
        // Inflate the layout for this fragment
        return rootView;
    }

    public void createGroup(String name) {
        button.setVisibility(View.GONE);
        editText.setVisibility(View.GONE);
        FirebaseFirestore dataBase = FirebaseFirestore.getInstance();
        HashMap<String, Object> user = new HashMap<>();
        HashMap<String, String> details = new HashMap<>();
        details.put(Constants.KEY_NAME_HOST, "true");
        details.put(Constants.KEY_LATITUDE, "0");
        details.put(Constants.KEY_LONGITUDE, "0");
        details.put(Constants.KEY_COLOR, String.valueOf(getRandomColor()));
        details.put(Constants.KEY_IMAGE, preferenceManager.getString(Constants.KEY_IMAGE));
        user.put(preferenceManager.getString(Constants.KEY_NAME), details);
        user.put(Constants.KEY_GROUP_NAME, editText.getText().toString());
        dataBase.collection(Constants.KEY_COLLECTION_GROUPS)
                .add(user)
                .addOnSuccessListener(documentReference -> {
                    preferenceManager.putBoolean(Constants.KEY_HAS_GROUP, true);
                    preferenceManager.putString(Constants.KEY_GROUP_NAME, documentReference.getId());
                    textView.setText("https://www.grouplocation.com/" + documentReference.getId());
                    textView.setVisibility(View.VISIBLE);
                    imageView.setVisibility(View.VISIBLE);
                    continueOn.setVisibility(View.VISIBLE);
                });
    }
    private int getRandomColor() {
        Random random = new Random();
        int red = random.nextInt(256);   // 0-255
        int green = random.nextInt(256); // 0-255
        int blue = random.nextInt(256);  // 0-255

        // Combine into a color (ARGB format)
        return 0xFF000000 | (red << 16) | (green << 8) | blue;
    }
    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);

        // Ensure the containing activity implements the interface
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }
    public void notifyActivity(String data) {
        if (mListener != null) {
            mListener.onFragmentEvent(data);
        }
    }
}