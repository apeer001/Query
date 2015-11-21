package com.ubiqlog.query.query.Data;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.util.Pair;
import android.widget.ImageView;

import com.ubiqlog.query.query.R;

import java.util.Arrays;
import java.util.Vector;

/**
 * Created by Aaron on 11/20/2015.
 */
public class ParseAlgorithm {

    private Vector<String> previousAdditions;
    private boolean timeComparisonFlag;

    public ParseAlgorithm () {
        this.previousAdditions = new Vector<>();
        this.timeComparisonFlag = false;
    }

    public boolean getTimeComparisonFlag() {
        return timeComparisonFlag;
    }

    public boolean isQuestionTerm(String term) {
        if (term.toLowerCase().contains(ParseStrings.which.toLowerCase())) {
            return  true;
        } else if (term.toLowerCase().contains(ParseStrings.does.toLowerCase())) {
            return true;
        }
        for (String s : ParseStrings.questionTerms) {
            if (s.toLowerCase().equals(term.toLowerCase())) {
                return true;
            }
        }
        return false;
    }
    public boolean isActionTerm(String term) {
        for (String s : ParseStrings.actionTerms) {
            if (s.toLowerCase().equals(term.toLowerCase())) {
                return true;
            }
        }
        return false;
    }
    public boolean isTimeTerm(String term) {
        for (String s : ParseStrings.timeTerms) {
            if (s.toLowerCase().equals(term.toLowerCase())) {
                return true;
            }
        }
        for (String s : ParseStrings.timeMonthTerms) {
            if (s.toLowerCase().equals(term.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    public Pair<Vector<String>, boolean[]> parseSpeechText(Context context, String speech,
                                                           boolean [] status,
                                                           Vector<ImageView> statusLight) {
        Vector<String> tokenVector = new Vector<>();
        Boolean bools[] = new Boolean[4];
        Arrays.fill(bools, false);

        Bitmap bmp = BitmapFactory.decodeResource(context.getResources(),
                R.drawable.green_circle);

        String Month = "";

        if (statusLight.size() == 3) {
            if (!status[0]) {
                for (String s : ParseStrings.questionTerms) {
                    if (s.toLowerCase().contains("which") || s.toLowerCase().contains("does")) {
                        String split[] = s.toLowerCase().split(" ");
                        if (speech.toLowerCase().contains(split[0])) {
                            String stripSpeech[] = speech.toLowerCase().split(" ");
                            for (int i = 0; i < stripSpeech.length; i++) {
                                if (stripSpeech[i].equals(split[0]) && i + 1 < stripSpeech.length && !previousAdditions.contains(split[0] + " " + stripSpeech[i+1])) {
                                    tokenVector.add(split[0] + " " + stripSpeech[i + 1]);
                                    previousAdditions.add(split[0] + " " + stripSpeech[i+1]);
                                    Log.e(getClass().getSimpleName(), "Added: " + split[0] + " " + stripSpeech[i + 1]);
                                    bools[0] = true;
                                    break;
                                }
                            }
                        }
                    } else {
                        if (speech.toLowerCase().contains(s.toLowerCase()) && !previousAdditions.contains(s.toLowerCase())) {
                            tokenVector.add(s.toLowerCase());
                            previousAdditions.add(s.toLowerCase());
                            Log.e(getClass().getSimpleName(), "Added: " + s);
                            bools[0] = true;
                            break;
                        }
                    }
                }
            }
            if (!bools[0] && !status[0]) {
                tokenVector.add("");
                Log.e(getClass().getSimpleName(), "[Status light 1] needs a tooltips");
            } else {
                statusLight.get(0).setImageBitmap(bmp);
                status[0] = true;
            }

            if (!status[1]) {
                for (String s : ParseStrings.actionTerms) {
                    if (speech.toLowerCase().contains(s.toLowerCase()) && !previousAdditions.contains(s.toLowerCase())) {
                        tokenVector.add(s.toLowerCase());
                        previousAdditions.add(s.toLowerCase());
                        Log.e(getClass().getSimpleName(), "Added: " + s);
                        bools[1] = true;
                        break;
                    }
                }
            }
            if (!bools[1] && !status[1]) {
                tokenVector.add("");
                Log.e(getClass().getSimpleName(), "[Status light 2] needs a tooltips");

            } else {
                statusLight.get(1).setImageBitmap(bmp);
                status[1] = true;
            }

            if (!status[2]) {
                Log.e(getClass().getSimpleName(), "Reading times");
                int countTimesterms = 0;
                for (String s : ParseStrings.timeTerms) {
                    if (speech.toLowerCase().contains(s.toLowerCase()) && !previousAdditions.contains(s.toLowerCase())) {
                        tokenVector.add(s.toLowerCase());
                        previousAdditions.add(s.toLowerCase());
                        Log.e(getClass().getSimpleName(), "Added: " + s);
                        bools[2] = true;
                        break;
                    }
                }
                ///*
                if (bools[2]) {
                    Log.e(getClass().getSimpleName(), "Found a time before month");
                } else {
                    Log.e(getClass().getSimpleName(), "Reading months");
                    for (String s : ParseStrings.timeMonthTerms) {
                        if (speech.toLowerCase().contains(s.toLowerCase())) {
                            if (!timeComparisonFlag) {

                                Log.e(getClass().getSimpleName(), "Added: " + s);
                                countTimesterms++;
                                if (tokenVector.size() <= 3) {
                                    Month = s.toLowerCase();
                                }
                                if (countTimesterms == 2) {
                                    timeComparisonFlag = true;
                                }
                            }
                            bools[2] = true;
                            if (timeComparisonFlag) {
                                Log.e(getClass().getSimpleName(), "Time comparison Flag value: " + timeComparisonFlag);
                                break;
                            }
                        }
                    }
                }
                //*/
            }
            if (!bools[2] && !status[2] && Month.equals("")) {
                Log.e(getClass().getSimpleName(), "[Status light 3] needs a tooltips");
                tokenVector.add("");
            } else {
                statusLight.get(2).setImageBitmap(bmp);
                status[2] = true;
                ///*
                if (!Month.equals("") && isTimeTerm(Month)) {
                    if(tokenVector.size() == 3) {
                        tokenVector.insertElementAt(Month, 2);
                        tokenVector.remove(tokenVector.size()-1);
                    } else if(tokenVector.size() < 3){
                        tokenVector.add(Month);
                    }
                }
                //*/
            }


            /*
            for (String s : ParseStrings.aggregationTerms) {
                if (speech.toLowerCase().contains(s.toLowerCase())) {
                    tokenVector.add(s.toLowerCase());
                    Log.e(getClass().getSimpleName(), "Added: " + s);
                    bools[3] = true;
                    break;
                }
            }
            */
            if (!bools[3]) {
                tokenVector.add("");
            } else {
                //statusLight4.setImageBitmap(bmp);
                //status[3] = true;
            }
        }

        return new Pair<>(tokenVector, status);
    }

    public void clearPreviouslyAdded() {
        previousAdditions.clear();
    }

    public Vector<String> getPreviousAdditions() {
        return previousAdditions;
    }
}
