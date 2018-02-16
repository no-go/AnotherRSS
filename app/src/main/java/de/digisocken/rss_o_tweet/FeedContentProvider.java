package de.digisocken.rss_o_tweet;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.support.annotation.Nullable;
import android.text.TextUtils;

/**
 * Konstanten und CRUD Funktionen zum Zugriff auf Feeds in der Datenbank via URI.
 *
 * Ich sehe das mit der Klasse als suboptimal an.
 * Kann ja nicht sein, dass man für jede Tabelle einen eigenen
 * Provider programmieren muss !? oder einen eigenen Helper! Das geht auch eleganter,
 * in dem man z.B. alles in einem Feed Objekt kapselt !!!!!!
 *
 * @see FeedContract
 * @see FeedHelper
 */
public class FeedContentProvider extends ContentProvider {

    public static final String AUTHORITY = "de.digisocken.rss_o_tweet.contentprovider";
    public static final String CONTENT_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE + "/feeds";
    public static final String CONTENT_ITEM_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE + "/feed";

    private FeedHelper _database;

    // used for the UriMacher
    private static final int FEEDS = 10;
    private static final int FEED_ID = 20;

    private static final String BASE_PATH = "feeds";

    public static final Uri CONTENT_URI = Uri.parse(
            "content://" + AUTHORITY
            + "/" + BASE_PATH
    );

    private static final UriMatcher sURIMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    /**
     * mappt URI auf FEED bzw. FEED_ID, je nachdem ob eine Nummer angehangen worden ist
     */
    static {
        sURIMatcher.addURI(AUTHORITY, BASE_PATH, FEEDS);
        sURIMatcher.addURI(AUTHORITY, BASE_PATH + "/#", FEED_ID);
    }

    @Override
    public boolean onCreate() {
        _database = new FeedHelper(getContext());
        return false;
    }

    @Nullable
    @Override
    public Cursor query(
            Uri uri, String[] projection, String selection,
            String[] selectionArgs, String sortOrder
    ) {
        SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();
        queryBuilder.setTables(FeedContract.Feeds.TABLE_NAME);

        int uriType = sURIMatcher.match(uri);
        switch (uriType) {

            case FEEDS:
                break;

            case FEED_ID:
                queryBuilder.appendWhere(
                        FeedContract.Feeds._ID + "=" + uri.getLastPathSegment()
                );
                break;

            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }

        SQLiteDatabase db = _database.getWritableDatabase();
        Cursor cursor = queryBuilder.query(
                db, projection, selection, selectionArgs, null, null, sortOrder
        );
        cursor.setNotificationUri(getContext().getContentResolver(), uri);

        return cursor;
    }

    @Nullable
    @Override
    public String getType(Uri uri) {
        return null;
    }

    @Nullable
    @Override
    public Uri insert(Uri uri, ContentValues contentValues) {
        int uriType = sURIMatcher.match(uri);
        SQLiteDatabase sqlDB = _database.getWritableDatabase();
        long id = 0;
        switch (uriType) {

            case FEEDS:
                id = sqlDB.insert(FeedContract.Feeds.TABLE_NAME, null, contentValues);
                break;

            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return Uri.parse(BASE_PATH + "/" + id);
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        int uriType = sURIMatcher.match(uri);
        SQLiteDatabase sqlDB = _database.getWritableDatabase();
        int rowsDeleted = 0;
        switch (uriType) {

            case FEEDS:
                rowsDeleted = sqlDB.delete(FeedContract.Feeds.TABLE_NAME, selection, selectionArgs);
                break;

            case FEED_ID:
                String id = uri.getLastPathSegment();
                if (TextUtils.isEmpty(selection)) {
                    rowsDeleted = sqlDB.delete(
                            FeedContract.Feeds.TABLE_NAME,
                            FeedContract.Feeds._ID + "=" + id,
                            null
                    );
                } else {
                    rowsDeleted = sqlDB.delete(
                            FeedContract.Feeds.TABLE_NAME,
                            FeedContract.Feeds._ID + "=" + id + " and " + selection,
                            selectionArgs
                    );
                }
                break;

            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return rowsDeleted;
    }

    @Override
    public int update(Uri uri, ContentValues contentValues, String selection, String[] selectionArgs) {
        int uriType = sURIMatcher.match(uri);
        SQLiteDatabase sqlDB = _database.getWritableDatabase();
        int rowsUpdated = 0;
        switch (uriType) {

            case FEEDS:
                rowsUpdated = sqlDB.update(
                        FeedContract.Feeds.TABLE_NAME,
                        contentValues,
                        selection,
                        selectionArgs
                );
                break;

            case FEED_ID:
                String id = uri.getLastPathSegment();
                if (TextUtils.isEmpty(selection)) {
                    rowsUpdated = sqlDB.update(
                            FeedContract.Feeds.TABLE_NAME,
                            contentValues,
                            FeedContract.Feeds._ID + "=" + id,
                            null
                    );
                } else {
                    rowsUpdated = sqlDB.update(
                            FeedContract.Feeds.TABLE_NAME,
                            contentValues,
                            FeedContract.Feeds._ID + "=" + id + " and " + selection,
                            selectionArgs
                    );
                }
                break;

            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return rowsUpdated;
    }
}
