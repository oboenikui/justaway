package info.justaway;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ListView;

import info.justaway.adapter.TwitterAdapter;
import info.justaway.model.Row;
import twitter4j.Status;
import twitter4j.Twitter;

/**
 * Created by aska on 2013/12/21.
 */
public class StatusActivity extends FragmentActivity {

    private ProgressDialog mProgressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN);

        setContentView(R.layout.activity_status);

        ListView listView = (ListView) findViewById(R.id.list);

        // コンテキストメニューを使える様にする為の指定、但しデフォルトではロングタップで開く
        registerForContextMenu(listView);

        // Status(ツイート)をViewに描写するアダプター
        TwitterAdapter adapter = new TwitterAdapter(this, R.layout.row_tweet);
        listView.setAdapter(adapter);

        // シングルタップでコンテキストメニューを開くための指定
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                view.showContextMenu();
            }
        });

        // インテント経由での起動をサポート
        Intent intent = getIntent();
        long statusId;
        if (Intent.ACTION_VIEW.equals(intent.getAction())) {
            statusId = Long.parseLong(intent.getData().getLastPathSegment());
        } else {
            statusId = intent.getLongExtra("id", -1L);
        }

        if (statusId > 0) {
            Twitter twitter = JustawayApplication.getApplication().getTwitter();
            showProgressDialog("Loading...");
            new LoadTalk(twitter, adapter).execute(statusId);
        }
    }

    public void onCreateContextMenu(ContextMenu menu, View view, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, view, menuInfo);
        JustawayApplication application = JustawayApplication.getApplication();
        application.onCreateContextMenuForStatus(menu, view, menuInfo);
    }

    public boolean onContextItemSelected(MenuItem item) {
        JustawayApplication application = JustawayApplication.getApplication();
        return application.onContextItemSelected(this, item);
    }

    private void showProgressDialog(String message) {
        mProgressDialog = new ProgressDialog(this);
        mProgressDialog.setMessage(message);
        mProgressDialog.show();
    }

    private void dismissProgressDialog() {
        if (mProgressDialog != null)
            mProgressDialog.dismiss();
    }

    private class LoadTalk extends AsyncTask<Long, Void, Status> {

        private Twitter twitter;
        private TwitterAdapter adapter;

        public LoadTalk(Twitter twitter, TwitterAdapter adapter) {
            super();
            this.twitter = twitter;
            this.adapter = adapter;
        }

        @Override
        protected twitter4j.Status doInBackground(Long... params) {
            try {
                return twitter.showStatus(params[0]);
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        @Override
        protected void onPostExecute(twitter4j.Status status) {
            dismissProgressDialog();
            if (status != null) {
                adapter.add(Row.newStatus(status));
                adapter.notifyDataSetChanged();
                Long inReplyToStatusId = status.getInReplyToStatusId();
                if (inReplyToStatusId > 0) {
                    new LoadTalk(twitter, adapter).execute(inReplyToStatusId);
                }
            } else {
                JustawayApplication.showToast(R.string.toast_load_data_failure);
            }
        }
    }
}
