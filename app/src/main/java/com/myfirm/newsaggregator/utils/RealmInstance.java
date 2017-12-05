package com.myfirm.newsaggregator.utils;

import android.content.Context;

import io.realm.Realm;

/**
 * Created by John on 11/23/2017.
 */

public class RealmInstance {
    public static Realm sRealm = null;

    private RealmInstance() {}

    public static Realm getRealm(Context context) {
        if (sRealm == null) {
            Realm.init(context);
            sRealm = Realm.getDefaultInstance();
        }

        return sRealm;
    }
}
