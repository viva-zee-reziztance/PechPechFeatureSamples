package com.example.pechpechfeaturesamples;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;



// ***************
// Notes:
//  minsdk = 24 is enforced by mobile-ffmpeg in build.gradle
//  Examples of mobile-ffmpeg usage: // See examples https://github.com/tanersener/mobile-ffmpeg
//  Example of file opener: https://o7planning.org/12725/create-a-simple-file-chooser-in-android
//  The following files are changed & marked:
//      1)  build.gradle
//      2)  AndroidManifest.xml
// ***************

public class CompressionSample extends AppCompatActivity {

    private FileChooserFragment fragment;
    private Button buttonShowInfo;

    // ***************
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_compression_sample);

        // Add the file browser to the view
        FragmentManager fragmentManager = this.getSupportFragmentManager();
        this.fragment = (FileChooserFragment) fragmentManager.findFragmentById(R.id.fragment_fileChooser);

        this.buttonShowInfo = this.findViewById(R.id.button_fileChooser_showInfo);

        this.buttonShowInfo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showInfo();
            }
        });
    }

    // ***************
    private void showInfo()  {
        String path = this.fragment.getPath();
        Toast.makeText(this, "Path: " + path, Toast.LENGTH_LONG).show();
    }
}