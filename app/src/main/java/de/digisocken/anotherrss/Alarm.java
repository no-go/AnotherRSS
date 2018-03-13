package de.digisocken.anotherrss;

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

import org.w3c.dom.Document;

import java.util.Random;

public class Alarm extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {

        AsyncTask<Object, Void, Void> asyncTask = new AsyncTask<Object, Void, Void>() {
            @Override
            protected Void doInBackground(Object... objs) {
                Context ctx = (Context) objs[0];

                SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(ctx);
                Refresher refresher = Refresher.ME(ctx);

                if (refresher.isOnline()) {
                    if (pref.getBoolean("isRetry", false)) {
                        if (pref.getBoolean("offline_hint", false)) {
                            refresher.error("Online again!", "repeating alarm set");
                        }
                        Log.d(AnotherRSS.TAG, "last retry!");
                        start(ctx);
                    }
                } else {
                    if (pref.getBoolean("offline_hint", false)) {
                        refresher.error(
                                "not Online",
                                "Retry alarm in seconds: " + AnotherRSS.Config.RETRYSEC_AFTER_OFFLINE
                        );
                    }
                    Log.w(AnotherRSS.TAG, "Retry alarm in seconds: " + AnotherRSS.Config.RETRYSEC_AFTER_OFFLINE);
                    AnotherRSS.alarm.retry(ctx, AnotherRSS.Config.RETRYSEC_AFTER_OFFLINE);
                    return null;
                }

                refresher._newFeeds.clear();
                String[] urls = pref.getString("rss_url", AnotherRSS.urls).split(" ");
                boolean active[] = PreferencesActivity.loadArray("rss_url_act", ctx);

                for (int urli=0; urli < urls.length; urli++) {
                    if (!active[urli]) continue;
                    if (!urls[urli].equals("")) {
                        Document doc = refresher.getDoc(urls[urli], AnotherRSS.Config.DEFAULT_expunge);
                        refresher.insertToDb(doc, AnotherRSS.Config.DEFAULT_expunge, urli);
                    }
                }

                refresher.sortFeeds();
                if (refresher._newFeeds.size() > 0) {

                    Intent notificationIntent = new Intent(ctx, MainActivity.class);
                    notificationIntent.setFlags(
                            Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP
                    );
                    PendingIntent pi = PendingIntent.getActivity(ctx, 0, notificationIntent, 0);

                    if (AnotherRSS.withGui) {
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
                pref.getString("rss_sec", AnotherRSS.Config.DEFAULT_rsssec)
        ) * 1000L;

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
        Log.d(AnotherRSS.TAG, "Alarm started.");
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
        Log.d(AnotherRSS.TAG, "Alarm stopped.");
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
