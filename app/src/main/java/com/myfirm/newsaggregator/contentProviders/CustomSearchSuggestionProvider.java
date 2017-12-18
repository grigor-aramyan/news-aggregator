package com.myfirm.newsaggregator.contentProviders;

import android.app.SearchManager;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.provider.BaseColumns;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.facebook.share.Share;
import com.myfirm.newsaggregator.realmModels.PostDataRealm;
import com.myfirm.newsaggregator.utils.RealmInstance;
import com.myfirm.newsaggregator.utils.Statics;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import io.realm.Case;
import io.realm.RealmResults;

/**
 * Created by John on 12/19/2017.
 */

public class CustomSearchSuggestionProvider extends ContentProvider {
    private List<String> mPostTitles = null;
    public static boolean sDataUpdated = true;

    @Override
    public boolean onCreate() {
        return true;
    }

    @Nullable
    @Override
    public Cursor query(@NonNull Uri uri, @Nullable String[] strings, @Nullable String s, @Nullable String[] strings1, @Nullable String s1) {
        String query = uri.getLastPathSegment();
        MatrixCursor cursor = new MatrixCursor(new String[] {BaseColumns._ID, SearchManager.SUGGEST_COLUMN_TEXT_1,
            SearchManager.SUGGEST_COLUMN_INTENT_DATA, SearchManager.SUGGEST_COLUMN_TEXT_2});
        if (query.equals("search_suggest_query")) {

        } else {

            if (sDataUpdated) {
                SharedPreferences sharedPreferences = getContext().getSharedPreferences(Statics.sSharedPrefForApp, Context.MODE_PRIVATE);
                String suggestionsEncoded = sharedPreferences.getString(Statics.sSuggestions, "");
                if (!suggestionsEncoded.isEmpty()) {
                    // encode: ##
                    String[] titles = suggestionsEncoded.split("##");
                    mPostTitles = Arrays.asList(titles);

                    int i = 0;
                    for (String title: mPostTitles) {
                        if (title.toLowerCase().contains(query.toLowerCase())) {
                            int beginningIndex = title.toLowerCase().indexOf(query.toLowerCase());
                            String word = title.substring(beginningIndex).split("\\W")[0];
                            cursor.addRow(new Object[] {i++, word, title, title});
                        }
                    }
                    sDataUpdated = false;
                }
            } else {
                int i = 0;
                for (String title: mPostTitles) {
                    if (title.toLowerCase().contains(query.toLowerCase())) {
                        int beginningIndex = title.toLowerCase().indexOf(query.toLowerCase());
                        String word = title.substring(beginningIndex).split("\\W")[0];
                        cursor.addRow(new Object[] {i++, word, title, title});
                    }
                }
            }
        }
        return cursor;
    }

    @Nullable
    @Override
    public String getType(@NonNull Uri uri) {
        return null;
    }

    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, @Nullable ContentValues contentValues) {
        return null;
    }

    @Override
    public int delete(@NonNull Uri uri, @Nullable String s, @Nullable String[] strings) {
        return 0;
    }

    @Override
    public int update(@NonNull Uri uri, @Nullable ContentValues contentValues, @Nullable String s, @Nullable String[] strings) {
        return 0;
    }
}
