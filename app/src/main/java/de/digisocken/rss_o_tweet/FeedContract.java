package de.digisocken.rss_o_tweet;

import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.preference.PreferenceManager;
import android.provider.BaseColumns;
import android.text.Html;
import android.text.Spanned;
import android.util.Log;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;


/**
 * FeedContract enthält wichtige DB Konstanten sowie Funktionen, die beim
 * verarbeiten von Daten innerhalb der Feeds notwenig sind, um diese
 * in bzw aus der Datenbank zu bekommen.
 *
 * @see FeedContentProvider
 * @see FeedCursorAdapter
 * @see FeedHelper
 */
public class FeedContract {

    /**
     * Feeds enthält die Datenbank-Spalten und den Tabellen Name
     */
    public static class Feeds implements BaseColumns {
        public static final String TABLE_NAME = "feeds";

        public static final String COLUMN_Title = "feed_title";
        public static final String COLUMN_Date = "feed_date";
        public static final String COLUMN_Link = "feed_link";
        public static final String COLUMN_Body = "feed_body";
        public static final String COLUMN_Image = "feed_image";
        public static final String COLUMN_Source = "feed_source";
        public static final String COLUMN_Souname = "feed_souname";
        public static final String COLUMN_Deleted = "feed_deleted";
        public static final String COLUMN_Flag = "feed_isnew";
    }

    public static class Flag {
        public static final int NEW = 1;
        public static final int READED = 0;
        public static final int FAVORITE = 2;

        public static final int VISIBLE = 0;
        public static final int DELETED = 1;
    }

    // Useful SQL query parts
    private static final String TEXT_TYPE = " TEXT";
    private static final String INTEGER_TYPE = " INTEGER";
    private static final String DATE_TYPE = " DATETIME";
    private static final String IMAGE_TYPE = " BLOB";
    private static final String COMMA_SEP = ",";

    public final static long MILLIS_PER_DAY = 24 * 60 * 60 * 1000L;

    /**
     * Die Datenbank will für DATETIME dieses Format {@value #DATABASE_DATETIME_FORMAT}
     */
    public static final String DATABASE_DATETIME_FORMAT = "yyyy-MM-dd HH:mm:ss";

    public static SimpleDateFormat tweetFormatDate = new SimpleDateFormat("EEE MMM dd HH:mm:ss z yyyy", Locale.ENGLISH);

    /**
     * Das ist das Format, was von Feeds als String im pubDate TAG erwartet wird.
     * Bei Abweichungen von FEEDRAW_DATETIME_FORMAT wird das aktuelle Datum für neue Feeds genommen.
     *
     * @see FeedContract#rawToDate(String)
     */
    public static final String[] FEEDRAW_DATETIME_FORMAT = {
            "yyyy-MM-dd'T'HH:mm:ssZ",
            "d M yyyy HH:mm:ss Z",
            "d MMM yyyy HH:mm:ss Z",
            "EEE, d M yyyy HH:mm:ss Z",
            "EEE, d MMM yyyy HH:mm:ss Z"
    };

    public static final String[] germanMonth = {
      "Jan", "Feb", "Mär", "Apr", "Mai", "Jun", "Jul", "Aug", "Sep", "Okt", "Nov", "Dez"
    };


    /**
     * Das DEFAULT_SORTORDER sollte nach Datum absteigend sortiert sein.
     */
    public static final String DEFAULT_SORTORDER = Feeds.COLUMN_Date +" DESC";

    public static final String DEFAULT_SELECTION =
            Feeds.COLUMN_Deleted +"=? AND " + Feeds.COLUMN_Source + "=?";
    public static final String[] DEFAULT_SELECTION_ARGS =
        {Integer.toString(Flag.VISIBLE), Integer.toString(0)};

    public static final String DEFAULT_SELECTION_ADD = Feeds.COLUMN_Deleted +"=?";
    public static final String[] DEFAULT_SELECTION_ARGS_ADD = {Integer.toString(Flag.VISIBLE)};

    // Useful SQL queries

    public static final String SQL_CREATE_ENTRIES =
            "CREATE TABLE " + Feeds.TABLE_NAME + " (" +
                    Feeds._ID + INTEGER_TYPE + " PRIMARY KEY" + COMMA_SEP +
                    Feeds.COLUMN_Title + TEXT_TYPE + COMMA_SEP +
                    Feeds.COLUMN_Date + DATE_TYPE + COMMA_SEP +
                    Feeds.COLUMN_Link + TEXT_TYPE + COMMA_SEP +
                    Feeds.COLUMN_Body + TEXT_TYPE + COMMA_SEP +
                    Feeds.COLUMN_Image + IMAGE_TYPE + COMMA_SEP +
                    Feeds.COLUMN_Source + INTEGER_TYPE + COMMA_SEP +
                    Feeds.COLUMN_Souname + TEXT_TYPE + COMMA_SEP +
                    Feeds.COLUMN_Deleted + INTEGER_TYPE + COMMA_SEP +
                    Feeds.COLUMN_Flag + INTEGER_TYPE + " )";

    public static final String SQL_DELETE_ENTRIES =
            "DROP TABLE IF EXISTS " + Feeds.TABLE_NAME;

    public static final String[] projection = {
            Feeds._ID,
            Feeds.COLUMN_Title,
            Feeds.COLUMN_Date,
            Feeds.COLUMN_Link,
            Feeds.COLUMN_Body,
            Feeds.COLUMN_Image,
            Feeds.COLUMN_Source,
            Feeds.COLUMN_Souname,
            Feeds.COLUMN_Deleted,
            Feeds.COLUMN_Flag
    };

    public static final String SELECTION_SEARCH =
            Feeds.COLUMN_Deleted +"=? AND (" +
                    Feeds.COLUMN_Title + " LIKE ? OR " +
                    Feeds.COLUMN_Souname + " LIKE ? OR " +
                    Feeds.COLUMN_Body + " LIKE ?)";

    public static String[] searchArgs(String query) {
        return new String[]{Integer.toString(Flag.VISIBLE), "%"+query+"%", "%"+query+"%", "%"+query+"%"};
    }

    /**
     * generates a DB friendly date string.
     *
     * @param date the date
     * @return the string
     */
    public static String dbFriendlyDate(Date date) {
        SimpleDateFormat formatOut = new SimpleDateFormat(DATABASE_DATETIME_FORMAT, Locale.ENGLISH);
        return formatOut.format(date);
    }

    /**
     * Wrapper für Html.fromHtml(), was sich von unterschiedlichen Android Versionen unterscheidet.
     *
     * @param str html Code
     * @return spanned
     */
    static public Spanned fromHtml(String str) {
        Spanned sp;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            sp = Html.fromHtml(str, Html.FROM_HTML_MODE_COMPACT, null, null);
        } else {
            sp = Html.fromHtml(str, null, null);
        }
        return sp;
    }

    /**
     * Ein Wrapper für gleichnamige andere Methode, um nicht immer <b>true</b> eingeben zu müssen.
     *
     * @param html String with html code
     * @return result without html code
     */
    public static String removeHtml(String html) {
        FeedContract fc = new FeedContract();
        return fc.removeHtml(html, true);
    }

    /**
     * Entfernt html code in einem String.
     * wenn ignoreEntities <b>true</b>, dann wird zusätzlich versucht, HTML-Entities auf zu
     * lösen und zu reinem Text zu konvertieren.
     *
     * @param html           string mit html code
     * @param ignoreEntities if true: method uses <tt>Html.fromHtml()</tt> to remove HTML-Entities
     * @return result without html code
     */
    public String removeHtml(String html, boolean ignoreEntities) {
        html = html.replaceAll("<(.*?)\\>"," ");
        html = html.replaceAll("<(.*?)\\\n"," ");
        html = html.replaceFirst("(.*?)\\>", " ");
        html = html.replaceAll("&nbsp;"," ");

        if (ignoreEntities) {
            // handle some html entities
            html = fromHtml(html).toString();
        }
        return html.trim();
    }

    /**
     * Macht aus dem Datums-String eines Feeds ein <b>Date</b>-Objekt.
     *
     * @param feedRaw String mit dem Datum, so wie er im <b>pubDate</b> Tag gefunden wurde.
     * @return Datum passend zum String oder (bei Fehler) das jetzige Datum
     */
    public static Date rawToDate(String feedRaw) {
        Date result;
        if (feedRaw == null) {
            Log.d(RssOTweet.TAG, "Feed Date is null! use date from now.");
            return new Date();
        }
        // terrible !?!?!!!!
        for (int i=0; i < germanMonth.length; i++) {
            feedRaw = feedRaw.replace(germanMonth[i], Integer.toString(i+1));
        }

        for (int i=0; i < FEEDRAW_DATETIME_FORMAT.length; i++) {
            try {
                SimpleDateFormat formatIn = new SimpleDateFormat(FEEDRAW_DATETIME_FORMAT[i], Locale.ENGLISH);
                formatIn.setLenient(true);
                result = formatIn.parse(feedRaw);
                return result;
            } catch (ParseException e) {
                if (BuildConfig.DEBUG) {
                    Log.d(RssOTweet.TAG, feedRaw + ": EN parse error: " + FEEDRAW_DATETIME_FORMAT[i]);
                    Log.d(RssOTweet.TAG, "Error Position: " + Integer.toString(e.getErrorOffset()));
                }
            }

            try {
                SimpleDateFormat formatIn = new SimpleDateFormat(FEEDRAW_DATETIME_FORMAT[i], Locale.GERMAN);
                formatIn.setLenient(true);
                result = formatIn.parse(feedRaw);
                return result;
            } catch (ParseException e) {
                if (BuildConfig.DEBUG) {
                    Log.d(RssOTweet.TAG, feedRaw + ": DE parse error: " + FEEDRAW_DATETIME_FORMAT[i]);
                    Log.d(RssOTweet.TAG, "Error Position: " + Integer.toString(e.getErrorOffset()));
                }
            }

        }

        return new Date();
    }

    /**
     * Reformatiert das Datum eines Feeds aus der Datenbank, so
     * dass es in einem View genutzt werden kann.
     *
     * @param dbDate Ein Datumsstring, so wie ihn die Datenbank liefert
     * @return Ein String, so wie man ihn in einen View als Datum nutzen kann
     */
    public static String getDate(String dbDate) {
        Date date = null;
        SimpleDateFormat formatIn = new SimpleDateFormat(
                DATABASE_DATETIME_FORMAT, Locale.ENGLISH
        );
        try {
            date = formatIn.parse(dbDate);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return getDate(date);
    }

    /**
     * Aus einem Datum wird ein String zur Darstellung im View erzeugt.
     *
     * @param date Datum
     * @return Das Datum in sprachabhängigem Format für den Feed
     */
    public static String getDate(Date date) {

        boolean moreThanDay = (new Date().getTime() - date.getTime()) > MILLIS_PER_DAY;
        SimpleDateFormat formatOut;

        if (moreThanDay) {
            formatOut = new SimpleDateFormat(
                    RssOTweet.getContextOfApplication().getString(R.string.dateForm2), Locale.ENGLISH
            );
        } else {
            formatOut = new SimpleDateFormat(
                    RssOTweet.getContextOfApplication().getString(R.string.dateForm), Locale.ENGLISH
            );

        }
        return formatOut.format(date);
    }

    /**
     * Extrahiert aus einem Node bestimmte Daten, die zu einem TAG gehören.
     *
     * @param n   das Item des Feed-Documents, aus dem Daten entnommen werden sollen
     * @param tag der Tag
     * @return gewünschter Inhalt des Tags
     */
    static public String extract(Node n, String tag) {
        Element e = null;
        Element ee = null;
        NodeList nl = null;
        e = (Element) n;
        nl = e.getElementsByTagName(tag);
        if (nl == null) return null;
        ee = (Element) nl.item(0);
        if (ee == null) return null;

        nl = ee.getChildNodes();
        if (nl == null) return null;
        Node n2 = (Node) nl.item(0);
        if (n2 == null) return null;

        return n2.getNodeValue();
    }

    static public String extract(Node n, String tag, String atr) {
        Element e = null;
        Element ee = null;
        NodeList nl = null;
        e = (Element) n;
        nl = e.getElementsByTagName(tag);
        if (nl == null) return null;
        ee = (Element) nl.item(0);
        if (ee == null) return null;

        return ee.getAttributes().getNamedItem(atr).getNodeValue();
    }

    /**
     * Erzeugt aus einem Bitmap ein Byte Array, so dass es in die Datenbank gespeichert werden kann.
     *
     * @param bitmap Das Bild
     * @return Bild als Byte Array, um es in der DB als BLOB zu speichern
     */
    public static byte[] getBytes(Bitmap bitmap) {
        if (bitmap == null) return null;
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 0, stream);
        return stream.toByteArray();
    }

    /**
     * Macht aus dem Daten in der DB ein Bild
     *
     * @param image Bild als Datenbank-Byte Array (BLOB)
     * @return Bild als Bitmap
     */
    public static Bitmap getImage(byte[] image) {
        if (image == null) return null;
        return BitmapFactory.decodeByteArray(image, 0, image.length);
    }

    /**
     * Skaliert ein Bitmap auf gewünschte Breite. Das Seitenverhältnis bleibt erhalten.
     *
     * @param b     input
     * @param width gewünschte Breite
     * @return output
     */
    public static Bitmap scale(Bitmap b, int width, float round) {
        float ration = (float) b.getHeight() / b.getWidth();
        int newHeight = (int) (ration * width);
        byte[] imageAsBytes = FeedContract.getBytes(b);
        Bitmap bo = BitmapFactory.decodeByteArray(imageAsBytes, 0, imageAsBytes.length);
        b = Bitmap.createScaledBitmap(bo, width, newHeight, false);

        Bitmap output = Bitmap.createBitmap(
                b.getWidth(),
                b.getHeight(),
                Bitmap.Config.ARGB_8888
        );
        Canvas canvas = new Canvas(output);

        BitmapShader shader;
        RectF rect;
        shader = new BitmapShader(b, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setShader(shader);
        rect = new RectF(0.0f, 0.0f, b.getWidth(), b.getHeight());
        canvas.drawRoundRect(rect, round, round, paint);

        return output;
    }


    public static Bitmap drawableToBitmap(Drawable drawable) {
        Bitmap bitmap = null;

        if (drawable instanceof BitmapDrawable) {
            BitmapDrawable bitmapDrawable = (BitmapDrawable) drawable;
            if(bitmapDrawable.getBitmap() != null) {
                return bitmapDrawable.getBitmap();
            }
        }

        if(drawable.getIntrinsicWidth() <= 0 || drawable.getIntrinsicHeight() <= 0) {
            bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888); // Single color bitmap will be created of 1x1 pixel
        } else {
            bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        }

        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        return bitmap;
    }

    public static Bitmap getImage(Node n) {
        Bitmap result = null;
        InputStream is = null;
        String path = null;

        String e = extract(n, "enclosure", "url");
        String t = extract(n, "media:thumbnail", "url");
        String u = extract(n, "media:content", "url");
        String c = extract(n, "content:encoded");
        String b = extract(n, "description");
        String q = extract(n, "img", "src");
        boolean well = false;

        if (well == false && q != null) {
            Log.d(RssOTweet.TAG, "img   " + path);
            well = true;
            path = q;
        }
        if (well == false && t != null) {
            path = t;
            Log.d(RssOTweet.TAG, "media:thumbnail " + path);
            well = true;
        }
        if (well == false && u != null) {
            path = u;
            Log.d(RssOTweet.TAG, "media:content " + path);
            well = true;
        }
        if (well == false && e != null) {
            path = e;
            Log.d(RssOTweet.TAG, "enclosure " + path);
            well = true;
        }
        if (b != null) b = b.replace("&lt;", "<").replace("&gt;", ">").replace("&quot;","\"");
        if (well == false && b != null && b.contains("<img ")) {

            int start = b.indexOf(" src=\"");
            b = b.replace(".jpg\"",".jpg\" ");
            b = b.replace(".png\"",".png\" ");
            b = b.replace(".gif\"",".gif\" ");
            b = b.replace(".jpeg\"",".jpeg\" ");

            int stopp = b.indexOf(".jpg\" ", start);
            int stoppPl = 4;
            if (stopp < 0) {
                stopp = b.indexOf(".JPG\" ", start);
            }
            if (stopp < 0) {
                stopp = b.indexOf(".png\" ", start);
            }
            if (stopp < 0) {
                stopp = b.indexOf(".gif\" ", start);
            }
            if (stopp < 0) {
                stopp = b.indexOf(".jpeg\" ", start);
                stoppPl = 5;
            }
            if (stopp < 0) {
                stopp = b.indexOf("\" alt=", start);
                stoppPl = 0;
            }
            if (start > 0 && stopp > 0) {
                b = b.substring(start + 6, stopp + stoppPl);
                Log.d(RssOTweet.TAG, "description  " + b);
                well = true;
                path = b;
            }
        }

        if (c != null) c = c.replace("&lt;", "<").replace("&gt;", ">").replace("&quot;","\"");
        if (well == false && c != null && c.contains("<img ")) {
            int start = c.indexOf(" src=\"");
            c = c.replace(".jpg\"",".jpg\" ");
            c = c.replace(".png\"",".png\" ");
            c = c.replace(".gif\"",".gif\" ");
            c = c.replace(".jpeg\"",".jpeg\" ");
            int stopp = c.indexOf(".jpg\" ", start);

            int stoppPl = 4;
            if (stopp < 0) {
                stopp = c.indexOf(".JPG\" ", start);
            }
            if (stopp < 0) {
                stopp = c.indexOf(".png\" ", start);
            }
            if (stopp < 0) {
                stopp = c.indexOf(".gif\" ", start);
            }
            if (stopp < 0) {
                stopp = c.indexOf(".jpeg\" ", start);
                stoppPl = 5;
            }
            if (stopp < 0) {
                stopp = c.indexOf("\" alt=", start);
                stoppPl = 0;
            }
            if (start > 0 && stopp > 0) {
                c = c.substring(start + 6, stopp + stoppPl);
                Log.d(RssOTweet.TAG, "content:encoded  " + c);
                well = true;
                path = c;
            }
        }

        if (well) result = getImageFromUrl(path, RssOTweet.Config.IMG_ROUND);

        return result;
    }

    public static Bitmap getImageFromUrl(String path, float round) {
        Bitmap result = null;
        InputStream is = null;
        Log.d(RssOTweet.TAG, "get Image from " + path);

        try {
            is = new URL(path).openStream();
            result = BitmapFactory.decodeStream(is);
            is.close();
            if (result.getWidth() < 16 || result.getHeight() < 16) return null;
            SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(RssOTweet.getContextOfApplication());
            int width = pref.getInt("image_width", RssOTweet.Config.DEFAULT_MAX_IMG_WIDTH);
            result = FeedContract.scale(result, width, round);
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        return result;
    }
}
