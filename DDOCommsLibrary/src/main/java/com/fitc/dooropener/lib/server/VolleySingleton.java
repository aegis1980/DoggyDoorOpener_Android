package com.fitc.dooropener.lib.server;

import android.content.Context;
import android.graphics.Bitmap;
import android.support.v4.util.LruCache;
import android.util.Log;


import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.HurlStack;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.Volley;

import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;

import java.io.IOException;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.CookieStore;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by Jon on 12/10/2015.
 */
public class VolleySingleton {
    private static final String TAG = VolleySingleton.class.getSimpleName() ;
    private static VolleySingleton mInstance;
    private RequestQueue mRequestQueue;

    private ImageLoader mImageLoader;

    private static Context mCtx;
    private HttpURLConnection mConnection;
    private HurlStack mHurlStack;
    private CookieManager mCookieManager;
    private boolean mFollowRedirects;

    private VolleySingleton(Context context) {
        mCtx = context;
        mRequestQueue = getRequestQueue();

    }

    public static synchronized VolleySingleton getInstance(Context context) {
        if (mInstance == null) {
            mInstance = new VolleySingleton(context);
        }
        return mInstance;
    }

    public RequestQueue getRequestQueue() {
        if (mRequestQueue == null) {
            // COOKIES ARE USED FOR THE GOOGlE ACCOUNT AUTH
           // Optionally, you can just use the default CookieManager
            // http://stackoverflow.com/questions/16680701/using-cookies-with-android-volley-library
            mCookieManager = new CookieManager(null, CookiePolicy.ACCEPT_ALL);
            CookieHandler.setDefault(mCookieManager);

            // to get handle on connection for redirects
            mHurlStack = new HurlStack() {
                @Override
                protected HttpURLConnection createConnection(URL url) throws IOException {

                    Log.i(TAG,"Creating HurlStack connection.");
                    mConnection = super.createConnection(url);
                    mConnection.setInstanceFollowRedirects(mFollowRedirects);
                    return mConnection;
                }
            };


            // getApplicationContext() is key, it keeps you from leaking the
            // Activity or BroadcastReceiver if someone passes one in.
            mRequestQueue = Volley.newRequestQueue(mCtx.getApplicationContext(),mHurlStack);



            mImageLoader = new ImageLoader(this.mRequestQueue, new ImageLoader.ImageCache() {
                private final LruCache<String, Bitmap> mCache = new LruCache<String, Bitmap>(10);
                public void putBitmap(String url, Bitmap bitmap) {
                    mCache.put(url, bitmap);
                }
                public Bitmap getBitmap(String url) {
                    return mCache.get(url);
                }
            });
        }
        return mRequestQueue;
    }

    public ImageLoader getImageLoader(){
        return this.mImageLoader;
    }

    public CookieManager getCookieManager(){
        return mCookieManager;
    }

    public <T> void addToRequestQueue(Request<T> req) {
        getRequestQueue().add(req);
    }

    public void setFollowRedirects(boolean followRedirects){
        mFollowRedirects = followRedirects;
    }
}
