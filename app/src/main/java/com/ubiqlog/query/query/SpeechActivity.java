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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_speech_watchviewstub);
        setAmbientEnabled();


        // Start of layout item creation
        WatchViewStub stub = (WatchViewStub) findViewById(R.id.watch_view_stub);
        stub.setOnLayoutInflatedListener(new WatchViewStub.OnLayoutInflatedListener() {

            @Override public void onLayoutInflated(WatchViewStub stub) {

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

                        promptSpeechInput();
                        if (myToolTipView != null) {
                            myToolTipView.remove();
                        }
                    }
                });

                // Create tool tips for speech button
                ToolTipRelativeLayout toolTipRelativeLayout = (ToolTipRelativeLayout) findViewById(R.id.activity_main_tooltipRelativeLayout);

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
                    ParseSpeech(result.get(0));
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
            }


        } else {

            Bitmap bmp = BitmapFactory.decodeResource(getResources(),
                    R.drawable.red_circle);
            statusLight1.setImageBitmap(bmp);
            statusLight2.setImageBitmap(bmp);
            statusLight3.setImageBitmap(bmp);
        }

        return tokenVector;

    }

}
