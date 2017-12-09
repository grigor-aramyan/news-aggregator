package com.myfirm.newsaggregator.activities;

import android.app.SearchManager;
import android.content.Intent;
import android.support.animation.DynamicAnimation;
import android.support.animation.SpringAnimation;
import android.support.animation.SpringForce;
import android.support.constraint.ConstraintLayout;
import android.support.constraint.ConstraintSet;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.transition.TransitionManager;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebView;
import android.widget.ImageView;
import android.widget.Toast;

import com.myfirm.newsaggregator.R;
import com.myfirm.newsaggregator.adapters.PostsListAdapter;
import com.myfirm.newsaggregator.realmModels.PostDataRealm;
import com.myfirm.newsaggregator.utils.RealmInstance;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import io.realm.Case;
import io.realm.RealmResults;
import io.realm.Sort;

public class SearchedActivity extends AppCompatActivity {
    private View.OnClickListener mClickListener = new View.OnClickListener() {
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
    private ConstraintLayout mGlobalLayout;
    private RecyclerView mList;
    private ImageView mCollapseWebViewIc;
    private WebView mWebView;

    // animation
    private SpringAnimation mSpringAnimation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_searched);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setBackgroundColor(ResourcesCompat.getColor(getResources(), R.color.colorBoldGreen, null));
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        initViews();

        handleIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        setIntent(intent);
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        if (intent.getAction().equals(Intent.ACTION_SEARCH)) {
            searchThePosts(intent.getStringExtra(SearchManager.QUERY));
        }
    }

    private void searchThePosts(String searchQuery) {
        Log.e("mmm", "query: " + searchQuery);
        RealmResults<PostDataRealm> results = RealmInstance.getRealm(getApplicationContext())
                .where(PostDataRealm.class).like("name", "*" + searchQuery + "*",
                        Case.INSENSITIVE).findAll();
        if (results.size() == 0) {
            Toast.makeText(getApplicationContext(), "No results found!",
                    Toast.LENGTH_LONG).show();
            return;
        }

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

        mList.setAdapter(new PostsListAdapter(dataset, new PostsListAdapter.OnPostSelectedListener() {
            @Override
            public void onPostSelected(String link) {
                mWebView.loadUrl(link);

                turnWebview(1);

            }
        }));
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
            set2.constrainHeight(R.id.web_view_id, (int) mGlobalLayout.getHeight() / 8 * 6);
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

            mSpringAnimation = new SpringAnimation(mCollapseWebViewIc, DynamicAnimation.ALPHA);
            mSpringAnimation.setSpring(force);
            mSpringAnimation.animateToFinalPosition(1.0f);
        } else if (flag == 0) {
            mCollapseWebViewIc.setVisibility(View.GONE);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            default:
                return false;
        }
    }

    private void initViews() {
        mList = (RecyclerView) findViewById(R.id.posts_list_id);
        mList.setLayoutManager(new LinearLayoutManager(getApplicationContext()));

        mCollapseWebViewIc = (ImageView) findViewById(R.id.collapse_webview_ic_id);
        mCollapseWebViewIc.setOnClickListener(mClickListener);

        mWebView = (WebView) findViewById(R.id.web_view_id);
        mWebView.setInitialScale(1);
        mWebView.getSettings().setLoadWithOverviewMode(true);
        mWebView.getSettings().setUseWideViewPort(true);
        mWebView.getSettings().setBuiltInZoomControls(true);

        mGlobalLayout = (ConstraintLayout) findViewById(R.id.global_layout_id);
    }
}
