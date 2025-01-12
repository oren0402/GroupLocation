package com.example.grouplocation;

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;

public class FriendsModel {
    // variables for our course
    // name and description.
    private String personName;
    private Bitmap personImage;
    private Drawable addOrRemove;
    private Boolean addOrRemoveBool;

    // creating constructor for our variables.
    public FriendsModel(String personName, Bitmap personImage, Drawable addOrRemove, Boolean addOrRemoveBool) {
        this.personName = personName;
        this.personImage = personImage;
        this.addOrRemove = addOrRemove;
        this.addOrRemoveBool = addOrRemoveBool;
    }

    // creating getter and setter methods.
    public String getPersonName() {
        return personName;
    }

    public void setPersonName(String personName) {
        this.personName = personName;
    }

    public Drawable getAddOrRemove() {
        return addOrRemove;
    }

    public Boolean getAddOrRemoveBool() {
        return addOrRemoveBool;
    }

    public Bitmap getPersonImage() {
        return personImage;
    }

    public void setPersonImage(Bitmap personImage) {
        this.personImage = personImage;
    }
}
