package info.justaway.adapter;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import info.justaway.JustawayApplication;
import info.justaway.MainActivity;
import info.justaway.PostActivity;
import info.justaway.ProfileActivity;
import info.justaway.R;
import info.justaway.ScaleImageActivity;
import info.justaway.model.Row;
import twitter4j.DirectMessage;
import twitter4j.MediaEntity;
import twitter4j.Status;
import twitter4j.URLEntity;
import twitter4j.User;

public class TwitterAdapter extends ArrayAdapter<Row> {
    private JustawayApplication mApplication;
    private Context mContext;
    private ArrayList<Row> mStatuses = new ArrayList<Row>();
    private LayoutInflater mInflater;
    private int mLayout;
    private Boolean isMain;
    private static final int LIMIT = 100;
    private int mLimit = LIMIT;
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy/MM'/'dd' 'HH':'mm':'ss",
            Locale.ENGLISH);

    public TwitterAdapter(Context context, int textViewResourceId) {
        super(context, textViewResourceId);
        this.mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        this.mContext = context;
        this.mLayout = textViewResourceId;
        this.mApplication = (JustawayApplication) context.getApplicationContext();
        this.isMain = mContext.getClass().getName().equals("info.justaway.MainActivity");
    }

    public void extensionAdd(Row row) {
        super.add(row);
        this.filter(row);
        this.mStatuses.add(row);
        mLimit++;
    }

    @Override
    public void add(Row row) {
        super.add(row);
        this.filter(row);
        this.mStatuses.add(row);
        this.limitation();
    }

    @Override
    public void insert(Row row, int index) {
        super.insert(row, index);
        this.filter(row);
        this.mStatuses.add(index, row);
        this.limitation();
    }

    @Override
    public void remove(Row row) {
        super.remove(row);
        this.mStatuses.remove(row);
    }

    private void filter(Row row) {
        Status status = row.getStatus();
        if (status != null && status.isRetweeted()) {
            Status retweet = status.getRetweetedStatus();
            long userId = mApplication.getUserId();
            if (retweet != null && status.getUser().getId() == userId) {
                mApplication.setRtId(retweet.getId(), status.getId());
            }
        }
    }

    @SuppressWarnings("unused")
    public void replaceStatus(Status status) {
        for (Row row : mStatuses) {
            if (!row.isDirectMessage() && row.getStatus().getId() == status.getId()) {
                row.setStatus(status);
                notifyDataSetChanged();
                break;
            }
        }
    }

    public void removeStatus(long statusId) {
        for (Row row : mStatuses) {
            if (!row.isDirectMessage() && row.getStatus().getId() == statusId) {
                remove(row);
                break;
            }
        }
    }

    public void removeDirectMessage(long directMessageId) {
        for (Row row : mStatuses) {
            if (row.isDirectMessage() && row.getMessage().getId() == directMessageId) {
                remove(row);
                break;
            }
        }
    }

    public void limitation() {
        int size = this.mStatuses.size();
        if (size > mLimit) {
            int count = size - mLimit;
            for (int i = 0; i < count; i++) {
                super.remove(this.mStatuses.remove(size - i - 1));
            }
        }
    }

    @Override
    public void clear() {
        super.clear();
        this.mStatuses.clear();
        mLimit = LIMIT;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // ビューを受け取る
        View view = convertView;
        if (view == null) {
            // 受け取ったビューがnullなら新しくビューを生成
            view = mInflater.inflate(this.mLayout, null);
        }

        // 表示すべきデータの取得
        Row row = mStatuses.get(position);

        if (row.isDirectMessage()) {
            DirectMessage message = row.getMessage();
            if (message == null) {
                return view;
            }
            renderMessage(view, message);
        } else {
            Status status = row.getStatus();
            if (status == null) {
                return view;
            }

            Status retweet = status.getRetweetedStatus();
            if (row.isFavorite()) {
                renderStatus(view, status, null, row.getSource());
            } else if (retweet == null) {
                renderStatus(view, status, null, null);
            } else {
                renderStatus(view, retweet, status, null);
            }
        }

        if (isMain && position == 0) {
            ((MainActivity) mContext).showTopView();
        }

        return view;
    }

    private void renderMessage(View view, final DirectMessage message) {

        Typeface fontello = Typeface.createFromAsset(mContext.getAssets(), "fontello.ttf");
        long userId = JustawayApplication.getApplication().getUserId();

        TextView doReply = (TextView) view.findViewById(R.id.do_reply);
        view.findViewById(R.id.do_retweet).setVisibility(View.GONE);
        view.findViewById(R.id.do_fav).setVisibility(View.GONE);
        view.findViewById(R.id.retweet_count).setVisibility(View.GONE);
        view.findViewById(R.id.fav_count).setVisibility(View.GONE);
        view.findViewById(R.id.menu_and_via).setVisibility(View.VISIBLE);

        if (message.getSender().getId() == userId) {
            doReply.setVisibility(View.GONE);
        } else {
            doReply.setVisibility(View.VISIBLE);
            doReply.setTypeface(fontello);
            doReply.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String text = "D " + message.getSender().getScreenName() + " ";
                    if (mContext.getClass().getName().equals("info.justaway.MainActivity")) {
                        MainActivity activity = (MainActivity) mContext;
                        View singleLineTweet = activity.findViewById(R.id.quick_tweet_layout);
                        if (singleLineTweet != null && singleLineTweet.getVisibility() == View.VISIBLE) {
                            EditText editStatus = (EditText) activity.findViewById(R.id.quick_tweet_edit);
                            editStatus.setText(text);
                            editStatus.setSelection(text.length());
                            editStatus.requestFocus();
                            mApplication.showKeyboard(editStatus);
                            activity.setInReplyToStatusId((long) 0);
                            return;
                        }
                    }
                    Intent intent = new Intent(mContext, PostActivity.class);
                    intent.putExtra("status", text);
                    intent.putExtra("selection", text.length());
                    mContext.startActivity(intent);
                }
            });
        }

        ((TextView) view.findViewById(R.id.display_name)).setText(message.getSender().getName());
        ((TextView) view.findViewById(R.id.screen_name)).setText("@"
                + message.getSender().getScreenName());
        ((TextView) view.findViewById(R.id.status)).setText("D " + message.getRecipientScreenName()
                + " " + message.getText());
        ((TextView) view.findViewById(R.id.datetime))
                .setText(getAbsoluteTime(message.getCreatedAt()));
        ((TextView) view.findViewById(R.id.datetime_relative))
                .setText(getRelativeTime(message.getCreatedAt()));
        view.findViewById(R.id.via).setVisibility(View.GONE);
        view.findViewById(R.id.retweet).setVisibility(View.GONE);
        view.findViewById(R.id.images).setVisibility(View.GONE);
        ImageView icon = (ImageView) view.findViewById(R.id.icon);
        mApplication.displayRoundedImage(message.getSender().getBiggerProfileImageURL(), icon);
        icon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(v.getContext(), ProfileActivity.class);
                intent.putExtra("screenName", message.getSender().getScreenName());
                mContext.startActivity(intent);
            }
        });
        view.findViewById(R.id.action).setVisibility(View.GONE);
        view.findViewById(R.id.fontello_lock).setVisibility(View.INVISIBLE);
    }

    private void renderStatus(View view, final Status status, Status retweet,
                              User favorite) {

        long userId = JustawayApplication.getApplication().getUserId();

        Typeface fontello = Typeface.createFromAsset(mContext.getAssets(), "fontello.ttf");

        final TextView doReply = (TextView) view.findViewById(R.id.do_reply);
        final TextView doRetweet = (TextView) view.findViewById(R.id.do_retweet);
        final TextView doFav = (TextView) view.findViewById(R.id.do_fav);
        TextView retweetCount = (TextView) view.findViewById(R.id.retweet_count);
        TextView favCount = (TextView) view.findViewById(R.id.fav_count);

        if (status.getFavoriteCount() > 0) {
            favCount.setText(String.valueOf(status.getFavoriteCount()));
            favCount.setVisibility(View.VISIBLE);
        } else {
            favCount.setText("0");
            favCount.setVisibility(View.INVISIBLE);
        }

        if (status.getRetweetCount() > 0) {
            retweetCount.setText(String.valueOf(status.getRetweetCount()));
            retweetCount.setVisibility(View.VISIBLE);
        } else {
            retweetCount.setText("0");
            retweetCount.setVisibility(View.INVISIBLE);
        }

        doReply.setTypeface(fontello);
        doReply.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(mContext, PostActivity.class);
                String text = "@" + status.getUser().getScreenName() + " ";
                if (mContext.getClass().getName().equals("info.justaway.MainActivity")) {
                    MainActivity activity = (MainActivity) mContext;
                    View singleLineTweet = activity.findViewById(R.id.quick_tweet_layout);
                    if (singleLineTweet != null && singleLineTweet.getVisibility() == View.VISIBLE) {
                        EditText editStatus = (EditText) activity.findViewById(R.id.quick_tweet_edit);
                        editStatus.setText(text);
                        editStatus.setSelection(text.length());
                        editStatus.requestFocus();
                        mApplication.showKeyboard(editStatus);
                        activity.setInReplyToStatusId(status.getId());
                        return;
                    }
                }
                intent.putExtra("status", text);
                intent.putExtra("selection", text.length());
                intent.putExtra("inReplyToStatusId", status.getId());
                mContext.startActivity(intent);
            }
        });

        doRetweet.setTypeface(fontello);
        doRetweet.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Long id = mApplication.getRtId(status);
                if (id != null) {
                    DialogFragment dialog = new DialogFragment() {
                        @Override
                        public Dialog onCreateDialog(Bundle savedInstanceState) {
                            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                            builder.setTitle(R.string.confirm_destroy_retweet);
                            builder.setMessage(status.getText());
                            builder.setPositiveButton(getString(R.string.button_destroy_retweet),
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            mApplication.doDestroyRetweet(status.getId());
                                            doRetweet.setTextColor(Color.parseColor("#666666"));
                                            dismiss();
                                        }
                                    });
                            builder.setNegativeButton(getString(R.string.button_cancel),
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            dismiss();
                                        }
                                    });

                            return builder.create();
                        }
                    };
                    FragmentActivity activity = (FragmentActivity) mContext;
                    dialog.show(activity.getSupportFragmentManager(), "dialog");
                } else {
                    DialogFragment dialog = new DialogFragment() {

                        /**
                         * ダイアログ閉じたあとの処理を定義できるようにしておく
                         */
                        private Runnable mOnDismiss;

                        @Override
                        public Dialog onCreateDialog(Bundle savedInstanceState) {
                            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                            builder.setTitle(R.string.confirm_retweet);
                            builder.setMessage(status.getText());
                            builder.setNeutralButton(getString(R.string.button_quote),
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            FragmentActivity activity = (FragmentActivity) mContext;
                                            EditText editStatus = null;
                                            View singleLineTweet = activity.findViewById(R.id.quick_tweet_layout);
                                            if (singleLineTweet != null && singleLineTweet.getVisibility() == View.VISIBLE) {
                                                editStatus = (EditText) activity.findViewById(R.id.quick_tweet_edit);
                                            }
                                            String text = " https://twitter.com/"
                                                    + status.getUser().getScreenName()
                                                    + "/status/" + String.valueOf(status.getId());
                                            if (editStatus != null) {
                                                editStatus.requestFocus();
                                                editStatus.setText(text);
                                                /**
                                                 * ダイアログ閉じた後じゃないとキーボードを出せない
                                                 */
                                                final View view = editStatus;
                                                mOnDismiss = new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        mApplication.showKeyboard(view);
                                                    }
                                                };
                                                return;
                                            }
                                            Intent intent = new Intent(activity, PostActivity.class);
                                            intent.putExtra("status", text);
                                            intent.putExtra("inReplyToStatusId", status.getId());
                                            startActivity(intent);
                                            dismiss();
                                        }
                                    });
                            builder.setPositiveButton(getString(R.string.button_retweet),
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            mApplication.doRetweet(status.getId());
                                            doRetweet.setTextColor(mContext.getResources()
                                                    .getColor(R.color.holo_green_light));
                                            dismiss();
                                        }
                                    });
                            builder.setNegativeButton(getString(R.string.button_cancel),
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            dismiss();
                                        }
                                    });

                            return builder.create();
                        }

                        @Override
                        public void onDismiss(DialogInterface dialog) {
                            super.onDismiss(dialog);
                            if (mOnDismiss != null) {
                                mOnDismiss.run();
                            }
                        }
                    };
                    FragmentActivity activity = (FragmentActivity) mContext;
                    dialog.show(activity.getSupportFragmentManager(), "dialog");
                }
            }
        });

        doFav.setTypeface(fontello);
        doFav.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mApplication.isFav(status)) {
                    mApplication.doDestroyFavorite(status.getId());
                    doRetweet.setTextColor(Color.parseColor("#666666"));
                } else {
                    mApplication.doFavorite(status.getId());
                    doFav.setTextColor(mContext.getResources().getColor(R.color.holo_orange_light));
                }
            }
        });

        if (mApplication.getRtId(status) != null) {
            doRetweet.setTextColor(mContext.getResources().getColor(R.color.holo_green_light));
        } else {
            doRetweet.setTextColor(Color.parseColor("#666666"));
        }

        if (mApplication.isFav(status)) {
            doFav.setTextColor(mContext.getResources().getColor(R.color.holo_orange_light));
        } else {
            doFav.setTextColor(Color.parseColor("#666666"));
        }

        ((TextView) view.findViewById(R.id.display_name)).setText(status.getUser().getName());
        ((TextView) view.findViewById(R.id.screen_name)).setText("@"
                + status.getUser().getScreenName());
        ((TextView) view.findViewById(R.id.datetime_relative))
                .setText(getRelativeTime(status.getCreatedAt()));
        ((TextView) view.findViewById(R.id.datetime))
                .setText(getAbsoluteTime(status.getCreatedAt()));
        ((TextView) view.findViewById(R.id.via))
                .setText("via " + getClientName(status.getSource()));
        view.findViewById(R.id.via).setVisibility(View.VISIBLE);

        TextView actionIcon = (TextView) view.findViewById(R.id.action_icon);
        actionIcon.setTypeface(fontello);
        TextView actionByName = (TextView) view.findViewById(R.id.action_by_display_name);
        TextView actionByScreenName = (TextView) view.findViewById(R.id.action_by_screen_name);

        // favの場合
        if (favorite != null) {
            actionIcon.setText(R.string.fontello_star);
            actionIcon.setTextColor(mContext.getResources().getColor(R.color.holo_orange_light));
            actionByName.setText(favorite.getName());
            actionByScreenName.setText("@" + favorite.getScreenName());
            view.findViewById(R.id.retweet).setVisibility(View.GONE);
            view.findViewById(R.id.menu_and_via).setVisibility(View.VISIBLE);
            view.findViewById(R.id.action).setVisibility(View.VISIBLE);
        }
        // RTの場合
        else if (retweet != null) {
            // 自分のツイート
            if (userId == status.getUser().getId()) {
                actionIcon.setText(R.string.fontello_retweet);
                actionIcon.setTextColor(mContext.getResources().getColor(R.color.holo_green_light));
                actionByName.setText(retweet.getUser().getName());
                actionByScreenName.setText("@" + retweet.getUser().getScreenName());
                view.findViewById(R.id.retweet).setVisibility(View.GONE);
                view.findViewById(R.id.menu_and_via).setVisibility(View.VISIBLE);
                view.findViewById(R.id.action).setVisibility(View.VISIBLE);
            } else {
                ImageView icon = (ImageView) view.findViewById(R.id.retweet_icon);
                mApplication.displayRoundedImage(retweet.getUser().getProfileImageURL(), icon);
                TextView retweet_by = (TextView) view.findViewById(R.id.retweet_by);
                retweet_by.setText("RT by "
                        + retweet.getUser().getName() + " @" + retweet.getUser().getScreenName());
                view.findViewById(R.id.action).setVisibility(View.GONE);
                view.findViewById(R.id.menu_and_via).setVisibility(View.VISIBLE);
                view.findViewById(R.id.retweet).setVisibility(View.VISIBLE);
            }
        } else {
            // 自分へのリプ
            if (userId == status.getInReplyToUserId()) {
                actionIcon.setText(R.string.fontello_at);
                actionIcon.setTextColor(mContext.getResources().getColor(R.color.holo_red_light));
                actionByName.setText(status.getUser().getName());
                actionByScreenName.setText("@" + status.getUser().getScreenName());
                view.findViewById(R.id.action).setVisibility(View.VISIBLE);
                view.findViewById(R.id.retweet).setVisibility(View.GONE);
            } else {
                view.findViewById(R.id.action).setVisibility(View.GONE);
                view.findViewById(R.id.retweet).setVisibility(View.GONE);
            }
            view.findViewById(R.id.menu_and_via).setVisibility(View.VISIBLE);
        }

        if (status.getUser().isProtected()) {
            ((TextView) view.findViewById(R.id.fontello_lock)).setTypeface(fontello);
            view.findViewById(R.id.fontello_lock).setVisibility(View.VISIBLE);
        } else {
            view.findViewById(R.id.fontello_lock).setVisibility(View.INVISIBLE);
        }
        ImageView icon = (ImageView) view.findViewById(R.id.icon);
        mApplication.displayRoundedImage(status.getUser().getBiggerProfileImageURL(), icon);
        icon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(v.getContext(), ProfileActivity.class);
                intent.putExtra("screenName", status.getUser().getScreenName());
                mContext.startActivity(intent);
            }
        });

        MediaEntity[] medias = retweet != null ? retweet.getMediaEntities() : status
                .getMediaEntities();
        URLEntity[] urls = retweet != null ? retweet.getURLEntities() : status.getURLEntities();
        ArrayList<String> imageUrls = new ArrayList<String>();
        Pattern twitpic_pattern = Pattern.compile("^http://twitpic\\.com/(\\w+)$");
        Pattern twipple_pattern = Pattern.compile("^http://p\\.twipple\\.jp/(\\w+)$");
        Pattern instagram_pattern = Pattern.compile("^http://instagram\\.com/p/([^/]+)/$");
        String statusString = status.getText();
        for (URLEntity url : urls) {
            Pattern p = Pattern.compile(url.getURL());
            Matcher m = p.matcher(statusString);
            statusString = m.replaceAll(url.getExpandedURL());

            Matcher twitpic_matcher = twitpic_pattern.matcher(url.getExpandedURL());
            if (twitpic_matcher.find()) {
                imageUrls.add("http://twitpic.com/show/full/" + twitpic_matcher.group(1));
                continue;
            }
            Matcher twipple_matcher = twipple_pattern.matcher(url.getExpandedURL());
            if (twipple_matcher.find()) {
                imageUrls.add("http://p.twpl.jp/show/orig/" + twipple_matcher.group(1));
                continue;
            }
            Matcher instagram_matcher = instagram_pattern.matcher(url.getExpandedURL());
            if (instagram_matcher.find()) {
                imageUrls.add(url.getExpandedURL() + "media?size=l");
            }
        }
        ((TextView) view.findViewById(R.id.status)).setText(statusString);

        for (MediaEntity media : medias) {
            imageUrls.add(media.getMediaURL());
        }
        LinearLayout images = (LinearLayout) view.findViewById(R.id.images);
        images.removeAllViews();
        if (imageUrls.size() > 0) {
            for (final String url : imageUrls) {
                ImageView image = new ImageView(mContext);
                image.setScaleType(ImageView.ScaleType.CENTER_CROP);
                images.addView(image, new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT, 120));
                mApplication.displayRoundedImage(url, image);
                // 画像タップで拡大表示（ピンチイン・ピンチアウトいつかちゃんとやる）
                image.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent = new Intent(v.getContext(), ScaleImageActivity.class);
                        intent.putExtra("url", url);
                        mContext.startActivity(intent);
                    }
                });
            }
            images.setVisibility(View.VISIBLE);
        } else {
            images.setVisibility(View.GONE);
        }
    }

    private String getClientName(String source) {
        String[] tokens = source.split("[<>]");
        if (tokens.length > 1) {
            return tokens[2];
        } else {
            return tokens[0];
        }
    }

    private String getRelativeTime(Date date) {
        int diff = (int) (((new Date()).getTime() - date.getTime()) / 1000);
        if (diff < 1) {
            return "now";
        } else if (diff < 60) {
            return diff + "s";
        } else if (diff < 3600) {
            return (diff / 60) + "m";
        } else if (diff < 86400) {
            return (diff / 3600) + "h";
        } else {
            return (diff / 86400) + "d";
        }
    }

    private String getAbsoluteTime(Date date) {
        return DATE_FORMAT.format(date);
    }
}
