package com.fitc.wifitrawl;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.ArrayAdapter;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.util.ArrayList;

/**
 * Created by Jon on 23/02/2016.
 */
public class WifiDevicesTrawler {

    private static final String SPAN = "span";
    private static final String WIFI_ADMIN_USERNAME = "admin";
    private static final String WIFI_ADMIN_PASSWORD = "password";

    private static final int COL_IPADDRESS = 1;
    private static final int COL_DEVICENAME = 2;
    private static final int COL_MACADDRESS = 3;
    private static final String TAG = WifiDevicesTrawler.class.getSimpleName();

    WifiTrawlerCallback mCallback;
    private static String URL = "http://www.routerlogin.net/DEV_device.htm";
    private final ArrayList<WifiDevice> mDevices;
    private final Context mContext;

    public WifiDevicesTrawler(Context c, ArrayList<WifiDevice> devices) {
        mDevices = devices;
        mContext = c;
    }

    public WifiDevicesTrawler(Context c){
        mContext = c;
        mDevices = new ArrayList<>();
    }

    /**
     * Runs trawler in Asynctask.
     */
    public void update(WifiTrawlerCallback callback){
        mCallback = callback;
        mDevices.clear();
        Authenticator.setDefault(new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(WIFI_ADMIN_USERNAME, WIFI_ADMIN_PASSWORD.toCharArray());
            }
        });
        new GetDevicesFromRouterTask().execute();

    }

    private class GetDevicesFromRouterTask extends AsyncTask<Void, Void, Document> {
        protected Document doInBackground(Void... urls) {
            Document doc = null;

            try {
                doc = Jsoup.connect(URL).get();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return doc;
        }

        protected void onPostExecute(Document doc) {
            if (doc != null) {
                parseHtmlTable(doc);
            }
            if (mCallback!=null){
                mCallback.onFinish(mDevices);
            }


            Authenticator.setDefault(null);
        }
    }

    private void parseHtmlTable(Document doc) {
        ArrayList<String> connectedMac = new ArrayList<>();
        Elements rows  = doc.select("tr tr"); //select data table is nested in layout table.

        Log.d(TAG,"Number of rows:" + rows.size());
        for (int i = 1; i < rows.size(); i++) { //first row is the col names so skip it.
            Element row = rows.get(i);
            Elements cols = row.select("td");

            WifiDevice device = new WifiDevice();
            device.deviceName = cols.get(COL_DEVICENAME).select(SPAN).get(0).text().trim();
            device.ipAddress = cols.get(COL_IPADDRESS).select(SPAN).get(0).text().trim();
            device.macAddress = cols.get(COL_MACADDRESS).select(SPAN).get(0).text().trim();
            if (!mDevices.contains(device)) mDevices.add(device);
        }
    }
}
