package com.ubiqlog.query.Services;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
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
import com.ubiqlog.query.FrontEnd.MainActivity;
import com.ubiqlog.query.OpenNLP.POSParser;
import com.ubiqlog.query.Tasks.SendDataTask;

import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Created by Aaron on 11/18/2015.
 */
public class QueryWearableListenerService extends WearableListenerService {

    GoogleApiClient mClient;
    Handler mHandler;

    public final static String POS_KEY = "Result";
    public final static String POS_BOOL = "wearableListenerBool";
    public POSParser posParser;

    Runnable r;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.e(getClass().getSimpleName(), "Starting WearablelistenerService OnStartCommand()");
        if (mClient != null) {
            mClient.connect();
        }
        //return super.onStartCommand(intent, flags, startId);
        return START_STICKY;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.e(getClass().getSimpleName(), "Starting WearablelistenerService OnCreate()");
        mClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
                    @Override
                    public void onConnected(Bundle bundle) {
                        //Log.d(TAG, "Successful connect");
                        Log.e(getClass().getSimpleName(), "Google api client connected in onCreate()");
                    }

                    @Override
                    public void onConnectionSuspended(int i) {

                    }
                })
                .build();

        mClient.connect();
        Log.e(getClass().getSimpleName(), "Google api client connected in onCreate()");

        final ExecutorService executor = Executors.newFixedThreadPool(1);
        executor.execute(new Runnable() {
            @Override
            public void run() {
                posParser = new POSParser(getApplicationContext());
                Log.e(getClass().getSimpleName(), "exited posParser constructor should finish now");
            }
        });
        executor.shutdown();

        r = new Runnable() {
            @Override
            public void run() {
                //Log.e(getClass().getSimpleName(), "Inside handler in wearable listener mobile");
                mHandler.postDelayed(this, 2000);
                if (executor.isTerminated()) {
                    //Log.e(getClass().getSimpleName(), "Inside handler run()");
                    Intent intent = new Intent(MainActivity.FINISHED_LOAD);
                    intent.putExtra(POS_BOOL, true);
                    sendBroadcast(intent);
                    //mHandler.removeCallbacks(this);
                }

            }
        };

        mHandler = new Handler();
        mHandler.postDelayed(r, 2000);
    }

    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
        Log.d(getClass().getSimpleName(), "Listening > data changed: ");
        final List<DataEvent> events = FreezableUtils.freezeIterable(dataEvents);

        for (DataEvent event : events) {
            if (event.getType() == DataEvent.TYPE_CHANGED) {
                //com.ubiqlog.query.query.Data item changed
                DataItem item = event.getDataItem();
                String itemURI = item.getUri().getPath().toLowerCase();

                if (itemURI.equals("/nlpmobile")) {
                    // Handlge NLP processing and return result back to wear
                    Log.e(getClass().getSimpleName(), "NLP transfers Complete");

                    Log.e(getClass().getSimpleName(), "POS String Data Received");
                    DataMap dataMap = DataMapItem.fromDataItem(item).getDataMap();
                    String POSString = dataMap.getString(POS_KEY);
                    Log.e(getClass().getSimpleName(), "POS String Data: " + POSString);

                    if (posParser != null) {
                        String POSArray[] = posParser.collectAllVerbTags(posParser.getTags(POSString));
                        if (POSArray != null) {
                            POSString = "";
                            if (POSArray.length == 1) {
                                POSString = POSArray[0];
                            } else if (POSArray.length > 1) {
                                for (int i = 0; i < POSArray.length - 1; i++) {
                                    POSString += POSArray[i] + " ";
                                }
                                POSString += POSArray[POSArray.length - 1];
                            }

                        /*
                        ConnectionResult connectionResult =
                                mClient.blockingConnect(30, TimeUnit.SECONDS);

                        if (!connectionResult.isSuccess()) {
                            //Log.e(TAG, "Failed to connect to GoogleApiClient.");
                            return;
                        }
                        */

                        /*
                        final PutDataMapRequest outgoingDataMap = PutDataMapRequest.create("/nlpwear");
                        outgoingDataMap.getDataMap().putString("Date", new Date().toString());
                        outgoingDataMap.getDataMap().putString(POS_KEY, POSString);
                        PutDataRequest request = outgoingDataMap.asPutDataRequest();
                        PendingResult<DataApi.DataItemResult> pendingResult = Wearable.DataApi.putDataItem(mClient, request);
                        pendingResult.setResultCallback(new ResultCallback<DataApi.DataItemResult>() {
                            @Override
                            public void onResult(DataApi.DataItemResult dataItemResult) {
                                // something
                                if (dataItemResult.getStatus().isSuccess()) {
                                    Log.d(getClass().getSimpleName(), "Success DataMap: " + outgoingDataMap.getDataMap());
                                } else {
                                    Log.d(getClass().getSimpleName(), "Transfer was not successful");
                                }
                            }
                        });
                        */
                            SendDataTask sendDataTask = new SendDataTask(getApplicationContext(), POSString, mClient);
                            sendDataTask.execute();

                            //mClient.disconnect();
                        }
                    }

                } else {
                    //Log.i(TAG, "unknown item: " + item.toString());
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

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mClient != null) {
            mClient.disconnect();
        }
        if (mHandler != null) {
            mHandler.removeCallbacks(r);
            //Log.e(getClass().getSimpleName(), "Inside handler run()");
            Intent intent = new Intent(MainActivity.FINISHED_LOAD);
            intent.putExtra(POS_BOOL, false);
            sendBroadcast(intent);
        }
        mHandler = null;
    }
}
