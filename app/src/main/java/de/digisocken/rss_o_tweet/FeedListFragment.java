package de.digisocken.rss_o_tweet;

import android.app.ListFragment;
import android.app.LoaderManager;
import android.content.ContentValues;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.widget.LinearLayoutCompat;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import java.io.IOException;

/**
 * Dieser "ListView" nutzt einen {@link FeedCursorAdapter} zur Darstellung der Feeds und
 * bezieht die Feeds über den CursorLoader, der {@link FeedContentProvider} nutzt.
 */
public class FeedListFragment extends ListFragment implements LoaderManager.LoaderCallbacks<Cursor> {
    private FeedCursorAdapter adapter;

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);

        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo)menuInfo;
        Cursor c = (Cursor) adapter.getItem(info.position);

        // first item!!
        int flagVal = c.getInt(c.getColumnIndex(FeedContract.Feeds.COLUMN_Flag));
        if (flagVal == FeedContract.Flag.FAVORITE) {
            menu.add(R.string.nofavorite);
        } else {
            menu.add(R.string.favorite);
        }
        MenuInflater inflater = getActivity().getMenuInflater();
        inflater.inflate(R.menu.item_context, menu);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        new ContextTask().execute(item);
        return super.onContextItemSelected(item);
    }

    /**
     * In dieser Methode wird ein Trick genutzt, um einen selbst definierten <b>emptyView</b> für
     * die ListView zu nutzen. Andere Stellen zur Festlegung dieses Views sind nicht möglich.
     *
     * @param savedInstanceState
     */
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        registerForContextMenu(getListView());
        View  emptyView = getActivity().getLayoutInflater().inflate(R.layout.empty_view, null);
        ((ViewGroup)getListView().getParent()).addView(emptyView);
        getListView().setEmptyView(emptyView);
        getListView().setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                Cursor c = (Cursor) adapter.getItem(position);

                long id = c.getInt(c.getColumnIndex(FeedContract.Feeds._ID));
                Uri uri = Uri.parse(FeedContentProvider.CONTENT_URI + "/" + id);
                int flagVal = c.getInt(c.getColumnIndex(FeedContract.Feeds.COLUMN_Flag));
                String link = c.getString(c.getColumnIndex(FeedContract.Feeds.COLUMN_Link));
                ContentValues values = new ContentValues();

                SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(RssOTweet.getContextOfApplication());
                String tap_mode = pref.getString("tap_mode", "intern");

                if (tap_mode.equals("intern") || tap_mode.equals("extern")) {

                    if (flagVal == FeedContract.Flag.NEW || flagVal == FeedContract.Flag.FAVORITE) {
                        // if unreaded or marked:
                        // -> mark readed +
                        // -> open in browser
                        values.put(FeedContract.Feeds.COLUMN_Flag, FeedContract.Flag.READED);
                        getActivity().getContentResolver().update(uri, values, null, null);

                        if (tap_mode.equals("intern")) {
                            ((MainActivity) getActivity()).setWebView(link);
                        } else {
                            Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(link));
                            startActivity(i);
                        }

                    } else if (flagVal == FeedContract.Flag.READED) {
                        // if readed
                        // -> mark
                        values.put(FeedContract.Feeds.COLUMN_Flag, FeedContract.Flag.FAVORITE);
                        getActivity().getContentResolver().update(uri, values, null, null);
                    }

                } else if (tap_mode.equals("readed")) {

                    if (flagVal == FeedContract.Flag.NEW) {
                        values.put(FeedContract.Feeds.COLUMN_Flag, FeedContract.Flag.READED);
                    } else {
                        values.put(FeedContract.Feeds.COLUMN_Flag, FeedContract.Flag.NEW);
                    }
                    getActivity().getContentResolver().update(uri, values, null, null);

                } else if (tap_mode.equals("marked")) {

                    if (flagVal == FeedContract.Flag.FAVORITE) {
                        values.put(FeedContract.Feeds.COLUMN_Flag, FeedContract.Flag.NEW);
                    } else {
                        values.put(FeedContract.Feeds.COLUMN_Flag, FeedContract.Flag.FAVORITE);
                    }
                    getActivity().getContentResolver().update(uri, values, null, null);
                }
            }
        });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View layout = inflater.inflate(R.layout.fragment_feedlist, container, false);
        int nightModeFlags =
                getResources().getConfiguration().uiMode &
                        Configuration.UI_MODE_NIGHT_MASK;
        getLoaderManager().initLoader(0, null, this);
        adapter = new FeedCursorAdapter(getActivity(), null, 0);
        setListAdapter(adapter);
        return layout;
    }

    /**
     * Holt die Feeds für die LiestView aus der DB.
     * Hier Wird DEFAULT_SELECTION von {@link FeedContract} genutzt sowie
     * weitere Konstanten, damit gelöschte Feeds nicht gezeigt und die
     * Feeds nach Datum sortiert sind.
     *
     * @param LoaderId
     * @param bundle
     * @return
     */
    @Override
    public Loader<Cursor> onCreateLoader(int LoaderId, Bundle bundle) {
        if (!RssOTweet.query.equals("")) {
            return new CursorLoader(
                    getActivity(),
                    FeedContentProvider.CONTENT_URI,
                    FeedContract.projection,
                    FeedContract.SELECTION_SEARCH,
                    FeedContract.searchArgs(RssOTweet.query),
                    FeedContract.DEFAULT_SORTORDER
            );
        }
        if (RssOTweet.showAdditionalFeed) {
            return new CursorLoader(
                    getActivity(),
                    FeedContentProvider.CONTENT_URI,
                    FeedContract.projection,
                    FeedContract.DEFAULT_SELECTION_ADD,
                    FeedContract.DEFAULT_SELECTION_ARGS_ADD,
                    FeedContract.DEFAULT_SORTORDER
            );
        } else {
            return new CursorLoader(
                    getActivity(),
                    FeedContentProvider.CONTENT_URI,
                    FeedContract.projection,
                    FeedContract.DEFAULT_SELECTION,
                    FeedContract.DEFAULT_SELECTION_ARGS,
                    FeedContract.DEFAULT_SORTORDER
            );
        }
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        adapter.swapCursor(data);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        adapter.swapCursor(null);
    }


    private class ContextTask extends AsyncTask<MenuItem, Void, String> {

        @Override
        protected String doInBackground(MenuItem... params) {
            MenuItem item = params[0];
            AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
            long id = info.id;
            Cursor c = (Cursor) adapter.getItem(info.position);
            Uri uri = Uri.parse(FeedContentProvider.CONTENT_URI + "/" + id);
            int flagVal = c.getInt(c.getColumnIndex(FeedContract.Feeds.COLUMN_Flag));
            ContentValues values = new ContentValues();
            String link, title, body;

            switch (item.getItemId()) {
                case R.id.action_openFeed:
                    link = c.getString(c.getColumnIndex(FeedContract.Feeds.COLUMN_Link));
                    /**
                     * play mp3 urls improvement
                     */
                    if (link.endsWith(".mp3")) {
                        if (RssOTweet.mediaPlayer.isPlaying()) {
                            RssOTweet.mediaPlayer.pause();
                            RssOTweet.mediaPlayer.reset();
                            RssOTweet.mediaPlayer.stop();
                        } else {
                            try {
                                RssOTweet.mediaPlayer.setDataSource(link);
                                RssOTweet.mediaPlayer.prepare();
                                RssOTweet.mediaPlayer.start();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    } else {
                        Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(link));
                        startActivity(i);
                    }
                    return null;

                case R.id.action_readedFeed:

                    if (flagVal == FeedContract.Flag.NEW) {
                        values.put(FeedContract.Feeds.COLUMN_Flag, FeedContract.Flag.READED);
                    } else {
                        values.put(FeedContract.Feeds.COLUMN_Flag, FeedContract.Flag.NEW);
                    }
                    getActivity().getContentResolver().update(uri, values, null, null);
                    return null;

                case R.id.action_deleteFeed:
                    title = c.getString(c.getColumnIndex(FeedContract.Feeds.COLUMN_Title));
                    values.put(FeedContract.Feeds.COLUMN_Deleted, FeedContract.Flag.DELETED);
                    getActivity().getContentResolver().update(uri, values, null, null);
                    return title + "\n" + getString(R.string.deleted);

                case R.id.action_share:
                    title = c.getString(c.getColumnIndex(FeedContract.Feeds.COLUMN_Title));
                    body = c.getString(c.getColumnIndex(FeedContract.Feeds.COLUMN_Body));

                    title = FeedContract.removeHtml(title);
                    body = FeedContract.removeHtml(body);
                    body = title.toUpperCase() +
                            "\n" + body + "\n" +
                            c.getString(c.getColumnIndex(FeedContract.Feeds.COLUMN_Link));

                    Intent sharingIntent = new Intent(android.content.Intent.ACTION_SEND);
                    sharingIntent.setType("text/plain");
                    sharingIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, title);
                    sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, body);
                    startActivity(Intent.createChooser(sharingIntent, getString(R.string.share)));
                    return null;

                case 0: // first item!!
                    if (flagVal == FeedContract.Flag.FAVORITE) {
                        values.put(FeedContract.Feeds.COLUMN_Flag, FeedContract.Flag.NEW);
                    } else {
                        values.put(FeedContract.Feeds.COLUMN_Flag, FeedContract.Flag.FAVORITE);
                    }
                    getActivity().getContentResolver().update(uri, values, null, null);
                    return null;

                default:
                    return null;
            }
        }

        @Override
        protected void onPostExecute(String msg) {
            if (msg != null) {
                Toast.makeText(getActivity(), msg, Toast.LENGTH_SHORT).show();
            }
        }
    }
}
