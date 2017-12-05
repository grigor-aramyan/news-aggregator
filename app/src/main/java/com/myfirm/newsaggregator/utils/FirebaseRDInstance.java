package com.myfirm.newsaggregator.utils;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

/**
 * Created by John on 11/23/2017.
 */

public class FirebaseRDInstance {
    public static DatabaseReference mDatabaseReference = null;

    public FirebaseRDInstance() {
    }

    public static DatabaseReference getRDInstance() {
        if (mDatabaseReference == null) {
            mDatabaseReference = FirebaseDatabase.getInstance().getReference();
        }

        return mDatabaseReference;
    }
}
