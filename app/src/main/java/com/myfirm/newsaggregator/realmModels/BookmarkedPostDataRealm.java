package com.myfirm.newsaggregator.realmModels;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

/**
 * Created by John on 11/24/2017.
 */

public class BookmarkedPostDataRealm extends RealmObject {
    @PrimaryKey
    private String id;
    private String name;
    private String link;
    private String createdTime;
    private String fullPicture;
    private String pageId;

    public BookmarkedPostDataRealm() {
    }

    public BookmarkedPostDataRealm(String id, String name, String link, String createdTime,
                         String fullPicture, String pageId) {
        this.id = id;
        this.name = name;
        this.link = link;
        this.createdTime = createdTime;
        this.fullPicture = fullPicture;
        this.pageId = pageId;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getLink() {
        return link;
    }

    public String getCreatedTime() {
        return createdTime;
    }

    public String getFullPicture() {
        return fullPicture;
    }

    public String getPageId() {
        return pageId;
    }
}
