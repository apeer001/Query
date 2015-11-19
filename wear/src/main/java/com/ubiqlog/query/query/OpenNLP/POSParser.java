package com.ubiqlog.query.query.OpenNLP;

import android.content.Context;
import android.util.Log;

import com.ubiqlog.query.query.R;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;

import opennlp.tools.postag.POSModel;
import opennlp.tools.postag.POSTaggerME;
import opennlp.tools.tokenize.SimpleTokenizer;


/**
 * Created by Aaron on 11/17/2015.
 */
public class POSParser {

    private POSTaggerME posTagger;

    /**
     *  Author: AP
     *  Constuctor that takes in a file from the /res folder
     *  names '/en_pos_maxent.bin' as the dictionary to refer to
     *  in order to give Parts-Of-Speech tags.
     */
    public POSParser(Context context){
        InputStream modelIn = null;
        /*
        try {
            modelIn = context.getResources().openRawResource(R.raw.en_pos_maxent);
            if (modelIn != null) {
                Log.e(getClass().getSimpleName(), "Getting Dictionary into POSParser");
                final POSModel posModel = new POSModel(modelIn);
                //posTagger = new POSTaggerME(getPOSModel(posModelUrl));
                posTagger = new POSTaggerME(posModel);
                if (posTagger != null) {
                    Log.e(getClass().getSimpleName(), "Successful read of Dictionary into POSParser");
                }
                else {
                    Log.e(getClass().getSimpleName(), "Failed to read of Dictionary into POSParser");
                }
                modelIn.close();
            } else {
                Log.e(getClass().getSimpleName(), "FAILED to load modelIn POSParser");
            }
        } catch (final IOException ioe) {
            Log.e(getClass().getSimpleName(), "POSParser FAILED1");
            ioe.printStackTrace();
        } finally {
            if (modelIn != null) {
                try {
                    modelIn.close();
                    Log.e(getClass().getSimpleName(), "POSParser FAILED2");
                } catch (final IOException e) {

                }
            }
        }
        */
    }


    /**
     *  Get tags for string or tokenized array
     *
     */
    public String[] getTags(String str) {
        Log.e(getClass().getSimpleName(), "POSParser getTags called");
        String[] tokens = SimpleTokenizer.INSTANCE.tokenize(str);
        Log.e(getClass().getSimpleName(), "Tokens printed");
        try {
            for (String s : tokens) {
                Log.e(getClass().getSimpleName(), s);
            }
            if (tokens != null && posTagger != null) {
                return posTagger.tag(tokens);
            }
        } catch (NullPointerException n) {
            n.printStackTrace();
        }
        Log.e(getClass().getSimpleName(), "getTags: Reached null");
        return null;
    }

    /**
     *  Get verb Tags for tokenized array
     *
     */
    public String[] collectAllVerbTags(String []tokens) {
        Log.e(getClass().getSimpleName(), "POSParser collectAllVerbTags called");
        Log.e(getClass().getSimpleName(), "tokens printed");

        if (tokens != null && tokens.length >= 1) {
            for (String s : tokens) {
                Log.e(getClass().getSimpleName(), s);
            }

            ArrayList<String> tags = new ArrayList<>();
            for (String s : tokens) {
                if (s.startsWith("V")) {
                    tags.add(s);
                }
            }
            String[] verbsList = new String[tags.size()];
            verbsList = tags.toArray(verbsList);
            return verbsList;
        }
        Log.e(getClass().getSimpleName(), "collectAllVerbTags: Reached null");
        return null;
    }

    public static POSModel getPOSModel(URL name) {
        try {
            return new POSModel(new DataInputStream(name.openStream()));
        } catch (IOException E) {
            E.printStackTrace();
            throw new RuntimeException("com.ubiqlog.query.query.OpenNLP Tokenizer can not be initialized!", E);
        }
    }
}
