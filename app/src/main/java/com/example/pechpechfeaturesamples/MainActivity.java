package com.example.pechpechfeaturesamples;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;



public class MainActivity extends AppCompatActivity {

    //  *******************
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Check if opencv is loaded
        /*
        if (OpenCVLoader.initDebug())
        {
            Log.d("Opencv", "OpenCV initiated");
        }
        else
        {
            Log.w("Opencv", "OpenCV NOT initiated ... maybe even quit or show some error?");
        }
        */


        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    //  *******************
    public void selectFeatureSampleRowClick(View view) {
        int selectedID = view.getId();
        switch(selectedID) {
            case R.id.compression:
                // Open the compression Sample
                startActivity(new Intent(this, VideoCompressionSample.class));
                break;
            case R.id.voice_change:
                //startActivity(new Intent(this, com.example.pechpechfeaturesamples.VoiceChangeSample.class));
                startActivity(new Intent(this, VoiceChangeSample.class));
                break;

        }
    }
}