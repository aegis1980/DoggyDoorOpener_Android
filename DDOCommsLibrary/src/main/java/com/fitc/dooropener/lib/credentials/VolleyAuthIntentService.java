package com.fitc.dooropener.lib.credentials;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.app.IntentService;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.android.volley.NetworkResponse;
import com.android.volley.NoConnectionError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.StringRequest;
import com.fitc.dooropener.lib.CommonApplication;
import com.fitc.dooropener.lib.server.SpecialResponseErrorListener;
import com.fitc.dooropener.lib.server.VolleySingleton;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookieStore;
import java.net.HttpCookie;
import java.util.List;

/**
* Authorises on Google App engine back end  using google accounts.
 *
 */
public class VolleyAuthIntentService extends IntentService {

    /**
     * If false then we just skip the whole process.
     */
    private static final boolean BOTHER_WITH_AUTH = false;

    private static final String TAG = VolleyAuthIntentService.class.getSimpleName();

    public static final String ACCOUNT_EXTRA = "account";
    private static final String ACSID = "ACSID";

    /**
     * Passes as a parameter
     */
    private Account mAccount;

     private AuthRequest authRequest;


    /**
     * Creates an IntentService.  Invoked by your subclass's constructor.
     *
     */
    public VolleyAuthIntentService() {
        super("VolleyAuthIntentService");
    }

    @Override
    protected void onHandleIntent(Intent workIntent) {
        Log.d(TAG,"Starting VolleyAuthIntentService");
        // Gets account from the incoming Intent
        mAccount = workIntent.getParcelableExtra(ACCOUNT_EXTRA);

        if (mAccount != null) {
            if (BOTHER_WITH_AUTH) {
                AccountManager accountManager = AccountManager.get(this);
                accountManager.getAuthToken(mAccount, "ah", null, false, new GetAuthTokenCallback(), null);
            } else {
                broadcastSuccessfulAuthIntent(true);
            }
        } else {
            Log.e(TAG, "No account object");
        }
    }


    private class GetAuthTokenCallback implements AccountManagerCallback<Bundle> {
        public void run(AccountManagerFuture<Bundle> result) {
            Bundle bundle;
            try {
                bundle = result.getResult();
                if (bundle.containsKey(AccountManager.KEY_INTENT)) {
                    Intent intent = bundle.getParcelable(AccountManager.KEY_INTENT);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                } else {
                    Log.i(TAG, bundle.getString(AccountManager.KEY_AUTHTOKEN));
                    onGetAuthToken(bundle);
                }
            } catch (OperationCanceledException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (AuthenticatorException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    ;

    private void onGetAuthToken(Bundle bundle) {
            Log.d(TAG,"onGetAuthToken");

            final String auth_token = bundle.getString(AccountManager.KEY_AUTHTOKEN);

            // Don't follow redirects
            VolleySingleton.getInstance(VolleyAuthIntentService.this).setFollowRedirects(false);

            final String uri = CommonApplication.getServerRootUrl() + "_ah/login?continue=http://localhost/&auth=" + auth_token;

            AuthRequestErrorListener errorListener = new AuthRequestErrorListener(this);

            // Request a string response from the provided URL.
            // response and error listeners all dealt with in AuthRequest (inner) class.
             authRequest = new AuthRequest(uri, errorListener);
            errorListener.queueErroredRequest(authRequest);

            // Add the request to the RequestQueue.
            VolleySingleton.getInstance(this).addToRequestQueue(authRequest);

    }

    /**
     * Called from {@code AuthRequest}
     * @return success
     */
    private boolean onAuthResponse(){
        Log.d(TAG, "onAuthResponse()");
        boolean success = false;

        VolleySingleton.getInstance(VolleyAuthIntentService.this).setFollowRedirects(true);
        int statusCode = authRequest.statusCode;

        if (statusCode != 302) {
            // Response should be a redirect
            success = false;
            Log.e(TAG, "Status code error. Needs to be 302, you got " + statusCode);
        }

        // get cookies from underlying
        // CookieStore
        CookieManager manager = VolleySingleton.getInstance(this).getCookieManager();
        CookieStore cookieJar =  manager.getCookieStore();
        List<HttpCookie> cookies = cookieJar.getCookies();

        HttpCookie theCookie = null;
        for (HttpCookie cookie : cookies) {
            Log.v(TAG,"Cookie - " + cookie);
            if (cookie.getName().equals(ACSID)){
                theCookie = cookie;
                success = true;
                break;
            }
        }
        Log.d(TAG,"Cookie success: " + success);
        CommonApplication.setGoogleAccountAuthenticated(success);
        CommonApplication.setGoogleAccountCookie(theCookie);
        broadcastSuccessfulAuthIntent(success);


        return success;
    }

    /**
     * Sends intent back to {@link com.fitc.dooropener.lib.BaseActivity} that auth process is successful
     * @param success
     */
    private void broadcastSuccessfulAuthIntent(boolean success) {
        // broadcast result.
        Intent intent = new Intent();
        intent.setAction(CommonApplication.ACTION_AUTH_RESPONSE);
        intent.putExtra(CommonApplication.EXTRA_AUTH_RESPONSE_SUCCESS, success);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private void onAuthResponseError(int statusCode) {
    }


    /**
     * Request that allows capture of the status code for redirects.
     */
    private class AuthRequest extends StringRequest{

        int statusCode;

        private AuthRequestErrorListener mErrorListener = new AuthRequestErrorListener(VolleyAuthIntentService.this);

        public AuthRequest(String url, AuthRequestErrorListener authErrorListener ) {
            super(Method.GET, url,
                    new Response.Listener<String>() {
                        @Override
                        public void onResponse(String response) {
                            onAuthResponse();
                        }
                    }, authErrorListener);
            this.setShouldCache(false);
        }

        @Override
        protected Response<String> parseNetworkResponse(NetworkResponse response) {
            statusCode = response.statusCode;
            return super.parseNetworkResponse(response);
        }



    }

    public class AuthRequestErrorListener extends SpecialResponseErrorListener{

        public AuthRequestErrorListener(Service c) {
            super(c);
        }

        @Override
        public void onErrorResponse(VolleyError error) {
            super.onErrorResponse(error);

            VolleyAuthIntentService.this.authRequest.statusCode = error.networkResponse.statusCode;
            onAuthResponse();
        }
    }



}
