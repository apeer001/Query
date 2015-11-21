package com.ubiqlog.query.query.Services;

import android.content.Intent;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.data.FreezableUtils;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;
import com.ubiqlog.query.query.FrontEnd.SpeechActivity;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 *  Author: AP
 *  WearablelistenerService in the Query wear App
 *
 */
public class QeuryWearableListenerService extends WearableListenerService {


    GoogleApiClient mClient;

    public final static String POS_KEY = "Result";
    private static final String DATA_ITEM_RECEIVED_PATH = "/data-item-received";


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.e(getClass().getSimpleName(), "wearable listener service onStartCommand() called");
        if (mClient != null) {
            mClient.connect();
        }
        //return super.onStartCommand(intent, flags, startId);
        return START_STICKY;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.e(getClass().getSimpleName(), "wearable listener service onCreate() called");
        mClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
                    @Override
                    public void onConnected(Bundle bundle) {
                        Log.d(getClass().getSimpleName(), "Successful connect");
                    }

                    @Override
                    public void onConnectionSuspended(int i) {

                    }
                })
                .build();

        mClient.connect();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mClient != null) {
            mClient.disconnect();
        }
    }


    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
        //Log.d(TAG, "Listening > data changed: ");
        final List<DataEvent> events = FreezableUtils.freezeIterable(dataEvents);

        for (DataEvent event : events) {
            if (event.getType() == DataEvent.TYPE_CHANGED) {
                //com.ubiqlog.query.query.Data item changed
                DataItem item = event.getDataItem();
                String itemURI = item.getUri().getPath().toLowerCase();
                Uri uri = event.getDataItem().getUri();

                if (itemURI.equals("/nlpwear")) {
                    Log.d(getClass().getSimpleName(), "NLP wear transfers Complete");

                    /*
                    ConnectionResult connectionResult =
                        mClient.blockingConnect(30, TimeUnit.SECONDS);

                    if (!connectionResult.isSuccess()) {
                        Log.e(getClass().getSimpleName(), "Failed to connect to GoogleApiClient.");
                        return;
                    }
                    */

                    DataMap dataMap = DataMapItem.fromDataItem(item).getDataMap();
                    String POSStr = dataMap.getString(POS_KEY);
                    Log.d(getClass().getSimpleName(), "POSstr: " + POSStr);

                    // Send to Speech activity broadcast receiver
                    Intent intent = new Intent(SpeechActivity.FINISHED_PARSE);
                    intent.putExtra(SpeechActivity.WEAR_RESULT, POSStr);
                    sendBroadcast(intent);

                    // Get the node id from the host value of the URI
                    String nodeId = uri.getHost();
                    // Set the data of the message to be the bytes of the URI
                    byte[] payload = uri.toString().getBytes();

                    // Send the RPC
                    //Wearable.MessageApi.sendMessage(mClient, nodeId,
                    //        DATA_ITEM_RECEIVED_PATH, payload);

                } else {
                    Log.i(getClass().getSimpleName(), "unknown item: " + item.toString());
                }

            }
        }
    }

    public void buildGoogleApiClient() {
        mClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
                    @Override
                    public void onConnected(Bundle bundle) {
                        //Log.d(TAG, "Successful connect");
                    }

                    @Override
                    public void onConnectionSuspended(int i) {

                    }
                })
                .build();
        if (mClient != null) {
            mClient.connect();
        }
    }
}
