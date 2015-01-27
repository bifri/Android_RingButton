package com.ai.ringbutton;

import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.RelativeLayout;

public class MainActivity extends Activity {

    @SuppressWarnings("unused")
    private static final String TAG = "RingButton";
//    private static int RING_BUTTON_VIEW_ID = View.NO_ID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "Entered onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final RelativeLayout frame = (RelativeLayout) findViewById(R.id.frame);

        // Static initialization
        final RingButton ringButton =
                (RingButton) getLayoutInflater().inflate(
                        R.layout.ring_button, frame, false);

        // there are small lags with hardware acceleration turned on
        // when orientation changes
        // see: http://stackoverflow.com/questions/17761980/
        // strange-behaviour-of-drawtextonpath-with-hardware-accelration
        if (!frame.isInEditMode()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                frame.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
            }
        }
        if (!ringButton.isInEditMode()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                ringButton.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
            }
        }

        frame.addView(ringButton);

        // view does its own state management
        ringButton.setSaveEnabled(true);

        // Initialization programmatically
/*
        final RingButton ringButtonView =
                new RingButton(getApplicationContext());
        if (RING_BUTTON_VIEW_ID == View.NO_ID) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1) {
                RING_BUTTON_VIEW_ID = Utils.generateViewId();
                //ringButtonView.setId(Utils.generateViewId());
            } else {
                RING_BUTTON_VIEW_ID = View.generateViewId();
                // ringButtonView.setId(View.generateViewId());
            }
        }
        ringButtonView.setId(RING_BUTTON_VIEW_ID);
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT,
                RelativeLayout.LayoutParams.MATCH_PARENT);
//      ringButtonView.setExternalDiameter(600);
//      ringButtonView.setInternalDiameter(300);
        params.addRule(RelativeLayout.CENTER_IN_PARENT);
        ringButtonView.setLayoutParams(params);
        ringButtonView.setText("View initialized programmatically");
        ringButtonView.setRingBackgroundColor(0xAA3454FD);
        ringButtonView.setTextColor(0xAA56FF89);
        ringButtonView.setTextSize(20);
        frame.addView(ringButtonView);
*/
    }
}