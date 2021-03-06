package info.justaway;


import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;

import twitter4j.StatusUpdate;
import twitter4j.Twitter;

public class PostActivity extends FragmentActivity {

    private Context mContext;
    private Twitter mTwitter;
    private EditText mEditText;
    private TextView mTextView;
    private Button mTweetButton;
    private Button mImgButton;
    private Button mSuddenlyButton;
    private Button mDraftButton;
    private ProgressDialog mProgressDialog;
    private Long mInReplyToStatusId;
    private File mImgPath;
    private DraftFragment mDraftDialog;
    private Typeface mFontello;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post);
        mContext = this;

        JustawayApplication application = JustawayApplication.getApplication();

        mFontello = Typeface.createFromAsset(getAssets(), "fontello.ttf");

        mEditText = (EditText) findViewById(R.id.status);
        mTextView = (TextView) findViewById(R.id.count);
        mTweetButton = (Button) findViewById(R.id.tweet);
        mImgButton = (Button) findViewById(R.id.img);
        mSuddenlyButton = (Button) findViewById(R.id.suddenly);
        mDraftButton = (Button) findViewById(R.id.draft);
        mTwitter = application.getTwitter();

        mTweetButton.setTypeface(mFontello);
        mImgButton.setTypeface(mFontello);
        mSuddenlyButton.setTypeface(mFontello);
        mDraftButton.setTypeface(mFontello);

        Intent intent = getIntent();
        String status = intent.getStringExtra("status");
        if (status != null) {
            mEditText.setText(status);
            updateCount(status);
        }
        int selection = intent.getIntExtra("selection", 0);
        if (selection > 0) {
            mEditText.setSelection(selection);
        }
        mInReplyToStatusId = intent.getLongExtra("inReplyToStatusId", 0);

        if (Intent.ACTION_VIEW.equals(intent.getAction())) {
            String text = intent.getData().getQueryParameter("text");
            String url = intent.getData().getQueryParameter("url");
            String hashtags = intent.getData().getQueryParameter("hashtags");
            if (text == null) {
                text = "";
            }
            if (url != null) {
                text += " " + url;
            }
            if (hashtags != null) {
                text += " #" + hashtags;
            }
            mEditText.setText(text);
        }

        if (Intent.ACTION_SEND.equals(intent.getAction())) {
            if (intent.getExtras().get(Intent.EXTRA_STREAM) != null) {
                Uri imgUri = (Uri) intent.getExtras().get(Intent.EXTRA_STREAM);
                uriToFile(imgUri);
            } else {
                String pageUri = intent.getExtras().getString(Intent.EXTRA_TEXT);
                String pageTitle = intent.getExtras().getString(Intent.EXTRA_SUBJECT);
                if (pageTitle == null) {
                    pageTitle = "";
                }
                if (pageUri != null) {
                    pageTitle += " " + pageUri;
                }
                mEditText.setText(pageTitle);
            }
        }

        mTweetButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showProgressDialog(getString(R.string.progress_sending));
                StatusUpdate superSugoi = new StatusUpdate(mEditText.getText().toString());
                if (mInReplyToStatusId > 0) {
                    superSugoi.setInReplyToStatusId(mInReplyToStatusId);
                }
                if (mImgPath != null) {
                    superSugoi.setMedia(mImgPath);
                }
                new PostTask().execute(superSugoi);
            }
        });

        mSuddenlyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String text = mEditText.getText().toString();
                int selectStart = mEditText.getSelectionStart();
                int selectEnd = mEditText.getSelectionEnd();

                String totsuzen;
                if (selectStart != selectEnd) {
                    totsuzen = text.substring(selectStart, selectEnd) + "\n";
                } else {
                    totsuzen = text + "\n";
                }

                int i;
                String top = "";
                String under = "";
                int j = 0;
                String generateTotsu = "";

                // 改行文字がある場所を見つけて上と下を作る
                for (i = 0; totsuzen.charAt(i) != '\n'; i++) {
                    int codeunit = totsuzen.codePointAt(i);
                    if (0xffff < codeunit) {
                        i++;
                    }
                    top += "人";
                    under += "^Y";
                }
                // 突然死したいテキストの文字をひとつづつ見る
                for (i = 0; totsuzen.length() > i; i++) {
                    // 一文字取り出して改行文字なのかチェック
                    if (totsuzen.charAt(i) == '\n') {
                        String gen = "＞ " + totsuzen.substring(j, i) + " ＜\n";
                        i++;
                        j = i;
                        generateTotsu = generateTotsu.concat(gen);
                    }
                }
                if (selectStart != selectEnd) {
                    mEditText.setText(text.substring(0, selectStart) + "＿" + top + "＿\n" + generateTotsu + "￣" + under + "￣" + text.substring(selectEnd));
                } else {
                    mEditText.setText("＿" + top + "＿\n" + generateTotsu + "￣" + under + "￣");
                }
            }
        });

        mImgButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_PICK);
                intent.setType("image/*");
                startActivityForResult(intent, 1);
            }
        });

        mDraftButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mDraftDialog = new DraftFragment();
                mDraftDialog.show(getSupportFragmentManager(), "dialog");
            }
        });

        // 文字数をカウントしてボタンを制御する
        mEditText.addTextChangedListener(new TextWatcher() {
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateCount(s.toString());
            }

            public void afterTextChanged(Editable s) {
            }

            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            Uri uri = data.getData();
            uriToFile(uri);
        }
    }

    private void uriToFile(Uri uri) {
        ContentResolver cr = getContentResolver();
        String[] columns = {MediaStore.Images.Media.DATA};
        Cursor c = cr.query(uri, columns, null, null, null);
        c.moveToFirst();
        File path = new File(c.getString(0));
        if (!path.exists()) {
            return;
        }
        this.mImgPath = path;
        JustawayApplication.showToast(R.string.toast_set_image_success);
        mImgButton.setTextColor(getResources().getColor(R.color.holo_blue_bright));
    }

    private void updateCount(String str) {
        int textColor;
        int length = 140 - str.codePointCount(0, str.length());
        // 140文字をオーバーした時は文字数を赤色に
        if (length < 0) {
            textColor = Color.RED;
        } else {
            textColor = Color.WHITE;
        }
        mTextView.setTextColor(textColor);
        mTextView.setText(String.valueOf(length));

        // 文字数が0文字または140文字以上の時はボタンを無効
        if (str.codePointCount(0, str.length()) == 0
                || str.codePointCount(0, str.length()) > 140) {
            mTweetButton.setEnabled(false);
        } else {
            mTweetButton.setEnabled(true);
        }
    }

    private class PostTask extends AsyncTask<StatusUpdate, Void, Boolean> {
        @Override
        protected Boolean doInBackground(StatusUpdate... params) {
            StatusUpdate super_sugoi = params[0];
            try {
                mTwitter.updateStatus(super_sugoi);
                return true;
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean success) {
            dismissProgressDialog();
            if (success) {
                mEditText.setText("");
                finish();
            } else {
                JustawayApplication.showToast(R.string.toast_update_status_failure);
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.post, menu);
        return true;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (mEditText.getText().length() != 0) {
                new AlertDialog.Builder(PostActivity.this)
                        .setTitle(R.string.tweet_draft)
                        .setPositiveButton(
                                R.string.save,
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        // 下書きとして保存する
                                        SaveLoadTraining saveLoadTraining = new SaveLoadTraining();
                                        ArrayList<String> draftList = saveLoadTraining.loadArray();
                                        draftList.add(mEditText.getText().toString());
                                        saveLoadTraining.saveArray(draftList);

                                        finish();
                                    }
                                })
                        .setNegativeButton(
                                R.string.destroy,
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        finish();
                                    }
                                })
                        .show();
            } else {
                finish();
            }
            return true;
        }
        return false;

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.tweet_clear:
                mEditText.setText("");
                break;
            case R.id.tweet_battery:
                Intent batteryIntent = getApplicationContext().registerReceiver(null,
                        new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
                int level = batteryIntent.getIntExtra("level", 0);
                int scale = batteryIntent.getIntExtra("scale", 100);
                int status = batteryIntent.getIntExtra("status", 0);
                int battery = level * 100 / scale;
                String model = Build.MODEL;

                switch (status) {
                    case BatteryManager.BATTERY_STATUS_FULL:
                        mEditText.setText(model + " のバッテリー残量:" + battery + "% (0゜・◡・♥​​)");
                        break;
                    case BatteryManager.BATTERY_STATUS_CHARGING:
                        mEditText.setText(model + " のバッテリー残量:" + battery + "% 充電なう(・◡・♥​​)");
                        break;
                    default:
                        if (level <= 10) {
                            mEditText.setText(model + " のバッテリー残量:" + battery + "% (◞‸◟)");
                        } else {
                            mEditText.setText(model + " のバッテリー残量:" + battery + "% (・◡・♥​​)");
                        }
                        break;
                }
                break;
        }
        return true;
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

    public class DraftFragment extends DialogFragment {

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {

            Activity activity = getActivity();
            Dialog dialog = new Dialog(activity);
            dialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
            dialog.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN);
            dialog.setContentView(R.layout.fragment_list);
            ListView listView = (ListView) dialog.findViewById(R.id.list);

            // 下書きをViewに描写するアダプター
            DraftAdapter adapter = new DraftAdapter(activity, R.layout.row_draft);
            listView.setAdapter(adapter);

            SaveLoadTraining saveLoadTraining = new SaveLoadTraining();
            ArrayList<String> draftList = saveLoadTraining.loadArray();

            for (String draft : draftList) {
                adapter.add(draft);
            }

            return dialog;
        }
    }

    public class DraftAdapter extends ArrayAdapter<String> {

        private ArrayList<String> mDraftLists = new ArrayList<String>();
        private Context mContext;
        private LayoutInflater mInflater;
        private int mLayout;

        public DraftAdapter(Context context, int textViewResourceId) {
            super(context, textViewResourceId);
            this.mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            this.mContext = context;
            this.mLayout = textViewResourceId;
        }

        @Override
        public void add(String draft) {
            super.add(draft);
            mDraftLists.add(draft);
        }

        public void remove(int position) {
            super.remove(mDraftLists.remove(position));
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {

            // ビューを受け取る
            View view = convertView;
            if (view == null) {
                // 受け取ったビューがnullなら新しくビューを生成
                view = mInflater.inflate(this.mLayout, null);
            }

            final String draft = mDraftLists.get(position);

            ((TextView) view.findViewById(R.id.draft)).setText(draft);
            ((TextView) view.findViewById(R.id.trash)).setTypeface(mFontello);

            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mEditText.setText(draft);
                    mDraftDialog.dismiss();
                    mDraftLists.remove(position);
                    SaveLoadTraining saveLoadTraining = new SaveLoadTraining();
                    saveLoadTraining.saveArray(mDraftLists);

                }
            });

            view.findViewById(R.id.trash).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    remove(position);
                    SaveLoadTraining saveLoadTraining = new SaveLoadTraining();
                    saveLoadTraining.saveArray(mDraftLists);
                }
            });
            return view;
        }
    }

    /**
     * SharedPreferencesにArrayListを突っ込む
     */
    public class SaveLoadTraining {

        private Context context;
        public static final String PREFS_NAME = "DraftListFile";
        private ArrayList<String> list;

        public SaveLoadTraining() {
            this.context = mContext;
        }

        public void saveArray(ArrayList<String> list) {
            this.list = list;

            SharedPreferences settings = context.getSharedPreferences(PREFS_NAME, 0);
            SharedPreferences.Editor editor = settings.edit();

            int size = list.size();
            editor.putInt("list_size", size);

            for (int i = 0; i < size; i++) {
                editor.remove("list_" + i);
            }
            for (int i = 0; i < size; i++) {
                editor.putString("list_" + i, list.get(i));
            }
            editor.commit();
        }

        public ArrayList<String> loadArray() {
            SharedPreferences file = context.getSharedPreferences(PREFS_NAME, 0);
            list = new ArrayList<String>();
            int size = file.getInt("list_size", 0);

            for (int i = 0; i < size; i++) {
                String draft = file.getString("list_" + i, null);
                list.add(draft);
            }
            return list;
        }
    }
}
