package de.digisocken.rss_o_tweet;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.util.Log;

import com.twitter.sdk.android.tweetui.SearchTimeline;
import com.twitter.sdk.android.tweetui.UserTimeline;

import org.w3c.dom.Document;

import java.util.Random;

public class Alarm extends BroadcastReceiver {
    static AsyncTask<Object, Void, Void> asyncTask;
    @SuppressLint("StaticFieldLeak")
    @Override
    public void onReceive(Context context, Intent intent) {

        asyncTask = new AsyncTask<Object, Void, Void>() {
            @Override
            protected Void doInBackground(Object... objs) {
                Context ctx = (Context) objs[0];

                SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(ctx);
                Refresher refresher = Refresher.ME(ctx);

                if (refresher.isOnline()) {
                    if (pref.getBoolean("isRetry", false)) {
                        if (BuildConfig.DEBUG) {
                            refresher.error("Online again!", "repeating alarm set");
                        }
                        Log.d(RssOTweet.TAG, "last retry!");
                        start(ctx);
                    }
                } else {
                    if (BuildConfig.DEBUG) {
                        refresher.error(
                                "not Online",
                                "Retry alarm in seconds: " + RssOTweet.Config.RETRYSEC_AFTER_OFFLINE
                        );
                    }
                    Log.w(RssOTweet.TAG, "Retry alarm in seconds: " + RssOTweet.Config.RETRYSEC_AFTER_OFFLINE);
                    RssOTweet.alarm.retry(ctx, RssOTweet.Config.RETRYSEC_AFTER_OFFLINE);
                    return null;
                }

                refresher._newFeeds.clear();
                String[] urls = pref.getString("rss_url", RssOTweet.urls).split(" ");
                boolean active[] = PreferencesActivity.loadArray("rss_url_act", ctx);

                for (int urli=0; urli < urls.length; urli++) {
                    if (!active[urli]) continue;
                    if (!urls[urli].equals("")) {
                        if (urls[urli].startsWith("#")) {
                            urls[urli] = urls[urli].replace("#","");
                            SearchTimeline searchTimeline = new SearchTimeline.Builder()
                                    .query(urls[urli])
                                    .maxItemsPerRequest(RssOTweet.Config.DEFAULT_TWITTER_MAX)
                                    .build();
                            searchTimeline.next(null, new TweetStopfer(refresher, urls[urli], urli));
                        } else if (urls[urli].startsWith("@")) {
                            urls[urli] = urls[urli].replace("@","");
                            UserTimeline userTimeline = new UserTimeline.Builder()
                                    .screenName(urls[urli])
                                    .maxItemsPerRequest(RssOTweet.Config.DEFAULT_TWITTER_MAX)
                                    .build();
                            userTimeline.next(null, new TweetStopfer(refresher, urls[urli], urli));
                        } else {
                            Document doc = refresher.getDoc(urls[urli], RssOTweet.Config.DEFAULT_expunge);
                            /**
                             * @todo ugly: ignore ".podcast" urls because tey are mostly old
                             */
                            if (urls[urli].toString().endsWith(".podcast")) {
                                refresher.insertToDb(doc, RssOTweet.Config.DEFAULT_autodelete, urli);
                            } else {
                                refresher.insertToDb(doc, RssOTweet.Config.DEFAULT_expunge, urli);
                            }
                        }
                    }
                }

                refresher.sortFeeds();
                if (refresher._newFeeds.size() > 0) {

                    Intent notificationIntent = new Intent(ctx, MainActivity.class);
                    notificationIntent.setFlags(
                            Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP
                    );
                    PendingIntent pi = PendingIntent.getActivity(ctx, 0, notificationIntent, 0);

                    if (RssOTweet.withGui) {
                        refresher.makeNotify(pi);
                    } else {
                        refresher.makeNotifies(pi);
                    }
                    Intent intent = new Intent(ctx.getString(R.string.serviceHasNews));
                    intent.putExtra("count", refresher._newFeeds.size());
                    ctx.sendBroadcast(intent);
                }
                return null;
            }
        };
        asyncTask.execute(context);
    }

    public void retry(Context context, long sec) {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent i = new Intent(context, Alarm.class);
        pref.edit().putBoolean("isRetry", true).apply();
        PendingIntent pi = PendingIntent.getBroadcast(context, 0, i, 0);
        am.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + sec * 1000L, pi);
    }

    public void start(Context context) {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);

        long refreshInterval = Long.parseLong(
                pref.getString("rss_sec", RssOTweet.Config.DEFAULT_rsssec)
        ) * 1000L;

        // never
        if (refreshInterval == 1000) return;

        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent i = new Intent(context, Alarm.class);
        pref.edit().putBoolean("isRetry", false).apply();
        PendingIntent pi = PendingIntent.getBroadcast(context, 0, i, 0);

        long mod = 0;
        if (refreshInterval >= 240000) { // more than 4 minutes
            Random r = new Random(System.currentTimeMillis());
            mod = r.nextInt(360000) - 180000; // plusminus 3min
        }
        am.setInexactRepeating(
                AlarmManager.ELAPSED_REALTIME_WAKEUP,
                500L,
                refreshInterval + mod,
                pi
        );
        Log.d(RssOTweet.TAG, "Alarm started.");
    }

    /**
     * Stop.
     *
     * @param context the context
     */
    public void stop(Context context) {
        Intent i = new Intent(context, Alarm.class);
        PendingIntent pi = PendingIntent.getBroadcast(context, 0, i, 0);
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        am.cancel(pi);
        Log.d(RssOTweet.TAG, "Alarm stopped.");
    }

    /**
     * Restart. Wird von {@link MainActivity#onCreate(Bundle)} genutzt, um beim Starten der
     * App nach neuen Feeds zu schauen.
     *
     * @param context the context
     */
    public void restart(Context context) {
        stop(context);
        start(context);
    }
}
