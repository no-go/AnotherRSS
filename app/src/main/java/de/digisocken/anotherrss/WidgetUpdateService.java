package de.digisocken.anotherrss;

import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.text.Html;
import android.util.Log;
import android.widget.RemoteViews;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import java.io.StringReader;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

public class WidgetUpdateService extends Service {
    SharedPreferences mPreferences;
    String[] urls;

    public static String extractTitles(String resp) {
        String back = "";
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        try {
            DocumentBuilder db = dbf.newDocumentBuilder();
            InputSource is = new InputSource(new StringReader(resp));
            Document doc = db.parse(is);
            doc.getDocumentElement().normalize();

            NodeList nodeList = doc.getElementsByTagName("item");
            if (nodeList.getLength() < 1) {
                nodeList = doc.getElementsByTagName("entry");
            }

            Node n;
            for (int i = 0; i < nodeList.getLength(); i++) {
                n = nodeList.item(i);
                back += "<b>"+ Integer.toString(i+1) + ")</b> " + FeedContract.extract(n, "title") + "<br>";
            }

        } catch (Exception e) {
            e.printStackTrace();
            Log.d(AnotherRSS.TAG, "widget parse error");
        }

        return back;
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        mPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        urls = mPreferences.getString("rss_url", AnotherRSS.urls).split(" ");

        RemoteViews views = new RemoteViews(getPackageName(), R.layout.main_widget);

        views.setTextViewText(R.id.wFeedtitles, "...");
        views.setTextViewText(R.id.wTextApp, getString(R.string.app_name) + " - Feed #no1");


        // Push update for this widget to the home screen
        ComponentName thisWidget = new ComponentName(WidgetUpdateService.this, MyWidgetProvider.class);
        AppWidgetManager manager = AppWidgetManager.getInstance(WidgetUpdateService.this);
        manager.updateAppWidget(thisWidget, views);

        RequestQueue queue = Volley.newRequestQueue(this);
        StringRequest stringRequest = new StringRequest(
                Request.Method.GET, urls[0],

                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        RemoteViews views = new RemoteViews(getPackageName(), R.layout.main_widget);

                        Date dNow = new Date();
                        SimpleDateFormat ft = new SimpleDateFormat("HH:mm");
                        String timeStr = ft.format(dNow);

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            views.setTextViewText(R.id.wFeedtitles, Html.fromHtml(extractTitles(response), Html.FROM_HTML_MODE_COMPACT));
                        } else {
                            views.setTextViewText(R.id.wFeedtitles, Html.fromHtml(extractTitles(response)));
                        }
                        views.setTextViewText(R.id.wTime, timeStr);

                        // Push update for this widget to the home screen
                        ComponentName thisWidget = new ComponentName(WidgetUpdateService.this, MyWidgetProvider.class);
                        AppWidgetManager manager = AppWidgetManager.getInstance(WidgetUpdateService.this);
                        manager.updateAppWidget(thisWidget, views);
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        RemoteViews views = new RemoteViews(getPackageName(), R.layout.main_widget);
                        views.setTextViewText(R.id.wFeedtitles, error.getMessage());
                        ComponentName thisWidget = new ComponentName(WidgetUpdateService.this, MyWidgetProvider.class);
                        AppWidgetManager manager = AppWidgetManager.getInstance(WidgetUpdateService.this);
                        manager.updateAppWidget(thisWidget, views);
                    }
                }
        );
        queue.add(stringRequest);

        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}