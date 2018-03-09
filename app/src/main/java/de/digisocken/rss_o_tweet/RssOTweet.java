package de.digisocken.rss_o_tweet;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.twitter.sdk.android.core.DefaultLogger;
import com.twitter.sdk.android.core.Twitter;
import com.twitter.sdk.android.core.TwitterAuthConfig;
import com.twitter.sdk.android.core.TwitterConfig;

import java.util.Calendar;

/**
 * Der Einstiegspunkt des Launchers.
 * In der Applications-Klasse befinden sich auch die initalen
 * Konfigurationen der SharedPreferences sowie einige Konstanten.
 *
 * @author Jochen Peters
 */
public class RssOTweet extends Application {
    public static boolean showAdditionalFeed = true;
    public static String query = "";

    /*
http://feeds.bbci.co.uk/news/world/europe/rss.xml
http://rss.nytimes.com/services/xml/rss/nyt/HomePage.xml
http://feeds.t-online.de/rss/nachrichten
http://www.wz.de/cmlink/wz-rss-uebersicht-1.516698
http://www.deutschlandfunk.de/die-nachrichten.353.de.rss
http://www.tagesschau.de/xml/rss2
http://www.taz.de/!p4608;rss/
https://www.heise.de/security/news/news-atom.xml
https://www.amnesty.de/rss/news
http://digisocken.de/_p/wdrWetter/?rss=true
https://www.umwelt.nrw.de/rss.xml
http://feeds.reuters.com/Reuters/UKWorldNews
http://feeds.reuters.com/reuters/scienceNews?format=xml
http://www.wetterleitstelle.de/nordrhein-westfalen.xml

     */
    public static final String urls =
            "http://www.tagesschau.de/xml/rss2 " +
                    "http://www.taz.de/!p4608;rss/ " +
                    "http://www.deutschlandfunk.de/die-nachrichten.353.de.rss " +
                    "http://digisocken.de/_p/wdrWetter/?rss=true " +
                    "@faznet " +
                    "@MartinSonneborn " +
                    "#attiny85 " +
                    "@evalodde";

    public static class Config {
        /**
         * really delete old database entries (marked as deleted)
         * older than Config.DEFAULT_expunge days
         */
        public static final int DEFAULT_autodelete = 6;
        public static final int DEFAULT_expunge = 3;
        public static final String DEFAULT_rsssec = "10800";
        public static final String DEFAULT_notifySound = "2";
        public static final String DEFAULT_notifyColor = "#FF00FFFF";
        public static final String DEFAULT_notifyType = "2";
        public static final int DEFAULT_NIGHT_START = 18;
        public static final int DEFAULT_NIGHT_STOP = 6;
        public static final String SEARCH_HINT_COLOR = "#FFAA00";
        public static final float DEFAULT_FONT_SIZE = 14.0f;
        public static final int DEFAULT_MAX_IMG_WIDTH = 160;
        public static final float IMG_ROUND = 5f;
        public static final float TWEET_IMG_ROUND = 40f;
        public static final boolean DEFAULT_ignoreRT = true;
        public static final boolean DEFAULT_tweetUsersImgOrg = false;

        public static final String DEFAULT_regexAll = "";
        public static final String DEFAULT_regexTo = "";

        public static final int DEFAULT_TWITTER_MAX = 10;

        /**
         * sollte eine Verbindung nicht zu sande kommen, wird ein neuer
         * Alarm in {@value #RETRYSEC_AFTER_OFFLINE} sec ausgelöst
         */
        public static final long RETRYSEC_AFTER_OFFLINE = 75L;
    }

    public static Alarm alarm = null;

    /**
     * So kann der {@link Refresher} erkennen, ob er nur im Hintergrund läuft.
     * Wäre withGui auf true, wird nur eine HeadUp Notifikation gezeigt.
     * An dieser Stelle wird klar, dass der Alarm <i>doch</i> auf this zugreifen kann (?)
     */
    public static boolean withGui = false;
    public static final String TAG = RssOTweet.class.getSimpleName();
    private static Context contextOfApplication;

    @Override
    public void onCreate() {
        super.onCreate();
        contextOfApplication = getApplicationContext();

        SharedPreferences mPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        if (!mPreferences.contains("ignoreRT")) {
            mPreferences.edit().putBoolean("ignoreRT", Config.DEFAULT_ignoreRT).commit();
        }
        if (!mPreferences.contains("tweetUsersImgOrg")) {
            mPreferences.edit().putBoolean("tweetUsersImgOrg", Config.DEFAULT_tweetUsersImgOrg).commit();
        }
        if (!mPreferences.contains("rss_url")) {
            mPreferences.edit().putString("rss_url", RssOTweet.urls).commit();
        }
        if (!mPreferences.contains("regexAll")) {
            mPreferences.edit().putString("regexAll", Config.DEFAULT_regexAll).commit();
        }
        if (!mPreferences.contains("regexTo")) {
            mPreferences.edit().putString("regexTo", Config.DEFAULT_regexTo).commit();
        }
        if (!mPreferences.contains("nightmode_use_start")) {
            mPreferences.edit().putInt("nightmode_use_start", Config.DEFAULT_NIGHT_START).commit();
        }
        if (!mPreferences.contains("nightmode_use_stop")) {
            mPreferences.edit().putInt("nightmode_use_stop", Config.DEFAULT_NIGHT_STOP).commit();
        }
        if (!mPreferences.contains("autodelete")) {
            mPreferences.edit().putInt("autodelete", Config.DEFAULT_autodelete).commit();
        }

        TwitterConfig config = new TwitterConfig.Builder(this)
                .logger(new DefaultLogger(Log.DEBUG))
                .twitterAuthConfig(new TwitterAuthConfig(
                        mPreferences.getString("CONSUMER_KEY",""),
                        mPreferences.getString("CONSUMER_SECRET","")
                ))
                .debug(BuildConfig.DEBUG)
                .build();
        Twitter.initialize(config);

        if (alarm == null) alarm = new Alarm();
    }

    public static Context getContextOfApplication() {
        return contextOfApplication;
    }

    public static boolean inTimeSpan(int startH, int stopH) {
        int nowH = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        if (startH == stopH && startH == nowH) return true;
        if (startH > stopH && (nowH <= stopH || nowH >= startH)) return true;
        if (startH < stopH && nowH >= startH && nowH <= stopH) return true;
        return false;
    }
}
