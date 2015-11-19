package com.ubiqlog.query.query.FrontEnd;

import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.support.wearable.activity.WearableActivity;
import android.support.wearable.view.BoxInsetLayout;
import android.support.wearable.view.WatchViewStub;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;
import com.nhaarman.supertooltips.ToolTip;
import com.nhaarman.supertooltips.ToolTipRelativeLayout;
import com.nhaarman.supertooltips.ToolTipView;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;
import java.util.Random;
import java.util.Vector;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import com.ubiqlog.query.query.Data.ParseStrings;
import com.ubiqlog.query.query.OpenNLP.POSParser;
import com.ubiqlog.query.query.R;
import com.ubiqlog.query.query.Services.QeuryWearableListenerService;

import opennlp.tools.tokenize.SimpleTokenizer;


/**
 *  Author: AP
 *  Main UI for speech in the Query App
 *
 */
public class SpeechActivity extends WearableActivity  implements
        DataApi.DataListener, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    private static final SimpleDateFormat AMBIENT_DATE_FORMAT = new SimpleDateFormat("HH:mm", Locale.US);

    private final int REQ_CODE_SPEECH_INPUT = 100;

    private static final Integer resourceIds [] = {R.id.statusCircle1, R.id.statusCircle2, R.id.statusCircle3};
    private Vector<Pair<Integer, ToolTip>> ttips = new Vector<>();

    private boolean status[] = new boolean[3];


    /**
     *  Time Comparison flag
     *  Used to determine when there are more than one Time terms involved in the parsed String
     */
    private boolean timeComparisonFlag;

    private BoxInsetLayout mContainerView;
    private TextView mTextView;
    private TextView mClockView;
    private TextView verbText;
    private Button microphoneBtn;
    private TextView txtSpeechInput;

    private ToolTipRelativeLayout toolTipRelativeLayout;
    private ToolTipView myToolTipView = null;
    private ImageView statusLight1;
    private ImageView statusLight2;
    private ImageView statusLight3;

    private Vector<String> parsedValues;

    private POSParser posParser;
    private ArrayList<String> tagtokens = new ArrayList<>();

    //Thread t;

    GoogleApiClient mGoogleApiClient;
    public String POSString;
    private static final String DATA_ITEM_RECEIVED_PATH = "/data-item-received";
    public static final String FINISHED_PARSE = "com.ubiqlog.query.query.FrontEnd.SpeechActivity.action.FINISHED_PARSE";
    public static final String WEAR_RESULT = "wearableListenerStr";

    BroadcastReceiver b;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_speech_watchviewstub);
        setAmbientEnabled();

        Log.e(getClass().getSimpleName(), "On Create() called");

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();


        // Load up service
        Log.e(getClass().getSimpleName(), "Starting WearablelistenerService from main");
        startService(new Intent(this, QeuryWearableListenerService.class));
        /*
        // Initialize POSParser
        t = new Thread(new Runnable() {
            @Override
            public void run() {
                posParser = new POSParser(getApplicationContext());
            }
        });

        t.setPriority(Thread.MAX_PRIORITY);
        t.start();

        try {
            t.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        */

        // Start of layout item creation
        WatchViewStub stub = (WatchViewStub) findViewById(R.id.watch_view_stub);
        stub.setOnLayoutInflatedListener(new WatchViewStub.OnLayoutInflatedListener() {

            @Override
            public void onLayoutInflated(WatchViewStub stub) {
                verbText = (TextView) findViewById(R.id.verbList);
                verbText.setText("waiting");
                b = new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        Log.d(getClass().getSimpleName(), "Received action intent in broadcast receiver");
                        // Update UI when done loading POS parser
                        if (intent != null) {
                            POSString = intent.getStringExtra(WEAR_RESULT);
                            verbText.setText(POSString);
                            String POSArray[] = SimpleTokenizer.INSTANCE.tokenize(POSString);
                            if (POSArray != null) {
                                for (String s : POSArray) {
                                    Log.e(getClass().getSimpleName(), s);
                                }
                                tagtokens = new ArrayList<String>(Arrays.asList(POSArray));
                            }

                        }
                    }
                };

                IntentFilter filter = new IntentFilter(FINISHED_PARSE);
                registerReceiver(b, filter);

                // Set timeComp flag to false
                timeComparisonFlag = false;

                Arrays.fill(status, false);
                // Now you can access your views
                statusLight1 = (ImageView) findViewById(R.id.statusCircle1);
                statusLight2 = (ImageView) findViewById(R.id.statusCircle2);
                statusLight3 = (ImageView) findViewById(R.id.statusCircle3);

                /**
                 *  Experiment without status light indicators
                 *  Uncomment these 3 visibility lines to set them to be invisible
                 */
                /*
                statusLight1.setVisibility(View.INVISIBLE);
                statusLight2.setVisibility(View.INVISIBLE);
                statusLight3.setVisibility(View.INVISIBLE);
                */

                /**
                 * End of experiment [without indicator lights]
                 */

                mContainerView = (BoxInsetLayout) findViewById(R.id.container);
                mTextView = (TextView) findViewById(R.id.text);
                txtSpeechInput = (TextView) findViewById(R.id.SpeechText);

                microphoneBtn = (Button) findViewById(R.id.Button02);
                microphoneBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Log.e(getClass().getSimpleName(), "On button pressed ");
                        // Reset timeComparisonFlag to false
                        timeComparisonFlag = false;

                        // Reset lights to Red
                        Bitmap bmp = BitmapFactory.decodeResource(getResources(),
                                R.drawable.red_circle);
                        statusLight1.setImageBitmap(bmp);
                        statusLight2.setImageBitmap(bmp);
                        statusLight3.setImageBitmap(bmp);

                        Arrays.fill(status, false);

                        promptSpeechInput();
                        if (myToolTipView != null) {
                            myToolTipView.remove();
                        }

                    }
                });

                // Create tool tips for speech button
                toolTipRelativeLayout = (ToolTipRelativeLayout) findViewById(R.id.activity_main_tooltipRelativeLayout);

                ToolTip toolTip = new ToolTip()
                        .withText("Touch to speak")
                        .withTextColor(Color.WHITE)
                        .withColor(Color.GRAY)
                        .withShadow()
                        .withAnimationType(ToolTip.AnimationType.FROM_TOP);
                myToolTipView = toolTipRelativeLayout.showToolTipForView(toolTip, findViewById(R.id.Button02));
                myToolTipView.setPointerCenterX(800);
                myToolTipView.setOnToolTipViewClickedListener(new ToolTipView.OnToolTipViewClickedListener() {
                    @Override
                    public void onToolTipViewClicked(ToolTipView toolTipView) {
                        Log.e(getClass().getSimpleName(), "tool tip clicked");
                        toolTipView.remove();
                    }
                });
            }
        });
    }

    @Override
    public void onEnterAmbient(Bundle ambientDetails) {
        super.onEnterAmbient(ambientDetails);
        updateDisplay();
    }

    @Override
    public void onUpdateAmbient() {
        super.onUpdateAmbient();
        updateDisplay();
    }

    @Override
    public void onExitAmbient() {
        updateDisplay();
        super.onExitAmbient();
    }

    private void updateDisplay() {
        if (isAmbient()) {
            try {
                mContainerView.setBackgroundColor(getResources().getColor(android.R.color.black));
                mTextView.setTextColor(getResources().getColor(android.R.color.white));
                mClockView.setVisibility(View.VISIBLE);

                mClockView.setText(AMBIENT_DATE_FORMAT.format(new Date()));
            } catch (NullPointerException n) {
                n.printStackTrace();
            }

        } else {
            mContainerView.setBackground(null);

            try {
                mTextView.setTextColor(getResources().getColor(android.R.color.black));
                mClockView.setVisibility(View.GONE);
            } catch (NullPointerException n) {
                n.printStackTrace();
            }

        }
    }

    /**
     * Showing google speech input dialog
     * */
    private void promptSpeechInput() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT,
                getString(R.string.speech_prompt));
        try {
            startActivityForResult(intent, REQ_CODE_SPEECH_INPUT);
        } catch (ActivityNotFoundException a) {
            Toast.makeText(getApplicationContext(),
                    getString(R.string.speech_not_supported),
                    Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Receiving speech input
     * */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case REQ_CODE_SPEECH_INPUT: {
                if (resultCode == RESULT_OK && null != data) {

                    final ArrayList<String> result = data
                            .getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                    txtSpeechInput.setText(result.get(0));

                    // Parse text from user and update status lights
                    parsedValues = ParseSpeech(result.get(0));
                    // Make suggestions if there are missing values for parsed values
                    setupTooltipSuggestions(parsedValues);


                    // Send String HERE
                    sendToMobileToParseVerbs(result.get(0));

                    /*
                    // Parts-of-speech parse for verb tense
                    ExecutorService executor = Executors.newFixedThreadPool(1);
                    executor.execute(new Runnable() {
                        @Override
                        public void run() {
                            String POSArray[] = posParser.collectAllVerbTags(posParser.getTags(result.get(0)));
                            if (POSArray != null) {
                                for (String s : POSArray) {
                                    Log.e(getClass().getSimpleName(), s);
                                }
                                tagtokens = new ArrayList<String>(Arrays.asList(POSArray));
                            }
                        }
                    });
                    executor.shutdown();
                    */
                }
                break;
            }
        }
    }

    /**
     *  Author AP
     * Speech text parsing algorithm
     *
     * @param speech
     * @return vector of strings
     *
     **/
    public Vector<String> ParseSpeech(String speech) {
        Vector<String> tokenVector = new Vector<>();
        Boolean bools [] = new Boolean[4];
        Arrays.fill(bools, false);

        if(speech != null && !speech.equals("")) {
            Bitmap bmp = BitmapFactory.decodeResource(getResources(),
                    R.drawable.green_circle);

            for (String s : ParseStrings.questionTerms) {

                if (s.toLowerCase().contains("which") || s.toLowerCase().contains("does")) {
                    String split [] = s.toLowerCase().split(" ");
                    if (speech.toLowerCase().contains(split[0])) {
                        String stripSpeech [] = speech.toLowerCase().split(" ");
                        for (int i = 0; i < stripSpeech.length; i++) {
                            if (stripSpeech[i].equals(split[0]) && i+1 < stripSpeech.length) {
                                tokenVector.add(split[0] + stripSpeech[i+1]);
                                bools[0] = true;
                                break;
                            }
                        }
                    }
                } else {
                    if (speech.toLowerCase().contains(s.toLowerCase())) {
                        tokenVector.add(s.toLowerCase());
                        bools[0] = true;
                        break;
                    }
                }
            }
            if (bools[0].equals(false)) {
                tokenVector.add("");
            } else {
                statusLight1.setImageBitmap(bmp);
                status[0] = true;
            }

            for (String s : ParseStrings.actionTerms) {
                if (speech.toLowerCase().contains(s.toLowerCase())) {
                    tokenVector.add(s.toLowerCase());
                    bools[1] = true;
                    break;
                }
            }
            if (bools[1].equals(false)) {
                tokenVector.add("");

            } else {
                statusLight2.setImageBitmap(bmp);
                status[1] = true;
            }

            int countTimesterms = 0;
            for (String s : ParseStrings.timeTerms) {
                if (speech.toLowerCase().contains(s.toLowerCase())) {
                    if (!timeComparisonFlag) {
                        tokenVector.add(s.toLowerCase());
                        countTimesterms++;
                        if (1 < countTimesterms) {
                            timeComparisonFlag = true;
                        }
                    }
                    bools[2] = true;
                    if (timeComparisonFlag) {
                        break;
                    }
                }
            }
            if (bools[2].equals(false)) {
                tokenVector.add("");
            } else {
                statusLight3.setImageBitmap(bmp);
                status[2] = true;
            }


            for (String s : ParseStrings.aggregationTerms) {
                if (speech.toLowerCase().contains(s.toLowerCase())) {
                    tokenVector.add(s.toLowerCase());
                    bools[3] = true;
                    break;
                }
            }
            if (bools[3].equals(false)) {
                tokenVector.add("");
            } else {
                //statusLight4.setImageBitmap(bmp);
                //status[3] = true;
            }


        } else {

            Bitmap bmp = BitmapFactory.decodeResource(getResources(), R.drawable.red_circle);
            statusLight1.setImageBitmap(bmp);
            statusLight2.setImageBitmap(bmp);
            statusLight3.setImageBitmap(bmp);

            Arrays.fill(status, false);
        }

        return tokenVector;

    }

    public Vector<Pair<Integer, ToolTip>> setupTooltipSuggestions(final Vector<String> pValues) {

        // Make tooltips to give suggestions
        Random randomGenerator =  new Random();
        Vector<Pair<Integer, ToolTip>> toolTips = new Vector<>();

        int randomInt = 0;
        String suggestion = "";
        if (pValues.size() >= 4) {
            if (pValues.get(0).equals("")) {

                if (!txtSpeechInput.getText().toString().contains("how")) {
                    randomInt = randomGenerator.nextInt(ParseStrings.questionTerms.length);
                    suggestion = ParseStrings.questionTerms[randomInt];
                } else {
                    randomInt = randomGenerator.nextInt(ParseStrings.howTerms.length);
                    suggestion = ParseStrings.howTerms[randomInt];
                }
                ToolTip toolTip = new ToolTip()
                        .withText(suggestion)
                        .withTextColor(Color.WHITE)
                        .withColor(Color.GRAY)
                        .withShadow()
                        .withAnimationType(ToolTip.AnimationType.FROM_TOP);
                toolTips.add(new Pair<Integer, ToolTip>(0, toolTip));
            }

            if(pValues.get(1).equals("")) {
                randomInt =  randomGenerator.nextInt(ParseStrings.actionTerms.length);
                suggestion = ParseStrings.actionTerms[randomInt];
                ToolTip toolTip = new ToolTip()
                        .withText(suggestion.toLowerCase())
                        .withTextColor(Color.WHITE)
                        .withColor(Color.GRAY)
                        .withShadow()
                        .withAnimationType(ToolTip.AnimationType.FROM_TOP);
                toolTips.add(new Pair<Integer, ToolTip>(1,toolTip));
            }

            if(pValues.get(2).equals("")) {
                randomInt =  randomGenerator.nextInt(ParseStrings.timeTerms.length);
                suggestion = ParseStrings.timeTerms[randomInt];
                ToolTip toolTip = new ToolTip()
                        .withText(suggestion.toLowerCase())
                        .withTextColor(Color.WHITE)
                        .withColor(Color.GRAY)
                        .withShadow()
                        .withAnimationType(ToolTip.AnimationType.FROM_TOP);
                toolTips.add(new Pair<Integer, ToolTip>(2,toolTip));
            }

            if(pValues.get(3).equals("") && !timeComparisonFlag || timeComparisonFlag && pValues.get(4).equals("") ) {
                // may be needed. Left empty for now
            }

        }

        if (toolTips.size() > 0) {
            final ToolTip toolTip = toolTips.get(0).second;
            /**
             *   Experiment without tooltips
             *   Comment these 3 lines containing 'myToolTipView'
             *
             **/
            ///*
            myToolTipView = toolTipRelativeLayout.showToolTipForView(toolTip, findViewById(resourceIds[toolTips.get(0).first]));
            myToolTipView.setPointerCenterX(800);
            myToolTipView.setOnToolTipViewClickedListener(new ToolTipView.OnToolTipViewClickedListener() {
                @Override
                public void onToolTipViewClicked(ToolTipView toolTipView) {

                    if (checkStatusAllRed()) {
                        txtSpeechInput.setText("");
                    }

                    String temp = txtSpeechInput.getText().toString() + " " + toolTip.getText().toString();
                    txtSpeechInput.setText(temp);
                    toolTipView.remove();

                    // update the lights with the inputs
                    parsedValues = ParseSpeech(txtSpeechInput.getText().toString());

                    // Make suggestions if there are missing values for parsed values
                    updateNewSuggestion();
                    setupTooltipSuggestions(parsedValues);


                    // Send String HERE
                    // Parts-of-speech parse for verb tense
                    final String speechText = txtSpeechInput.getText().toString();
                    sendToMobileToParseVerbs(speechText);

                    /*
                    Log.e(getClass().getSimpleName(), "Speech text value(1): " + speechText);
                    ExecutorService executor = Executors.newFixedThreadPool(1);
                    executor.execute(new Runnable() {
                        @Override
                        public void run() {
                            Log.e(getClass().getSimpleName(), "Speech text value(thread1): " + speechText);
                            String POSArray[] = posParser.collectAllVerbTags(posParser.getTags(speechText));
                            if (POSArray != null) {
                                for (String s : POSArray) {
                                    Log.e(getClass().getSimpleName(), s);
                                }
                                tagtokens = new ArrayList<String>(Arrays.asList(POSArray));
                            }
                        }
                    });
                    executor.shutdown();
                    */
                }
            });
            toolTips.remove(0);
            //*/

            /**
             * End of experiment [without tooltips]
             */
        }

        return toolTips;
    }

    public void updateNewSuggestion() {
        if (!ttips.isEmpty()) {
            Pair<Integer, ToolTip> p = ttips.get(0);
            final ToolTip toolTip2 = p.second;

            myToolTipView = toolTipRelativeLayout.showToolTipForView(toolTip2, findViewById(resourceIds[p.first]));
            myToolTipView.setPointerCenterX(800);
            myToolTipView.setOnToolTipViewClickedListener(new ToolTipView.OnToolTipViewClickedListener() {
                @Override
                public void onToolTipViewClicked(ToolTipView toolTipView) {
                    Log.e(getClass().getSimpleName(), toolTip2.getText().toString());

                    if (checkStatusAllRed()) {
                        txtSpeechInput.setText("");
                    }

                    String temp = txtSpeechInput.getText().toString() + " " + toolTip2.getText().toString();
                    txtSpeechInput.setText(temp);

                    toolTipView.remove();
                    updateNewSuggestion();

                    // update the lights with the inputs
                    parsedValues = ParseSpeech(txtSpeechInput.getText().toString());
                    // Make suggestions if there are missing values for parsed values
                    setupTooltipSuggestions(parsedValues);


                    // Send String HERE
                    // Parts-of-speech parse for verb tense
                    final String speechText = txtSpeechInput.getText().toString();
                    sendToMobileToParseVerbs(speechText);

                    /*
                    Log.e(getClass().getSimpleName(), "Speech text value(2): " + speechText);
                    ExecutorService executor = Executors.newFixedThreadPool(1);
                    executor.execute(new Runnable() {
                        @Override
                        public void run() {
                            Log.e(getClass().getSimpleName(), "Speech text value(thread2): " + speechText);
                            String POSArray[] = posParser.collectAllVerbTags(posParser.getTags(speechText));
                            if (POSArray != null) {
                                for (String s : POSArray) {
                                    Log.e(getClass().getSimpleName(), s);
                                }
                                tagtokens = new ArrayList<String>(Arrays.asList(POSArray));
                            }
                        }
                    });
                    executor.shutdown();
                    */

                }
            });

            // remove the tooltip because it is in use
            ttips.remove(0);
        }
    }

    public boolean checkStatusAllRed() {
        for (int i = 0; i < status.length; i++) {
            if (status[i]) {
                return false;
            }
        }
        return true;
    }


    @Override
    public void onConnected(Bundle connectionHint) {
        if (Log.isLoggable(getClass().getSimpleName(), Log.DEBUG)) {
            Log.d(getClass().getSimpleName(), "Connected to Google Api Service");
        }
        Wearable.DataApi.addListener(mGoogleApiClient, this);
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
            Wearable.DataApi.removeListener(mGoogleApiClient, this);
            mGoogleApiClient.disconnect();
        }
        if (b != null) {
            unregisterReceiver(b);
            b = null;
        }
        super.onStop();
    }

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
                        POSString = str;

                        Log.e(getClass().getSimpleName(), "Message Received(String transfer): " + POSString);
                    } catch (UnsupportedEncodingException u) {
                        u.printStackTrace();
                    }
                }
            }
        }
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }

    public void sendToMobileToParseVerbs(final String speechText) {

        Log.e(getClass().getSimpleName(), "On sendToMobileToParseVerbs() called: " +speechText );
        ExecutorService executor = Executors.newFixedThreadPool(1);
        executor.execute(new Runnable() {
            @Override
            public void run() {
                Log.e(getClass().getSimpleName(), "Sending user string to mobile to be parsed for verbs");
                ConnectionResult connectionResult =
                        mGoogleApiClient.blockingConnect(30, TimeUnit.SECONDS);

                if (!connectionResult.isSuccess()) {
                    //Log.e(TAG, "Failed to connect to GoogleApiClient.");
                    return;
                }

                final PutDataMapRequest outgoingDataMap = PutDataMapRequest.create("/nlpmobile");
                outgoingDataMap.getDataMap().putString("Date", new Date().toString());
                outgoingDataMap.getDataMap().putString(QeuryWearableListenerService.POS_KEY, speechText);
                PutDataRequest request = outgoingDataMap.asPutDataRequest();
                PendingResult<DataApi.DataItemResult> pendingResult = Wearable.DataApi.putDataItem(mGoogleApiClient, request);
                pendingResult.setResultCallback(new ResultCallback<DataApi.DataItemResult>() {
                    @Override
                    public void onResult(DataApi.DataItemResult dataItemResult) {
                        // something
                        if (dataItemResult.getStatus().isSuccess()) {
                            //Log.d(TAG, "Success DataMap: " + outgoingDataMap.getDataMap());
                        } else {
                            //Log.d(TAG, "Transfer was not successful");
                        }
                    }
                });
                mGoogleApiClient.disconnect();
            }
        });
        executor.shutdown();
    }
}






