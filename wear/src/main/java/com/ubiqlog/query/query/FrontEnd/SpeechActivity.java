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
import com.google.android.gms.wearable.Wearable;
import com.nhaarman.supertooltips.ToolTip;
import com.nhaarman.supertooltips.ToolTipRelativeLayout;
import com.nhaarman.supertooltips.ToolTipView;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;
import java.util.Random;
import java.util.Vector;

import com.ubiqlog.query.query.Data.ParseAlgorithm;
import com.ubiqlog.query.query.Data.ParseStrings;
import com.ubiqlog.query.query.Data.TooltipCreator;
import com.ubiqlog.query.query.OpenNLP.POSParser;
import com.ubiqlog.query.query.R;
import com.ubiqlog.query.query.Tasks.SendDataTask;
import com.ubiqlog.query.query.Services.QeuryWearableListenerService;

import opennlp.tools.tokenize.SimpleTokenizer;


/**
 *  Author: AP
 *  Main UI for speech in the Query App
 *
 */
public class SpeechActivity extends WearableActivity implements
                                                                GoogleApiClient.ConnectionCallbacks,
                                                                GoogleApiClient.OnConnectionFailedListener {

    private static final SimpleDateFormat AMBIENT_DATE_FORMAT = new SimpleDateFormat("HH:mm", Locale.US);
    private final int REQ_CODE_SPEECH_INPUT = 100;
    private static final Integer resourceIds [] = {R.id.statusCircle1, R.id.statusCircle2, R.id.statusCircle3};
    private Vector<Pair<Integer, ToolTip>> ttips = new Vector<>();
    private boolean status[] = new boolean[3];
    private BoxInsetLayout mContainerView;
    private TextView mTextView;
    private TextView mClockView;
    private TextView verbText;
    private TextView txtSpeechInput;
    private Button microphoneBtn;
    private ToolTipRelativeLayout toolTipRelativeLayout;
    private ToolTipView myToolTipView = null;
    private ImageView statusLight1;
    private ImageView statusLight2;
    private ImageView statusLight3;
    private Vector<String> parsedValues;
    private Vector<ImageView> imageViews = new Vector<>();
    private ParseAlgorithm parseAlgorithm = new ParseAlgorithm();
    private POSParser posParser;
    private ArrayList<String> tagtokens = new ArrayList<>();
    private TooltipCreator tooltipCreator;
    private Bitmap bmp_red;
    private Bitmap bmp_green;
    private GoogleApiClient mGoogleApiClient;
    public String POSString;
    private static final String DATA_ITEM_RECEIVED_PATH = "/data-item-received";
    public static final String FINISHED_PARSE = "com.ubiqlog.query.query.FrontEnd.SpeechActivity.action.FINISHED_PARSE";
    public static final String WEAR_RESULT = "wearableListenerStr";
    private BroadcastReceiver b;

    /**
     *  Time Comparison flag
     *  Used to determine when there are more than one Time terms involved in the parsed String
     */
    private boolean timeComparisonFlag;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_speech_watchviewstub);
        setAmbientEnabled();

        Log.e(getClass().getSimpleName(), "On Create() called");
        // Build(initialize) the Google api client
        buildGoogleApiClient();

        // Load up service
        Log.e(getClass().getSimpleName(), "Starting WearablelistenerService from main");
        startService(new Intent(this, QeuryWearableListenerService.class));

        // Start of layout item creation
        WatchViewStub stub = (WatchViewStub) findViewById(R.id.watch_view_stub);
        stub.setOnLayoutInflatedListener(new WatchViewStub.OnLayoutInflatedListener() {
            @Override
            public void onLayoutInflated(WatchViewStub stub) {
                setupOnCreate();
            }
        });
    }
    public void setupOnCreate() {
        // Verb list of tenses
        verbText = (TextView) findViewById(R.id.verbList);
        verbText.setText("waiting");
        verbText.setVisibility(View.GONE);

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

        imageViews.add(statusLight1);
        imageViews.add(statusLight2);
        imageViews.add(statusLight3);

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

        // Create tool tips for speech button
        tooltipCreator = new TooltipCreator();
        toolTipRelativeLayout = (ToolTipRelativeLayout) findViewById(R.id.activity_main_tooltipRelativeLayout);
        String toolTxt = "Touch to speak";
        myToolTipView = tooltipCreator.createSingleTooltipView(SpeechActivity.this, myToolTipView, toolTipRelativeLayout, toolTxt);

        microphoneBtn = (Button) findViewById(R.id.Button02);
        microphoneBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.e(getClass().getSimpleName(), "On button pressed ");
                // Reset timeComparisonFlag to false
                timeComparisonFlag = false;

                parseAlgorithm.clearPreviouslyAdded();
                ttips = new Vector<Pair<Integer, ToolTip>>();

                // Reset lights to Red
                statusLight1.setImageBitmap(bmp_red);
                statusLight2.setImageBitmap(bmp_red);
                statusLight3.setImageBitmap(bmp_red);

                // Reset all status
                Arrays.fill(status, false);

                // Prompt for new speech recognition
                promptSpeechInput();
                if (myToolTipView != null) {
                    myToolTipView.removeAllViews();
                } else {
                    Log.e(getClass().getSimpleName(), "Tooltipview was NULL");
                }
            }
        });

        // Get bitmap resources to use for lights
        bmp_red = BitmapFactory.decodeResource(getResources(), R.drawable.red_circle);
        bmp_green = BitmapFactory.decodeResource(getResources(), R.drawable.green_circle);
    }
    public void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
        mGoogleApiClient.connect();
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
     *  Author AP
     * Speech text parsing algorithm
     *
     * @param speech
     * @return vector of strings
     *
     **/
    public Vector<String> ParseSpeech(String speech) {
        Vector<String> tokenVector = new Vector<>();

        if(speech != null && !speech.equals("")) {
            Pair<Vector<String>, boolean[]> p = parseAlgorithm.parseSpeechText(getApplicationContext(), speech, status, imageViews);
            // update token vector
            tokenVector = p.first;
            // Update all status indicator booleans
            for (int i = 0; i < p.second.length; i++) {
                status[i] = p.second[i];
            }
            // update time comparison flag
            timeComparisonFlag = parseAlgorithm.getTimeComparisonFlag();
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

        try {
            int randomInt = 0;
            String suggestion = "";
            if (pValues.size() == 4) {
                if (pValues.get(0).equals("")) {
                    if (!txtSpeechInput.getText().toString().contains("how")) {
                        randomInt = randomGenerator.nextInt(ParseStrings.questionTerms.length-1);
                        suggestion = ParseStrings.questionTerms[randomInt];
                    } else {
                        randomInt = randomGenerator.nextInt(ParseStrings.howTerms.length-1);
                        suggestion = ParseStrings.howTerms[randomInt];
                    }

                    toolTips.add(new Pair<Integer, ToolTip>(0, tooltipCreator.createSingleTooltip(suggestion)));
                }

                if(pValues.get(1).equals("")) {
                    randomInt =  randomGenerator.nextInt(ParseStrings.actionTerms.length-1);
                    suggestion = ParseStrings.actionTerms[randomInt];
                    toolTips.add(new Pair<Integer, ToolTip>(1, tooltipCreator.createSingleTooltip(suggestion)));
                }

                if(pValues.get(2).equals("")) {
                    randomInt =  randomGenerator.nextInt(ParseStrings.timeTerms.length-1);
                    suggestion = ParseStrings.timeTerms[randomInt];
                    toolTips.add(new Pair<Integer, ToolTip>(2, tooltipCreator.createSingleTooltip(suggestion)));
                }

                /*
                if(pValues.get(3).equals("") && !timeComparisonFlag) {
                    // may be needed. Left empty for now
                }
                */
            }

            ttips = toolTips;
            Log.e(getClass().getSimpleName(), "Tooltips size: " + toolTips.size());
            if (!toolTips.isEmpty()) {
                /**
                 *   Experiment without tooltips
                 *   Comment this one line containing 'updateNewSuggestion'
                 *
                 **/
                ///*
                updateNewSuggestion();
                //*/
                /**
                 * End of experiment [without tooltips]v
                 */
            }

            Log.e(getClass().getSimpleName(), "Tooltips size: " + toolTips.size());
            return toolTips;
        } catch (ArrayIndexOutOfBoundsException a) {
            a.printStackTrace();
        }

        return null;
    }
    public void updateNewSuggestion() {
        if (ttips != null && !ttips.isEmpty()) {
            Pair<Integer, ToolTip> p = ttips.get(0);
            final ToolTip toolTip2 = p.second;

            Log.e(getClass().getSimpleName(), "!!!resource ID: " + p.first);
            myToolTipView = toolTipRelativeLayout.showToolTipForView(toolTip2, findViewById(resourceIds[p.first]));
            myToolTipView.setPointerCenterX(800);
            myToolTipView.setOnToolTipViewClickedListener(new ToolTipView.OnToolTipViewClickedListener() {
                @Override
                public void onToolTipViewClicked(ToolTipView toolTipView) {
                    Log.e(getClass().getSimpleName(), toolTip2.getText().toString());

                    if (checkStatusAllRed()) {
                        txtSpeechInput.setText("");
                    }
                    // Update the textview with new tooltip suggestion
                    tooltipCreator.toolTipTextViewUpdater(toolTip2, txtSpeechInput, parseAlgorithm, imageViews, bmp_green);
                    toolTipView.remove();

                    // update the lights with the inputs
                    parsedValues = ParseSpeech(txtSpeechInput.getText().toString());

                    // Make suggestions if there are missing values for parsed values
                    removeDuplicateTooltips();
                    updateNewSuggestion();

                    // Send String HERE
                    // Parts-of-speech parse for verb tense
                    final String speechText = txtSpeechInput.getText().toString();
                    sendToMobileToParseVerbs(speechText);
                }
            });

            // remove the tooltip because it is in use
            ttips.removeElementAt(0);
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
    public void sendToMobileToParseVerbs(final String speechText) {
        Log.e(getClass().getSimpleName(), "On sendToMobileToParseVerbs() called: " +speechText );
        Log.e(getClass().getSimpleName(), "Sending user string to mobile to be parsed for verbs");
        // Send the Speechtext to "/nlpmobile"
        SendDataTask sendDataTask = new SendDataTask(getApplicationContext(), speechText, mGoogleApiClient);
        sendDataTask.execute();
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
            mGoogleApiClient.disconnect();
        }
        if (b != null) {
            unregisterReceiver(b);
            b = null;
        }
        super.onStop();
    }
    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {}
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
    private void removeDuplicateTooltips() {
        if (ttips != null && !ttips.isEmpty()) {
            Pair<Integer, ToolTip> temp = ttips.elementAt(0);
            for (int i = 1; i < ttips.size(); i++) {
                if (temp.first.equals(ttips.get(i).first)) {
                    ttips.removeElementAt(i);
                    i--;
                } else {
                    temp = ttips.elementAt(i);
                }
            }
        }
    }
}






