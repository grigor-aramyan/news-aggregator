package com.myfirm.newsaggregator.adapters;

import android.content.Context;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.GlideBuilder;
import com.bumptech.glide.request.RequestOptions;
import com.facebook.share.Share;
import com.myfirm.newsaggregator.R;
import com.myfirm.newsaggregator.animationInterpolators.MyBounceInterpolator;
import com.myfirm.newsaggregator.realmModels.BookmarkedPostDataRealm;
import com.myfirm.newsaggregator.utils.RealmInstance;
import com.robertsimoes.shareable.Shareable;

import java.util.ArrayList;
import java.util.Map;

import io.realm.Realm;
import io.realm.RealmResults;

/**
 * Created by John on 11/23/2017.
 */

public class PostsListAdapter extends RecyclerView.Adapter<PostsListAdapter.MyViewHolder> {
    public interface OnPostSelectedListener {
        void onPostSelected(String link);
    }
    private OnPostSelectedListener mPostSelectedListener;
    private ArrayList<Map<String, String>> mDataset;
    private Context mContext;

    public class MyViewHolder extends RecyclerView.ViewHolder {
        public ImageView mPostImage;
        public TextView mPostName, mPostDate;
        public ImageView mPostBookmark, mPostShare;

        public MyViewHolder(View itemView) {
            super(itemView);

            this.mPostImage = (ImageView) itemView.findViewById(R.id.post_image_id);
            this.mPostName = (TextView) itemView.findViewById(R.id.post_name_txt);
            this.mPostDate = (TextView) itemView.findViewById(R.id.post_date_txt);
            this.mPostBookmark = (ImageView) itemView.findViewById(R.id.bookmark_icon);
            this.mPostShare = (ImageView) itemView.findViewById(R.id.share_icon);
        }
    }

    public PostsListAdapter(ArrayList<Map<String, String>> dataset,
                            OnPostSelectedListener onPostSelectedListener) {
        this.mDataset = dataset;
        this.mPostSelectedListener = onPostSelectedListener;
    }

    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_posts_list,
                parent, false);

        mContext = parent.getContext();

        return new MyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final MyViewHolder holder, int position) {
        Map<String, String> postInfo = mDataset.get(position);

        final boolean[] postBookmarked = new boolean[] { false };
        final String name, date, link, fullPicture, id, pageId;
        name = postInfo.get("name");
        date = postInfo.get("createdDate").replace("T", " ")
                                            .split("\\+")[0];
        link = postInfo.get("link");
        fullPicture = postInfo.get("fullPicture");
        id = postInfo.get("id");
        pageId = postInfo.get("pageId");

        RequestOptions options = new RequestOptions();
        options.centerCrop();
        Glide.with(mContext).load(fullPicture).apply(options).into(holder.mPostImage);

        holder.mPostName.setText(name);
        holder.mPostDate.setText(date);

        final PopupMenu menu = new PopupMenu(mContext, holder.mPostShare);
        menu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                switch (menuItem.getItemId()) {
                    case R.id.facebook_share_id:
                        sharePost(0, name, link);
                        return true;
                    case R.id.google_plus_share_id:
                        sharePost(1, name, link);
                        return true;
                    case R.id.twitter_share_id:
                        sharePost(2, name, link);
                        return true;
                    case R.id.linked_in_share_id:
                        sharePost(3, name, link);
                        return true;
                }

                return false;
            }
        });
        menu.getMenuInflater().inflate(R.menu.share_menu, menu.getMenu());
        holder.mPostShare.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                menu.show();
            }
        });

        final RealmResults<BookmarkedPostDataRealm> results = RealmInstance.getRealm(mContext)
                .where(BookmarkedPostDataRealm.class).equalTo("id", id).findAll();
        if (results.size() > 0) {
            postBookmarked[0] = true;
            holder.mPostBookmark.setImageDrawable(ContextCompat.getDrawable(mContext, R.drawable.ic_bookmark_red_24dp));
        }
        holder.mPostBookmark.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (postBookmarked[0]) {
                    holder.mPostBookmark.setImageDrawable(ContextCompat.getDrawable(mContext, R.drawable.ic_bookmark_white_24dp));
                    animateButton(holder.mPostBookmark);

                    final RealmResults<BookmarkedPostDataRealm> results1 = RealmInstance.getRealm(mContext)
                            .where(BookmarkedPostDataRealm.class).equalTo("id", id).findAll();

                    RealmInstance.getRealm(mContext).executeTransaction(new Realm.Transaction() {
                        @Override
                        public void execute(Realm realm) {
                            results1.deleteAllFromRealm();

                            postBookmarked[0] = false;
                        }
                    });
                } else {
                    postBookmarked[0] = true;
                    holder.mPostBookmark.setImageDrawable(ContextCompat.getDrawable(mContext, R.drawable.ic_bookmark_red_24dp));
                    animateButton(holder.mPostBookmark);

                    RealmInstance.getRealm(mContext).beginTransaction();
                    RealmInstance.getRealm(mContext).copyToRealmOrUpdate(
                            new BookmarkedPostDataRealm(id, name, link, date, fullPicture, pageId)
                    );
                    RealmInstance.getRealm(mContext).commitTransaction();

                }
            }
        });

        holder.mPostImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mPostSelectedListener.onPostSelected(link);
            }
        });
    }

    private void animateButton(ImageView bookmarkIcon) {
        final Animation animation = AnimationUtils.loadAnimation(mContext, R.anim.bounce);

        MyBounceInterpolator interpolator = new MyBounceInterpolator(0.2, 20);
        animation.setInterpolator(interpolator);

        bookmarkIcon.startAnimation(animation);
    }

    private void sharePost(int index, String name, String link) {
        int socialChannel = 0;
        switch (index) {
            case 0:
                socialChannel = Shareable.Builder.FACEBOOK;
                break;
            case 1:
                socialChannel = Shareable.Builder.GOOGLE_PLUS;
                break;
            case 2:
                socialChannel = Shareable.Builder.TWITTER;
                break;
            case 3:
                socialChannel = Shareable.Builder.LINKED_IN;
                break;
            default:
                break;

        }
        Shareable shareable = new Shareable.Builder(mContext)
                .message(name)
                .url(link)
                .socialChannel(socialChannel)
                .build();
        shareable.share();
    }

    @Override
    public int getItemCount() {
        return mDataset.size();
    }
}
