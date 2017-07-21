package de.digisocken.anotherrss;

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
            if (AnotherRSS.alarm == null) AnotherRSS.alarm = new Alarm();
            AnotherRSS.alarm.start(context);
        }
    }
}
