package com.ubiqlog.query.FrontEnd;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.Wearable;
import com.ubiqlog.query.R;
import com.ubiqlog.query.Services.QueryWearableListenerService;

/**
 *  Author: Aaron Peery
 *  Main UI for Query App Query mobile app
 *
 */
public class MainActivity extends AppCompatActivity implements
                                                    //DataApi.DataListener,
                                                    GoogleApiClient.ConnectionCallbacks,
                                                    GoogleApiClient.OnConnectionFailedListener {

    public TextView statusText;

    public final static String FINISHED_LOAD = "com.ubiqlog.query.FrontEnd.MainActivity.action.FINISHED_LOAD";
    private final static String status_waiting = "Waiting for NLP Parts-Of-Speech Dictionary to load...";
    public final static String status_done = "Loaded NLP Parts-Of-Speech Dictionary successfully";

    GoogleApiClient mGoogleApiClient;
    public boolean finishedLoadingPOS = false;
    private static final String DATA_ITEM_RECEIVED_PATH = "/data-item-received";

    BroadcastReceiver b;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        statusText = (TextView) findViewById(R.id.statusText);
        statusText.setText(status_waiting);

        // Setup Broadcast receiver to receiver results from wearableListenerService
        b = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                //Log.d(getClass().getSimpleName(), "Received action intent in broadcast receiver");
                // Update UI when done loading POS parser
                boolean result = intent.getBooleanExtra(QueryWearableListenerService.POS_BOOL, false);
                if(!statusText.getText().toString().equals(status_done) && result) {
                    statusText.setText(status_done);
                } else if (!result) {
                    statusText.setText(status_waiting);
                    stopService(new Intent(MainActivity.this, QueryWearableListenerService.class));
                    startService(new Intent(MainActivity.this, QueryWearableListenerService.class));
                }
            }
        };

        IntentFilter filter = new IntentFilter(FINISHED_LOAD);
        registerReceiver(b, filter);

        // Load up service
        Log.e(getClass().getSimpleName(), "Starting WearablelistenerService from main");
        startService(new Intent(this, QueryWearableListenerService.class));

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

    }


    @Override
    public void onConnected(Bundle connectionHint) {
        if (Log.isLoggable(getClass().getSimpleName(), Log.DEBUG)) {
            Log.d(getClass().getSimpleName(), "Connected to Google Api Service");
        }
        //Wearable.DataApi.addListener(mGoogleApiClient, this);
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    protected void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
    }

    @Override
    protected void onStop() {
        if (null != mGoogleApiClient && mGoogleApiClient.isConnected()) {
            //Wearable.DataApi.removeListener(mGoogleApiClient, this);
            mGoogleApiClient.disconnect();
        }

        // unregister broadcast receiver
        if (b != null) {
            unregisterReceiver(b);
            b = null;
        }
        super.onStop();
    }

    /*
    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
        for (DataEvent event : dataEvents) {
            if (event.getType() == DataEvent.TYPE_DELETED) {
                Log.d(getClass().getSimpleName(), "DataItem deleted: " + event.getDataItem().getUri());
            } else if (event.getType() == DataEvent.TYPE_CHANGED) {
                Log.d(getClass().getSimpleName(), "DataItem changed: " + event.getDataItem().getUri());

                DataItem dataItem = event.getDataItem();
                String itemUri = dataItem.getUri().getPath().toLowerCase();
                Uri uri = dataItem.getUri();

                if (itemUri.equals(DATA_ITEM_RECEIVED_PATH))  {
                    byte bytes[] = dataItem.getData();
                    String str = "";
                    try {
                        str = new String(bytes, "UTF-8");
                        if (str.equals("finished")) {
                           finishedLoadingPOS = true;
                        } else {
                            finishedLoadingPOS = false;
                        }

                        Log.e(getClass().getSimpleName(), "Message Received(String transfer): " + str + ", " + finishedLoadingPOS);
                    } catch (UnsupportedEncodingException u) {
                        u.printStackTrace();
                    }
                }
            }
        }
    }
    */

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }
}
