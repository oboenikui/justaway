package info.justaway.fragment.profile;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.ProgressBar;

import info.justaway.BaseActivity;
import info.justaway.JustawayApplication;
import info.justaway.ProfileActivity;
import info.justaway.R;
import info.justaway.adapter.TwitterAdapter;
import info.justaway.model.Row;
import twitter4j.Paging;
import twitter4j.ResponseList;
import twitter4j.Status;
import twitter4j.User;


/**
 * ユーザーのタイムライン
 */
public class UserTimelineFragment extends Fragment {

    private TwitterAdapter mAdapter;
    private ListView mListView;
    private ProgressBar mFooter;
    private User mUser;
    private Boolean mAutoLoader = false;
    private int mPage = 1;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.list, container, false);
        if (v == null) {
            return null;
        }

        mUser = (User) getArguments().getSerializable("user");

        // リストビューの設定
        mListView = (ListView) v.findViewById(R.id.list_view);
        mListView.setVisibility(View.GONE);

        // コンテキストメニューを使える様にする為の指定、但しデフォルトではロングタップで開く
        registerForContextMenu(mListView);

        mFooter = (ProgressBar) v.findViewById(R.id.guruguru);

        // Status(ツイート)をViewに描写するアダプター
        mAdapter = new TwitterAdapter(getActivity(), R.layout.row_tweet);
        mListView.setAdapter(mAdapter);

        // シングルタップでコンテキストメニューを開くための指定
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                view.showContextMenu();
            }
        });
        new UserTimelineTask().execute(mUser.getScreenName());

        final ProfileActivity profileActivity = (ProfileActivity) getActivity();
        mListView.setOnScrollListener(new AbsListView.OnScrollListener() {

            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                // 最後までスクロールされたかどうかの判定
                if (totalItemCount == firstVisibleItem + visibleItemCount) {
                    additionalReading();
                }
                if (firstVisibleItem > 2) {
                    profileActivity.findViewById(R.id.frame).setVisibility(View.GONE);
                }
            }
        });
        return v;
    }

    public void onCreateContextMenu(ContextMenu menu, View view, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, view, menuInfo);
        BaseActivity baseActivity = (BaseActivity) getActivity();
        baseActivity.onCreateContextMenuForStatus(menu, view, menuInfo);
    }

    public boolean onContextItemSelected(MenuItem item) {
        BaseActivity baseActivity = (BaseActivity) getActivity();
        return baseActivity.onContextItemSelected(item);
    }

    private void additionalReading() {
        if (!mAutoLoader) {
            return;
        }
        mFooter.setVisibility(View.VISIBLE);
        mAutoLoader = false;
        new UserTimelineTask().execute(mUser.getScreenName());
    }

    private class UserTimelineTask extends AsyncTask<String, Void, ResponseList<Status>> {
        @Override
        protected ResponseList<twitter4j.Status> doInBackground(String... params) {
            try {
                ResponseList<twitter4j.Status> statuses = JustawayApplication.getApplication().getTwitter().getUserTimeline(params[0], new Paging(mPage));
                return statuses;
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        @Override
        protected void onPostExecute(ResponseList<twitter4j.Status> statuses) {
            mFooter.setVisibility(View.GONE);
            if (statuses == null || statuses.size() == 0) {
                return;
            }
            for (twitter4j.Status status : statuses) {
                mAdapter.add(Row.newStatus(status));
            }
            mPage++;
            mAutoLoader = true;
            mListView.setVisibility(View.VISIBLE);
        }
    }
}
