package de.digisocken.rss_o_tweet;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Mit diesem BroadcastReceiver wird der AlarmManager gestartet, sobald
 * das Gerät eingeschaltet wird.
 */
public class DeviceBootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {

        if (intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)) {
            if (RssOTweet.alarm == null) RssOTweet.alarm = new Alarm();
            RssOTweet.alarm.start(context);
        }
    }
}
