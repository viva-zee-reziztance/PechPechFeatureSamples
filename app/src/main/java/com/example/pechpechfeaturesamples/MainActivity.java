package com.example.pechpechfeaturesamples;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

public class MainActivity extends AppCompatActivity {

    //  *******************
    @Override
    protected void onCreate(Bundle savedInstanceState) {
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