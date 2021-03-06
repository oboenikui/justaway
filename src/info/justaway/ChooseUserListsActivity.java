package info.justaway;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.view.KeyEvent;
import android.widget.CheckBox;
import android.widget.ListView;

import java.util.ArrayList;

import info.justaway.adapter.SubscribeUserListAdapter;
import info.justaway.task.UserListsLoader;
import twitter4j.ResponseList;
import twitter4j.UserList;

public class ChooseUserListsActivity extends FragmentActivity implements
        LoaderManager.LoaderCallbacks<ResponseList<UserList>> {

    private SubscribeUserListAdapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_choose_user_lists);

        ListView listView = (ListView) findViewById(R.id.list);

        mAdapter = new SubscribeUserListAdapter(this, R.layout.row_subscribe_user_list);

        listView.setAdapter(mAdapter);

        // 起動時に読み込んだユーザーリストがある場合は新たにAPIを叩かない
        ResponseList<UserList> userLists = JustawayApplication.getApplication().getUserLists();
        if (userLists != null) {
            for (UserList userList : userLists) {
                mAdapter.add(userList);
            }
        } else {
            getSupportLoaderManager().initLoader(0, null, this);
        }
    }

    @Override
    public Loader<ResponseList<UserList>> onCreateLoader(int arg0, Bundle arg1) {
        return new UserListsLoader(this);
    }

    @Override
    public void onLoadFinished(Loader<ResponseList<UserList>> arg0, ResponseList<UserList> userLists) {
        if (userLists != null) {
            for (UserList userList : userLists) {
                mAdapter.add(userList);
            }
        }
    }

    @Override
    public void onLoaderReset(Loader<ResponseList<UserList>> arg0) {
    }

    /**
     * finish前に色々セットしておく、ここでセットした値は onActivityResult で取れる
     */
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {

            ArrayList<Integer> lists = new ArrayList<Integer>();

            // 有効なチェックボックスからリストIDを取得
            ListView listView = (ListView) findViewById(R.id.list);
            int count = listView.getChildCount();
            for (int i = 0; i < count; i++) {
                CheckBox checkbox = (CheckBox) listView.getChildAt(i);
                if (checkbox != null && checkbox.isChecked()) {
                    lists.add((Integer) checkbox.getTag());
                }
            }

            if (lists.size() > 0) {
                Intent data = new Intent();
                Bundle bundle = new Bundle();
                bundle.putIntegerArrayList("lists", lists);
                data.putExtras(bundle);
                setResult(RESULT_OK, data);
            } else {
                setResult(RESULT_CANCELED);
            }
            finish();
        }
        return false;
    }
}
