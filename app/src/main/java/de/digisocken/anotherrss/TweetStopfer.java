package de.digisocken.anotherrss;

import com.twitter.sdk.android.core.Callback;
import com.twitter.sdk.android.core.Result;
import com.twitter.sdk.android.core.TwitterException;
import com.twitter.sdk.android.core.models.Tweet;
import com.twitter.sdk.android.tweetui.TimelineResult;

import java.text.ParseException;

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
            try {
                _refresher.insertTweet(tweet, _query, AnotherRSS.Config.DEFAULT_expunge, _sourceId);
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void failure(TwitterException exception) {
        exception.printStackTrace();
    }
}