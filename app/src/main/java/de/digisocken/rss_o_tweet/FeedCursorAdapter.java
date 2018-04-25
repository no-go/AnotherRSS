package de.digisocken.rss_o_tweet;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.support.v4.content.ContextCompat;
import android.text.Spanned;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.MediaController;
import android.widget.TextView;
import android.widget.VideoView;

import java.io.IOException;

/**
 * Der FeedCursorAdapter verknüpft den Daten(Bank)Cursor mit den Feldern eines Views.
 */
public class FeedCursorAdapter extends CursorAdapter {
    private Bitmap largeIcon;
    private Drawable favoriteIcon;
    private SharedPreferences _pref;
    private Typeface myFont;
    private boolean isNight;

    public FeedCursorAdapter(Context context, Cursor c, int flags) {
        super(context, c, flags);
        int nightModeFlags =
                context.getResources().getConfiguration().uiMode &
                        Configuration.UI_MODE_NIGHT_MASK;
        switch (nightModeFlags) {
            case Configuration.UI_MODE_NIGHT_YES:
                myFont = Typeface.createFromAsset(context.getAssets(), "fonts/C64_Pro_Mono-STYLE.ttf");
                isNight=true;
                break;
            case Configuration.UI_MODE_NIGHT_NO:
            case Configuration.UI_MODE_NIGHT_UNDEFINED:
            default:
                myFont = Typeface.SERIF;
                isNight = false;
        }

        largeIcon = BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_launcher);
        favoriteIcon = ContextCompat.getDrawable(context, R.drawable.favorite);
        _pref = PreferenceManager.getDefaultSharedPreferences(RssOTweet.getContextOfApplication());
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        return LayoutInflater.from(context).inflate(R.layout.feed, parent, false);
    }

    /**
     * Diese Methode ist so ähnlich wie <tt>CursorAdapter#getView(int, View, ViewGroup)</tt>, recycled
     * jedoch bereits zuvor genutzte Views.
     * <p>
     * Wegen Recycling sind entsprechende Else-Zweige sind nötig, da sonst Padding/Color aus einem
     * alten View genutzt/recycled wird.
     * </p><p>
     * Sollte in der Datenbank ein Bild existieren, wird ein Abstand zum Body-Text eingebaut.
     * </p>
     * @param view
     * @param context
     * @param cursor
     */
    @Override
    public void bindView(View view, final Context context, Cursor cursor) {
        float fontSize = _pref.getFloat("font_size", RssOTweet.Config.DEFAULT_FONT_SIZE);
        TextView tt = (TextView) view.findViewById(R.id.feedTitle);
        String title = cursor.getString(cursor.getColumnIndexOrThrow(FeedContract.Feeds.COLUMN_Title));
        boolean isTweet = false;
        RssOTweet.mediaController = new MediaController(context);

        isTweet = title.startsWith("(");
        title = title.replaceAll("(\\(\\d+\\))", "");
        if (isNight) {
            // the c64 font has not this chars
            title = title.replace("Ä", "Ae");
            title = title.replace("Ü", "Ue");
            title = title.replace("Ö", "Oe");
            title = title.replace("ä", "ae");
            title = title.replace("ü", "ue");
            title = title.replace("ö", "oe");
            title = title.replace("ß", "ss");
        }
        if (!RssOTweet.query.equals("")) {
            tt.setText(highlight(RssOTweet.query, title.trim()));
        } else {
            tt.setText(title.trim());
        }
        tt.setTextSize(TypedValue.COMPLEX_UNIT_DIP, fontSize * 1.25f);
        tt.setTypeface(myFont);

        TextView td = (TextView) view.findViewById(R.id.feedDate);
        td.setTextSize(TypedValue.COMPLEX_UNIT_DIP, fontSize * 0.75f);
        td.setText(FeedContract.getDate(cursor.getString(
                cursor.getColumnIndexOrThrow(FeedContract.Feeds.COLUMN_Date)
        )));

        TextView tb = (TextView) view.findViewById(R.id.feedBody);
        tb.setTextSize(TypedValue.COMPLEX_UNIT_DIP, fontSize);
        String body = FeedContract.removeHtml(cursor.getString(cursor.getColumnIndexOrThrow(FeedContract.Feeds.COLUMN_Body)));

        if (isNight) {
            // the c64 font has not this chars
            body = body.replace("Ä", "Ae");
            body = body.replace("Ü", "Ue");
            body = body.replace("Ö", "Oe");
            body = body.replace("ä", "ae");
            body = body.replace("ü", "ue");
            body = body.replace("ö", "oe");
            body = body.replace("ß", "ss");
        }
        if (!RssOTweet.query.equals("")) {
            tb.setText(highlight(RssOTweet.query, body));
        } else {
            tb.setText(body);
        }
        tb.setTypeface(myFont);

        TextView sn = (TextView) view.findViewById(R.id.sourceName);
        sn.setTextSize(TypedValue.COMPLEX_UNIT_DIP, fontSize * 0.75f);
        String sName = FeedContract.removeHtml(
                cursor.getString(cursor.getColumnIndexOrThrow(FeedContract.Feeds.COLUMN_Souname))
        );
        if (!RssOTweet.query.equals("")) {
            sn.setText(highlight(RssOTweet.query, sName));
        } else {
            sn.setText(sName);
        }

        tt.setPadding(10, 3,  5, 10);
        tb.setPadding(10, 0, 10, 0);
        sn.setPadding(10, 0, 10, 5);

        Bitmap bmp = FeedContract.getImage(
                cursor.getBlob(cursor.getColumnIndexOrThrow(FeedContract.Feeds.COLUMN_Image))
        );
        ImageView iv = (ImageView) view.findViewById(R.id.image);
        iv.setImageBitmap(bmp);
        int source = cursor.getInt(cursor.getColumnIndexOrThrow(FeedContract.Feeds.COLUMN_Source));
        if (bmp != null) {
            if (!isTweet) {
                iv.setBackgroundColor(Color.TRANSPARENT);
            }
            int width = _pref.getInt("image_width", RssOTweet.Config.DEFAULT_MAX_IMG_WIDTH);
            iv.setPadding(10, 10, 10, 10);
            iv.setMaxWidth(width);
            if (bmp.getWidth() != width) {
                if (isTweet) {
                    bmp = FeedContract.scale(bmp, width, RssOTweet.Config.TWEET_IMG_ROUND);
                } else {
                    bmp = FeedContract.scale(bmp, width, RssOTweet.Config.IMG_ROUND);
                }
                iv.setImageBitmap(bmp);
            }
        } else {
            final String link = cursor.getString(cursor.getColumnIndexOrThrow(FeedContract.Feeds.COLUMN_Link));
            if (link.endsWith(".mp3") || link.endsWith(".mp4") || link.endsWith(".ogg")) {
                if (RssOTweet.mediaPlayer.isPlaying()) {
                    bmp = BitmapFactory.decodeResource(
                            RssOTweet.getContextOfApplication().getResources(),
                            android.R.drawable.ic_media_pause
                    );
                } else {
                    bmp = BitmapFactory.decodeResource(
                            RssOTweet.getContextOfApplication().getResources(),
                            android.R.drawable.ic_media_play
                    );
                }
                iv.setBackgroundColor(Color.TRANSPARENT);
                int width = _pref.getInt("image_width", RssOTweet.Config.DEFAULT_MAX_IMG_WIDTH);
                iv.setPadding(10, 10, 10, 10);
                iv.setMaxWidth(width);
                iv.setImageBitmap(bmp);
                iv.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View viewimg) {

                        if (link.endsWith(".mp4")) {
                            Log.d(RssOTweet.TAG, "on click do " + link);
                            ((MainActivity) context).setMediaView(link);

                        } else {
                            if (RssOTweet.mediaPlayer.isPlaying()) {
                                RssOTweet.mediaPlayer.pause();
                                RssOTweet.mediaPlayer.reset();
                                RssOTweet.mediaPlayer.stop();
                                Bitmap bmp = BitmapFactory.decodeResource(
                                        RssOTweet.getContextOfApplication().getResources(),
                                        android.R.drawable.ic_media_play
                                );
                                ((ImageView) viewimg).setImageBitmap(bmp);
                            } else {
                                try {
                                    RssOTweet.mediaPlayer.setDataSource(link);
                                    RssOTweet.mediaPlayer.prepare();
                                    RssOTweet.mediaPlayer.start();
                                    Bitmap bmp = BitmapFactory.decodeResource(
                                            RssOTweet.getContextOfApplication().getResources(),
                                            android.R.drawable.ic_media_pause
                                    );
                                    ((ImageView) viewimg).setImageBitmap(bmp);
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    }
                });
            } else {
                iv.setPadding(0, 0, 0, 0);
                tt.setPadding(20, 3, 5, 10);
                tb.setPadding(20, 0, 10, 0);
                sn.setPadding(20, 0, 10, 5);
            }
        }
        int hasFlag = cursor.getInt(cursor.getColumnIndexOrThrow(FeedContract.Feeds.COLUMN_Flag));
        if (hasFlag == FeedContract.Flag.READED) {
            int oldTxt = ContextCompat.getColor(context, R.color.colorOldText);
            tt.setTextColor(oldTxt);
            td.setTextColor(oldTxt);
            tb.setTextColor(oldTxt);
            sn.setTextColor(oldTxt);
            iv.setAlpha(0.3f);
            view.setBackgroundColor(ContextCompat.getColor(context, R.color.colorOld));
            td.setBackground(null);
        } else if (hasFlag == FeedContract.Flag.FAVORITE) {
            tt.setTextColor(ContextCompat.getColor(context, R.color.colorTitle));
            td.setTextColor(ContextCompat.getColor(context, R.color.colorDate));
            tb.setTextColor(ContextCompat.getColor(context, R.color.colorBody));
            sn.setTextColor(ContextCompat.getColor(context, R.color.colorDate));
            iv.setAlpha(1.0f);
            view.setBackgroundColor(ContextCompat.getColor(context, R.color.colorBackground));
            td.setBackground(favoriteIcon);
        } else {
            tt.setTextColor(ContextCompat.getColor(context, R.color.colorTitle));
            td.setTextColor(ContextCompat.getColor(context, R.color.colorDate));
            tb.setTextColor(ContextCompat.getColor(context, R.color.colorBody));
            sn.setTextColor(ContextCompat.getColor(context, R.color.colorDate));
            iv.setAlpha(1.0f);
            view.setBackgroundColor(ContextCompat.getColor(context, R.color.colorBackground));
            td.setBackground(null);
        }
    }

    public Spanned highlight(String key, String msg) {
        msg = msg.replaceAll(
                "((?i)"+key+")",
                "<b><font color='"+ RssOTweet.Config.SEARCH_HINT_COLOR + "'>$1</font></b>"
        );
        return FeedContract.fromHtml(msg);
    }
}
