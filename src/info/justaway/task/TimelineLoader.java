package info.justaway.task;

import android.content.Context;

import info.justaway.JustawayApplication;
import twitter4j.ResponseList;
import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterException;

public class TimelineLoader extends AbstractAsyncTaskLoader<ResponseList<Status>> {

    public TimelineLoader(Context context) {
        super(context);
    }

    @Override
    public ResponseList<Status> loadInBackground() {
        try {
            Twitter twitter = JustawayApplication.getApplication().getTwitter();
            return twitter.getHomeTimeline();
        } catch (TwitterException e) {
            e.printStackTrace();
            return null;
        }
    }
}
