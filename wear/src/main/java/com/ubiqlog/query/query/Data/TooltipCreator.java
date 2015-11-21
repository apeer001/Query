package com.ubiqlog.query.query.Data;


import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.util.Log;
import android.util.Pair;
import android.widget.ImageView;
import android.widget.TextView;

import com.nhaarman.supertooltips.ToolTip;
import com.nhaarman.supertooltips.ToolTipRelativeLayout;
import com.nhaarman.supertooltips.ToolTipView;
import com.ubiqlog.query.query.R;

import java.lang.reflect.ParameterizedType;
import java.util.Vector;

/**
 * Created by Aaron P on 11/20/2015.
 */
public class TooltipCreator {

    public TooltipCreator() {

    }

    public ToolTipView createSingleTooltipView(Activity activity, ToolTipView myToolTipView, ToolTipRelativeLayout toolTipRelativeLayout, String toolTxt) {
        ToolTip toolTip = new ToolTip()
                .withText(toolTxt)
                .withTextColor(Color.WHITE)
                .withColor(Color.GRAY)
                .withShadow()
                .withAnimationType(ToolTip.AnimationType.FROM_TOP);
        myToolTipView = toolTipRelativeLayout.showToolTipForView(toolTip, activity.findViewById(R.id.Button02));
        myToolTipView.setPointerCenterX(800);
        myToolTipView.setOnToolTipViewClickedListener(new ToolTipView.OnToolTipViewClickedListener() {
            @Override
            public void onToolTipViewClicked(ToolTipView toolTipView) {
                Log.e(getClass().getSimpleName(), "tool tip clicked");
                toolTipView.remove();
            }
        });
        return myToolTipView;
    }

    public ToolTip createSingleTooltip(String toolTxt) {
        ToolTip toolTip = new ToolTip()
                .withText(toolTxt)
                .withTextColor(Color.WHITE)
                .withColor(Color.GRAY)
                .withShadow()
                .withAnimationType(ToolTip.AnimationType.FROM_TOP);
        return toolTip;
    }
    public void toolTipTextViewUpdater(ToolTip toolTip, TextView txtSpeechInput, ParseAlgorithm parseAlgorithm,
                                       Vector<ImageView> statusLight, Bitmap bmp_green) {
        String tooltipTerm = toolTip.getText().toString();
        String text = txtSpeechInput.getText().toString().trim();
        if (parseAlgorithm.isQuestionTerm(tooltipTerm)) {
            Log.e(getClass().getSimpleName(), "Question added: " + tooltipTerm);
            if (tooltipTerm.equals(ParseStrings.whichOther) || tooltipTerm.equals(ParseStrings.doesOther)) {
                tooltipTerm = tooltipTerm.substring(0, tooltipTerm.indexOf(" "));
            }

            String temp = tooltipTerm + " " + text;
            txtSpeechInput.setText("");
            txtSpeechInput.setText(temp);
            statusLight.get(0).setImageBitmap(bmp_green);

        } else if (parseAlgorithm.isActionTerm(tooltipTerm)) {
            Log.e(getClass().getSimpleName(), "Action added: " + tooltipTerm);
            Pair<Integer, Integer> tempPair = null;

            int index = 0;
            int numChars = 0;
            if (parseAlgorithm.isQuestionTerm(parseAlgorithm.getPreviousAdditions().elementAt(0)) || text.toLowerCase().contains("which") || text.toLowerCase().contains("does")) {
                tempPair = getQuestionLengthIndex(text, parseAlgorithm.getPreviousAdditions().elementAt(0));
                index = tempPair.first;
                numChars = tempPair.second;
            }

            String ending = "";
            if (index + numChars < text.length()) {
                ending = text.substring(index + numChars);
            }
            String temp = text.substring(0,index + numChars) + " " + tooltipTerm + " " + ending;
            //String temp = "dog";
            txtSpeechInput.setText("");
            txtSpeechInput.setText(temp);
            statusLight.get(1).setImageBitmap(bmp_green);

        } else if (parseAlgorithm.isTimeTerm(tooltipTerm)) {
            Log.e(getClass().getSimpleName(), "Time added: " + tooltipTerm);
            String temp = txtSpeechInput.getText().toString().trim() + " " + tooltipTerm;
            txtSpeechInput.setText("");
            txtSpeechInput.setText(temp);
            statusLight.get(2).setImageBitmap(bmp_green);

        }
    }

    private Pair<Integer, Integer> getQuestionLengthIndex(String text, String questionTerm) {
        int numberOfChars = 0;
        int beginIndex = 0;

        Log.d(getClass().getSimpleName(), "Text: " + text + " Questionterm: " + questionTerm);
        if (text.toLowerCase().contains(questionTerm.toLowerCase())) {
            beginIndex = text.toLowerCase().indexOf(questionTerm.toLowerCase());
            numberOfChars = questionTerm.length();
        }
        return new Pair<>(beginIndex,numberOfChars);
    }
}
