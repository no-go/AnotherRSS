package de.digisocken.anotherrss;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.v4.content.ContextCompat;
import android.text.Spanned;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * Der FeedCursorAdapter verknüpft den Daten(Bank)Cursor mit den Feldern eines Views.
 */
public class FeedCursorAdapter extends CursorAdapter {
    private Bitmap largeIcon;
    private Drawable favoriteIcon;
    private SharedPreferences _pref;

    public FeedCursorAdapter(Context context, Cursor c, int flags) {
        super(context, c, flags);
        largeIcon = BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_launcher);
        favoriteIcon = ContextCompat.getDrawable(context, R.drawable.favorite);
        _pref = PreferenceManager.getDefaultSharedPreferences(AnotherRSS.getContextOfApplication());
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
    public void bindView(View view, Context context, Cursor cursor) {
        float fontSize = _pref.getFloat("font_size", AnotherRSS.Config.DEFAULT_FONT_SIZE);
        TextView tt = (TextView) view.findViewById(R.id.feedTitle);
        String title = cursor.getString(cursor.getColumnIndexOrThrow(FeedContract.Feeds.COLUMN_Title));
        if (!AnotherRSS.query.equals("")) {
            tt.setText(highlight(AnotherRSS.query, title));
        } else {
            tt.setText(title);
        }
        tt.setTextSize(TypedValue.COMPLEX_UNIT_DIP, fontSize * 1.25f);

        TextView td = (TextView) view.findViewById(R.id.feedDate);
        td.setTextSize(TypedValue.COMPLEX_UNIT_DIP, fontSize * 0.75f);
        td.setText(FeedContract.getDate(cursor.getString(
                cursor.getColumnIndexOrThrow(FeedContract.Feeds.COLUMN_Date)
        )));

        TextView tb = (TextView) view.findViewById(R.id.feedBody);
        tb.setTextSize(TypedValue.COMPLEX_UNIT_DIP, fontSize);
        String body = FeedContract.removeHtml(cursor.getString(cursor.getColumnIndexOrThrow(FeedContract.Feeds.COLUMN_Body)));
        if (!AnotherRSS.query.equals("")) {
            tb.setText(highlight(AnotherRSS.query, body));
        } else {
            tb.setText(body);
        }

        TextView sn = (TextView) view.findViewById(R.id.sourceName);
        sn.setTextSize(TypedValue.COMPLEX_UNIT_DIP, fontSize * 0.75f);
        String sName = FeedContract.removeHtml(
                cursor.getString(cursor.getColumnIndexOrThrow(FeedContract.Feeds.COLUMN_Souname))
        );
        if (!AnotherRSS.query.equals("")) {
            sn.setText(highlight(AnotherRSS.query, sName));
        } else {
            sn.setText(sName);
        }

        tt.setPadding(10, 20,  5, 0);
        tb.setPadding(10,  0, 10, 0);
        sn.setPadding(10,  0, 10, 0);

        Bitmap bmp = FeedContract.getImage(
                cursor.getBlob(cursor.getColumnIndexOrThrow(FeedContract.Feeds.COLUMN_Image))
        );
        ImageView iv = (ImageView) view.findViewById(R.id.image);
        iv.setImageBitmap(bmp);
        int source = cursor.getInt(cursor.getColumnIndexOrThrow(FeedContract.Feeds.COLUMN_Source));
        if (bmp != null) {
            int width = _pref.getInt("image_width", AnotherRSS.Config.DEFAULT_MAX_IMG_WIDTH);
            iv.setPadding(20, 30, 10, 0);
            iv.setMaxWidth(width);
            if (bmp.getWidth() != width) {
                bmp = FeedContract.scale(bmp, width);
                iv.setImageBitmap(bmp);
            }
        } else {
            iv.setPadding( 0, 0, 0, 0);
            tt.setPadding(20, 10,  5, 0);
            tb.setPadding(20,  0, 10, 0);
            sn.setPadding(20,  0, 10, 0);
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
                "<b><font color='"+ AnotherRSS.Config.SEARCH_HINT_COLOR + "'>$1</font></b>"
        );
        return FeedContract.fromHtml(msg);
    }
}
