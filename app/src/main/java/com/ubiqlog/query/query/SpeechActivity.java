package com.ubiqlog.query.query;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.support.wearable.activity.WearableActivity;
import android.support.wearable.view.BoxInsetLayout;
import android.support.wearable.view.WatchViewStub;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

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

import Data.ParseStrings;


/**
 *  Author: AP
 *  Main UI for speech in the Query App
 *
 */

public class SpeechActivity extends WearableActivity {

    private static final SimpleDateFormat AMBIENT_DATE_FORMAT =
            new SimpleDateFormat("HH:mm", Locale.US);

    private static final Integer resourceIds [] = {R.id.statusCircle1, R.id.statusCircle2, R.id.statusCircle3};
    private Vector<Pair<Integer, ToolTip>> ttips = new Vector<>();

    private boolean status[] = new boolean[3];


    private BoxInsetLayout mContainerView;
    private TextView mTextView;
    private TextView mClockView;

    private Button microphoneBtn;
    private ImageView circularImage;

    private TextView txtSpeechInput;
    private ImageButton btnSpeak;
    private final int REQ_CODE_SPEECH_INPUT = 100;

    private ToolTipView myToolTipView = null;

    private ImageView statusLight1;
    private ImageView statusLight2;
    private ImageView statusLight3;

    private ToolTipRelativeLayout toolTipRelativeLayout;

    private Vector<String> parsedValues;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_speech_watchviewstub);
        setAmbientEnabled();


        // Start of layout item creation
        WatchViewStub stub = (WatchViewStub) findViewById(R.id.watch_view_stub);
        stub.setOnLayoutInflatedListener(new WatchViewStub.OnLayoutInflatedListener() {

            @Override
            public void onLayoutInflated(WatchViewStub stub) {

                Arrays.fill(status, false);
                // Now you can access your views
                statusLight1 = (ImageView) findViewById(R.id.statusCircle1);
                statusLight2 = (ImageView) findViewById(R.id.statusCircle2);
                statusLight3 = (ImageView) findViewById(R.id.statusCircle3);

                mContainerView = (BoxInsetLayout) findViewById(R.id.container);
                mTextView = (TextView) findViewById(R.id.text);

                txtSpeechInput = (TextView) findViewById(R.id.SpeechText);
                microphoneBtn = (Button) findViewById(R.id.Button02);

                microphoneBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

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
            mContainerView.setBackgroundColor(getResources().getColor(android.R.color.black));
            mTextView.setTextColor(getResources().getColor(android.R.color.white));
            mClockView.setVisibility(View.VISIBLE);

            mClockView.setText(AMBIENT_DATE_FORMAT.format(new Date()));
        } else {
            mContainerView.setBackground(null);
            mTextView.setTextColor(getResources().getColor(android.R.color.black));
            mClockView.setVisibility(View.GONE);
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

                    ArrayList<String> result = data
                            .getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                    txtSpeechInput.setText(result.get(0));

                    // Parse text from user and update status lights
                    parsedValues = ParseSpeech(result.get(0));
                    // Make suggestions if there are missing values for parsed values
                    setupTooltipSuggestions(parsedValues);

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
     */
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

            for (String s : ParseStrings.timeTerms) {
                if (speech.toLowerCase().contains(s.toLowerCase())) {
                    tokenVector.add(s.toLowerCase());
                    bools[2] = true;
                    break;
                }
            }
            if (bools[2].equals(false)) {
                tokenVector.add("");
            } else {
                statusLight3.setImageBitmap(bmp);
                status[2] = true;
            }


            for (String s : ParseStrings.quantativeTerms) {
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

            Bitmap bmp = BitmapFactory.decodeResource(getResources(),
                    R.drawable.red_circle);
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
        if (pValues.size() == 4) {
            if (pValues.get(0).equals("")) {

                randomInt =  randomGenerator.nextInt(ParseStrings.questionTerms.length);
                suggestion = ParseStrings.questionTerms[randomInt];
                ToolTip toolTip = new ToolTip()
                        .withText(suggestion)
                        .withTextColor(Color.WHITE)
                        .withColor(Color.GRAY)
                        .withShadow()
                        .withAnimationType(ToolTip.AnimationType.FROM_TOP);
                toolTips.add(new Pair<Integer, ToolTip>(0,toolTip));
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

            if(pValues.get(3).equals("")) {
                // may be needed. Left empty for now
            }

        }

        if (toolTips.size() > 0) {
            final ToolTip toolTip = toolTips.get(0).second;


            myToolTipView = toolTipRelativeLayout.showToolTipForView(toolTip, findViewById(resourceIds[toolTips.get(0).first]));
            myToolTipView.setPointerCenterX(800);
            myToolTipView.setOnToolTipViewClickedListener(new ToolTipView.OnToolTipViewClickedListener() {
                @Override
                public void onToolTipViewClicked(ToolTipView toolTipView) {
                    Log.d(getClass().getSimpleName(), toolTip.getText().toString());

                    if (checkStatusAllRed()) {
                        txtSpeechInput.setText("");
                    }

                    String temp = txtSpeechInput.getText().toString() + " " + toolTip.getText().toString();
                    txtSpeechInput.setText(temp);
                    toolTipView.remove();
                    updateNewSuggestion();

                    // update the lights with the inputs
                    parsedValues = ParseSpeech(txtSpeechInput.getText().toString());
                    // Make suggestions if there are missing values for parsed values
                    setupTooltipSuggestions(parsedValues);
                }
            });

            toolTips.remove(0);
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
                    Log.d(getClass().getSimpleName(), toolTip2.getText().toString());

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

}
