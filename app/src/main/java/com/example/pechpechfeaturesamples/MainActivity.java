package com.example.pechpechfeaturesamples;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
//import org.opencv.android.*;


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
        switch(view.getId()) {
            case R.id.compression:
                // Open the compression Sample
                startActivity(new Intent(this, CompressionSample.class));
                break;
        }
    }
}