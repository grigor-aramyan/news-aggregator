package com.myfirm.newsaggregator.activities;

import android.animation.ObjectAnimator;
import android.graphics.Color;
import android.media.Image;
import android.os.Bundle;
import android.support.animation.DynamicAnimation;
import android.support.animation.SpringAnimation;
import android.support.animation.SpringForce;
import android.support.constraint.ConstraintLayout;
import android.support.constraint.ConstraintSet;
import android.support.design.widget.TabLayout;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.transition.TransitionManager;
import android.util.Log;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.WebView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.facebook.AccessToken;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.HttpMethod;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.myfirm.newsaggregator.R;
import com.myfirm.newsaggregator.adapters.PostsListAdapter;
import com.myfirm.newsaggregator.realmModels.BookmarkedPostDataRealm;
import com.myfirm.newsaggregator.realmModels.PostDataRealm;
import com.myfirm.newsaggregator.utils.FirebaseRDInstance;
import com.myfirm.newsaggregator.utils.RealmInstance;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import devlight.io.library.ntb.NavigationTabBar;
import io.realm.Realm;
import io.realm.RealmResults;
import io.realm.Sort;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    // listeners
    private View.OnClickListener mOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            switch (view.getId()) {
                case R.id.collapse_webview_ic_id:
                    mCollapseWebViewIc.setVisibility(View.GONE);

                    turnWebview(0);

                    break;
                default:
                    break;
            }
        }
    };

    // UI elements
    private RecyclerView mPostsList = null;
    private ConstraintLayout mGlobalLayout = null;
    private LinearLayout mMainContentLayout = null;
    private RelativeLayout mProgressBarLayout = null;
    private WebView mWebView = null;
    private ImageView mCollapseWebViewIc = null;
    private NavigationTabBar mNtb = null;

    // animation
    private SpringAnimation mSpringAnimation;

    // data
    private List<String> mPageIds = Arrays.asList("ankakh.am", "Tertam.arm");
    private AccessToken mFBAccessToken = null;
    private boolean mWithinBookmarks = false;
    //for persisting selected tab
    private int mTabNavSelectedIndex = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        initViews();

        FirebaseRDInstance.getRDInstance().addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                String token, appId, userId;
                token = dataSnapshot.child("accessToken").getValue().toString();
                appId = dataSnapshot.child("applicationId").getValue().toString();
                userId = dataSnapshot.child("userId").getValue().toString();

                mFBAccessToken = new AccessToken(token, appId, userId, null, null,
                        null, null, null);

                clearDBAndFetchFeeds(0);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        /*FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });*/

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        // **************************************
        // tab navigation init and configuration
        ArrayList<NavigationTabBar.Model> models = new ArrayList<>();
        models.add(new NavigationTabBar.Model.Builder(
                ContextCompat.getDrawable(getApplicationContext(),R.drawable.ic_all_out_white_24dp),
                        ResourcesCompat.getColor(getResources(), R.color.colorAccent, null)
        ).title("All news").build());
        models.add(new NavigationTabBar.Model.Builder(
                        ContextCompat.getDrawable(getApplicationContext(),R.drawable.ic_bookmark_white_24dp),
                        ResourcesCompat.getColor(getResources(), R.color.colorPurpleBold, null)
                ).title("Bookmarked").build());
        mNtb.setModels(models);
        mNtb.setModelIndex(0);
        mNtb.setOnTabBarSelectedIndexListener(new NavigationTabBar.OnTabBarSelectedIndexListener() {
            @Override
            public void onStartTabSelected(NavigationTabBar.Model model, int index) {

            }

            @Override
            public void onEndTabSelected(NavigationTabBar.Model model, int index) {
                switch (index) {
                    case 0:
                        Toast.makeText(getApplicationContext(), "All news", Toast.LENGTH_LONG).show();
                        mWithinBookmarks = false;
                        resetListAdapter(mTabNavSelectedIndex);

                        break;
                    case 1:
                        Toast.makeText(getApplicationContext(), "Bookmarked", Toast.LENGTH_LONG).show();
                        mWithinBookmarks = true;
                        resetListAdapter(mTabNavSelectedIndex);

                        break;
                    default:
                        break;
                }
            }
        });
        // **********************************************
    }

    private void clearDBAndFetchFeeds(int index) {
        final RealmResults<PostDataRealm> results = RealmInstance.getRealm(getApplicationContext())
                .where(PostDataRealm.class).findAll();
        RealmInstance.getRealm(getApplicationContext()).executeTransaction(new Realm.Transaction() {
            @Override
            public void execute(Realm realm) {
                results.deleteAllFromRealm();
            }
        });

        fetchPageFeeds(index);
    }

    private void resetListAdapter(int index) {
        turnWebview(0);
        mTabNavSelectedIndex = index;

        String pageId = "all";
        if (index > 0)
            pageId = mPageIds.get(--index);

        ArrayList<Map<String, String>> dataset = new ArrayList<>();
        if (mWithinBookmarks) {
            RealmResults<BookmarkedPostDataRealm> results = null;
            if (pageId.equals("all")) {
                results = RealmInstance.getRealm(getApplicationContext())
                        .where(BookmarkedPostDataRealm.class).findAll();
            } else {
                results = RealmInstance.getRealm(getApplicationContext())
                        .where(BookmarkedPostDataRealm.class).equalTo("pageId", pageId).findAll();
            }
            results = results.sort("createdTime", Sort.DESCENDING);

            Map<String, String> postInfo = null;
            for (BookmarkedPostDataRealm postDataRealm: results) {
                postInfo = new HashMap<>();

                postInfo.put("name", postDataRealm.getName());
                postInfo.put("createdDate", postDataRealm.getCreatedTime());
                postInfo.put("link", postDataRealm.getLink());
                postInfo.put("fullPicture", postDataRealm.getFullPicture());
                postInfo.put("id", postDataRealm.getId());
                postInfo.put("pageId", postDataRealm.getPageId());

                dataset.add(postInfo);
            }

        } else {
            RealmResults<PostDataRealm> results = null;
            if (pageId.equals("all")) {
                results = RealmInstance.getRealm(getApplicationContext())
                        .where(PostDataRealm.class).findAll();
            } else {
                results = RealmInstance.getRealm(getApplicationContext())
                        .where(PostDataRealm.class).equalTo("pageId", pageId).findAll();
            }
            results = results.sort("createdTime", Sort.DESCENDING);

            Map<String, String> postInfo = null;
            for (PostDataRealm postDataRealm: results) {
                postInfo = new HashMap<>();

                postInfo.put("name", postDataRealm.getName());
                postInfo.put("createdDate", postDataRealm.getCreatedTime());
                postInfo.put("link", postDataRealm.getLink());
                postInfo.put("fullPicture", postDataRealm.getFullPicture());
                postInfo.put("id", postDataRealm.getId());
                postInfo.put("pageId", postDataRealm.getPageId());

                dataset.add(postInfo);
            }
        }

        mPostsList.setAdapter(new PostsListAdapter(dataset, new PostsListAdapter.OnPostSelectedListener() {
            @Override
            public void onPostSelected(String link) {
                mWebView.loadUrl(link);

                turnWebview(1);

            }
        }));
    }

    private void fetchPageFeeds(int index) {
        mNtb.setVisibility(View.GONE);
        mMainContentLayout.setVisibility(View.GONE);
        mProgressBarLayout.setVisibility(View.VISIBLE);

        final String pageId = mPageIds.get(index);
        final int nextIndex = ++index;

        new GraphRequest(mFBAccessToken, "/" + pageId + "/feed?fields=link,name,created_time,full_picture&limit=50",
                null, HttpMethod.GET,
                new GraphRequest.Callback() {
                    @Override
                    public void onCompleted(GraphResponse response) {
                        try {
                            //Log.e("mmm", "res: " + response.getRawResponse());

                            JSONObject initialData = new JSONObject(response.getRawResponse());
                            JSONArray postsArray = initialData.getJSONArray("data");

                            JSONObject postData = null;
                            String link = null, name = null, createdTime = null,
                                    fullPicture = null, id = null;
                            for (int i = 0; i < postsArray.length(); i++) {
                                postData = postsArray.getJSONObject(i);

                                try {
                                    link = postData.getString("link").replace("\\", "");
                                    name = postData.getString("name");
                                    createdTime = postData.getString("created_time");
                                    fullPicture = postData.getString("full_picture").replace("\\", "");
                                    id = postData.getString("id");
                                } catch (JSONException jException) {
                                    continue;
                                }

                                RealmInstance.getRealm(getApplicationContext()).beginTransaction();
                                RealmInstance.getRealm(getApplicationContext()).copyToRealmOrUpdate(
                                        new PostDataRealm(id, name, link, createdTime, fullPicture, pageId)
                                );
                                RealmInstance.getRealm(getApplicationContext()).commitTransaction();
                            }
                        } catch (JSONException jExp) {
                            Toast.makeText(getApplicationContext(), "Unable to parse api response",
                                    Toast.LENGTH_LONG).show();
                        }

                        if (nextIndex < mPageIds.size()) {
                            fetchPageFeeds(nextIndex);
                        } else {
                            // all necessary posts are fetched, prepare list
                            preparePostsList();
                        }
                    }
                }).executeAsync();
    }

    private void preparePostsList() {
        RealmResults<PostDataRealm> results = RealmInstance.getRealm(getApplicationContext())
                .where(PostDataRealm.class).findAll();
        results = results.sort("createdTime", Sort.DESCENDING);

        ArrayList<Map<String, String>> dataset = new ArrayList<>();
        Map<String, String> postInfo = null;
        for (PostDataRealm postDataRealm: results) {
            postInfo = new HashMap<>();

            postInfo.put("name", postDataRealm.getName());
            postInfo.put("createdDate", postDataRealm.getCreatedTime());
            postInfo.put("link", postDataRealm.getLink());
            postInfo.put("fullPicture", postDataRealm.getFullPicture());
            postInfo.put("id", postDataRealm.getId());
            postInfo.put("pageId", postDataRealm.getPageId());

            dataset.add(postInfo);
        }

        mPostsList.setAdapter(new PostsListAdapter(dataset, new PostsListAdapter.OnPostSelectedListener() {
            @Override
            public void onPostSelected(String link) {
                mWebView.loadUrl(link);

                turnWebview(1);

            }
        }));
        mMainContentLayout.setVisibility(View.VISIBLE);
        mNtb.setVisibility(View.VISIBLE);
        mProgressBarLayout.setVisibility(View.GONE);
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_refresh) {

            turnWebview(0);
            clearDBAndFetchFeeds(0);

            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void turnWebview(int flag) {
        // flag: 0 - collapse, 1 - expand

        ConstraintSet set1, set2;
        set1 = new ConstraintSet();
        set1.clone(mGlobalLayout);
        set2 = new ConstraintSet();
        set2.clone(mGlobalLayout);
        if (flag == 0) {
            set2.constrainHeight(R.id.web_view_id, 0);
        } else if (flag == 1) {
            set2.constrainHeight(R.id.web_view_id, (int) mGlobalLayout.getHeight() / 8 * 7);
        }
        TransitionManager.beginDelayedTransition(mGlobalLayout);
        set2.applyTo(mGlobalLayout);
        
        // animation for collapse webview icon
        SpringForce force = new SpringForce();
        force.setStiffness(SpringForce.STIFFNESS_VERY_LOW)
                .setDampingRatio(SpringForce.DAMPING_RATIO_HIGH_BOUNCY);
        if (flag == 1) {
            mCollapseWebViewIc.setAlpha(0.0f);
            mCollapseWebViewIc.setVisibility(View.VISIBLE);

            mNtb.setVisibility(View.GONE);

            mSpringAnimation = new SpringAnimation(mCollapseWebViewIc, DynamicAnimation.ALPHA);
            mSpringAnimation.setSpring(force);
            mSpringAnimation.animateToFinalPosition(1.0f);
        } else if (flag == 0) {
            mNtb.setVisibility(View.VISIBLE);
            mCollapseWebViewIc.setVisibility(View.GONE);
        }
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.all_news) {
            resetListAdapter(0);
        } else if (id == R.id.ankakh) {
            resetListAdapter(1);
        } else if (id == R.id.tertam) {
            resetListAdapter(2);
        }
        /*else if (id == R.id.nav_camera) {
            // Handle the camera action
        } else if (id == R.id.nav_gallery) {

        } else if (id == R.id.nav_slideshow) {

        } else if (id == R.id.nav_manage) {

        } else if (id == R.id.nav_share) {

        } else if (id == R.id.nav_send) {

        }*/

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    private void initViews() {
        mPostsList = (RecyclerView) findViewById(R.id.posts_list_id);
        mPostsList.setLayoutManager(new LinearLayoutManager(getApplicationContext()));

        mGlobalLayout = (ConstraintLayout) findViewById(R.id.main_layout_id);

        mMainContentLayout = (LinearLayout) findViewById(R.id.main_content_layout);

        mProgressBarLayout = (RelativeLayout) findViewById(R.id.progress_bar_layout_id);

        mWebView = (WebView) findViewById(R.id.web_view_id);
        mWebView.setInitialScale(1);
        mWebView.getSettings().setLoadWithOverviewMode(true);
        mWebView.getSettings().setUseWideViewPort(true);
        mWebView.getSettings().setBuiltInZoomControls(true);

        mCollapseWebViewIc = (ImageView) findViewById(R.id.collapse_webview_ic_id);
        mCollapseWebViewIc.setOnClickListener(mOnClickListener);

        mNtb = (NavigationTabBar) findViewById(R.id.ntb_id);
    }
}
