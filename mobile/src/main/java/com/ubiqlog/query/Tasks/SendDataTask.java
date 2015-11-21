package com.ubiqlog.query.Tasks;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;
import com.ubiqlog.query.Services.QueryWearableListenerService;

import java.util.Date;

/**
 * Created by Aaron P on 11/19/2015.
 * Mobile
 *
 */
public class SendDataTask extends AsyncTask<Void, Void, Void> {

    private final String contents;
    private final Context mContext;
    private GoogleApiClient googleApiClient;

    public SendDataTask(Context c, String contents, GoogleApiClient googleApiClient) {
        this.contents = contents;
        this.mContext = c;
        this.googleApiClient = googleApiClient;
    }

    @Override
    protected Void doInBackground(Void... nodes) {

        // Create datamap request
        PutDataMapRequest dataMap = PutDataMapRequest.create("/nlpwear");

        // Add elements to datamap
        dataMap.getDataMap().putString("Date", new Date().toString());
        dataMap.getDataMap().putString(QueryWearableListenerService.POS_KEY, contents);

        PutDataRequest request = dataMap.asPutDataRequest();
        // Send over DataApi
        DataApi.DataItemResult dataItemResult = Wearable.DataApi
                .putDataItem(googleApiClient, request)
                .await();

        if (dataItemResult.getStatus().isSuccess()) {
            Log.e(getClass().getSimpleName(), "[mobile]Sent successfully");
        } else {
            Log.e (getClass().getSimpleName(), "[mobile]There has been a problem sending the datamap");
        }

        return null;
    }
}