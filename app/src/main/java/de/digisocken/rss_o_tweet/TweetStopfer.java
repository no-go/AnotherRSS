package de.digisocken.rss_o_tweet;

import com.twitter.sdk.android.core.Callback;
import com.twitter.sdk.android.core.Result;
import com.twitter.sdk.android.core.TwitterException;
import com.twitter.sdk.android.core.models.Tweet;
import com.twitter.sdk.android.tweetui.TimelineResult;

public class TweetStopfer extends Callback<TimelineResult<Tweet>> {
    private Refresher _refresher;
    private String _query;
    private int _sourceId;

    public TweetStopfer(Refresher refresher, String q, int sourceId) {
        super();
        _sourceId = sourceId;
        _refresher = refresher;
        _query = q;
    }

    @Override
    public void success(Result<TimelineResult<Tweet>> result) {
        for(Tweet tweet : result.data.items) {
            new InsertThread(tweet).start();
        }
    }

    @Override
    public void failure(TwitterException exception) {
        exception.printStackTrace();
    }

    class InsertThread extends Thread {
        Tweet tweet;

        public InsertThread(Tweet tweet) {
            this.tweet = tweet;
        }

        public void run() {
            try  {
                _refresher.insertTweet(tweet, _query, RssOTweet.Config.DEFAULT_expunge, _sourceId);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}