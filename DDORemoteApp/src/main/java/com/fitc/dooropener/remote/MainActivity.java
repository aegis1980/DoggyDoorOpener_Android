package com.fitc.dooropener.remote;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.CardView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.NetworkImageView;
import com.fitc.dooropener.lib.BaseActivity;
import com.fitc.dooropener.lib.CommonApplication;
import com.fitc.dooropener.lib.gcm.GcmDataPayload;
import com.fitc.dooropener.lib.server.MapBundler;
import com.fitc.dooropener.lib.server.ServerService;
import com.fitc.dooropener.lib.server.VolleySingleton;
import com.fitc.dooropener.lib.ui.DoorControllerLayout;
import com.fitc.dooropener.lib.ui.SeekBarAnimation;
import com.fitc.wifitrawl.WifiDetailSnackBar;

import java.util.HashMap;
import java.util.Map;

public class MainActivity extends BaseActivity implements View.OnClickListener {

    private static final String TAG = MainActivity.class.getSimpleName();

    private DoorControllerLayout mDoorControllerLayout;
    private Button mDoorOpenButton, mDoorCloseButton;

    private CardView mSelfieCardView;
    private ImageView mSelfieImageView;
    private TextView mSelfieTextView;
    private FloatingActionButton mfloatingActionButton;
    private Snackbar mSnackBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);

        setSupportActionBar(toolbar);

        mCoordinatorLayout = (CoordinatorLayout) findViewById(R.id.parent);

        mfloatingActionButton = (FloatingActionButton) findViewById(R.id.fab);
        mfloatingActionButton.setOnClickListener(this);

        mDoorControllerLayout = (DoorControllerLayout) findViewById(R.id.door_opener_layout);

        mDoorOpenButton = (Button) findViewById(R.id.openButton);
        mDoorOpenButton.setOnClickListener(this);

        mDoorCloseButton = (Button) findViewById(R.id.closeButton);
        mDoorCloseButton.setOnClickListener(this);

        mSelfieCardView = (CardView) findViewById(R.id.selfieCardView);
        mSelfieCardView.setVisibility(View.GONE);
        mSelfieImageView = (ImageView) findViewById(R.id.selfieImageView);
        mSelfieTextView = (TextView) findViewById(R.id.selfieTextView);

    //    Log.e(TAG, CommonApplication.getEmail());
    }

    @Override
    public void onResume(){
        super.onResume();

        
    }

    @Override
    public void onPause(){
        super.onPause();

    }

    @Override
    public void onServerResponse(Context context, Intent intent) {


        if (intent.getAction().equals(CommonApplication.ACTION_SERVER_RESPONSE)) {
      //      mDoorOpenCloseSwitch.setActivated(true);
        }
    }

    @Override
    public void onGcmStatus(final GcmDataPayload status) {
          switch(status.getStatusType()){
              // if registered succesfully then get status of door.
              case GcmDataPayload.Status.GCM_REGISTER:
                  Log.d(TAG,"Getting Status from control");
                  sendRequestToServer(CommonApplication.ControlTask.STATUS);
                  break;
              case GcmDataPayload.Status.CONTROL_REPORT:
                  SeekBarAnimation anim;
                  float p=0;
                  switch (status.getStatusData()){
                      case CommonApplication.DoorAction.CLOSED:
                      case CommonApplication.DoorAction.CLOSING:
                          mDoorControllerLayout.startCloseDoorAnim();
                          break;
                      case CommonApplication.DoorAction.OPENED:
                      case CommonApplication.DoorAction.OPENING:
                          mDoorControllerLayout.startOpenDoorAnim();
                          break;

                  }
                  break;
              case GcmDataPayload.Status.IMAGE_READY_TO_VIEW:
                //  Log.d(TAG, "Loading image");


                  String url = CommonApplication.getServerImageDownloadUrl(status.getStatusData());
                  VolleySingleton.getInstance(this).getImageLoader().get(url, new ImageLoader.ImageListener() {

                      public void onErrorResponse(VolleyError arg0) {
                          Log.d(TAG, "onErrorResponse");
                          Snackbar.make(mCoordinatorLayout, "There was an error getting the image", Snackbar.LENGTH_LONG)
                                  .setAction("Action", null).show();
                      }

                      public void onResponse(ImageLoader.ImageContainer response, boolean arg1) {

                          if (response.getBitmap() != null) {
                              final Bitmap image = response.getBitmap();


                              mSelfieCardView.setVisibility(View.VISIBLE);
                              mSelfieImageView.setImageBitmap(response.getBitmap());
                              mSelfieTextView.setText(CommonApplication.getTimestampFromImageFileName(status.getStatusData()));
                          } //else
                          // image.setImageResource(R.drawable.icon_loading); // set the loading image while the download is in progress
                      }
                  });

                  break;
          }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_status) {
            sendRequestToServer(CommonApplication.ControlTask.STATUS);
            Snackbar.make(mCoordinatorLayout, "Requesting Current Door Status", Snackbar.LENGTH_SHORT)
                    .setAction("Action", null).show();
            return true;
        } else if (id== R.id.action_skype){
            Intent sky = new Intent("android.intent.action.VIEW");
            sky.setData(Uri.parse("skype:" + "doggydooropener"));
            startActivity(sky);
        } else if (id==R.id.action_network){
            // show snackbar with wifi details.
            WifiDetailSnackBar.make(this,mCoordinatorLayout);
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Starts {@code ServerService} sednring request to server with given task
     * @param requestTask
     * @return
     */
    private boolean sendRequestToServer(String requestTask) {
        Map<String,String> params = new HashMap<>();
        params.put(CommonApplication.EndPointParams.REQUEST_ID,ServerService.getRequestId());
        params.put(CommonApplication.EndPointParams.FROM_EMAIL, CommonApplication.getEmail());
        params.put(CommonApplication.EndPointParams.GCM_TOKEN,CommonApplication.getGcmRegId());
        params.put(CommonApplication.EndPointParams.COMMAND, requestTask);

        final Intent serviceIntent = new Intent(this, ServerService.class);
        serviceIntent.setAction(CommonApplication.ACTION_SERVER_REQUEST);
        serviceIntent.putExtra(CommonApplication.EXTRA_SERVER_REQUEST_TYPE_INT, CommonApplication.COMMAND);
        serviceIntent.putExtra(CommonApplication.BUNDLE_SERVER_REQUEST_PARAMS, MapBundler.toBundle(params));
        // Starts the IntentService
        startService(serviceIntent);
        return true;
    }

    //********************************************************************************************

    @Override
    public void onClick(View v) {
        final int id = v.getId();

    String task;
        if (id==mDoorOpenButton.getId()) {
            sendRequestToServer(CommonApplication.ControlTask.OPEN);
        } else if (id==mDoorCloseButton.getId()) {
            sendRequestToServer(CommonApplication.ControlTask.CLOSE);
        } else if (id==mfloatingActionButton.getId()){
            sendRequestToServer(CommonApplication.ControlTask.PHOTO);
            mSnackBar = Snackbar.make(mCoordinatorLayout, "Sending command to take photo", Snackbar.LENGTH_LONG);
            mSnackBar.show();
        }
    }
}
