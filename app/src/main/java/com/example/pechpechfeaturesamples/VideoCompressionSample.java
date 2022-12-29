package com.example.pechpechfeaturesamples;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.net.Uri;
import android.os.Bundle;

import androidx.fragment.app.FragmentManager;

import android.os.Environment;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import com.otaliastudios.transcoder.TranscoderListener;

import java.io.File;
import java.io.IOException;


// ***************
// Notes:
//  minsdk = 24 is enforced by mobile-ffmpeg in build.gradle
//  Examples of mobile-ffmpeg usage: // See examples https://github.com/tanersener/mobile-ffmpeg
//  Example of file opener: https://o7planning.org/12725/create-a-simple-file-chooser-in-android
//  The following files are changed & marked:
//      1)  build.gradle
//      2)  AndroidManifest.xml
// ***************


public class VideoCompressionSample extends AppCompatActivity implements FileSelect, TranscoderListener {

    private FileChooserFragment fragment = null;
    private Button buttonShowInfo;
    private VideoUtils vu = new VideoUtils();

    // ***************
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_compression_sample);

        // Add the file browser to the view
        FragmentManager fragmentManager = this.getSupportFragmentManager();
        this.fragment = (FileChooserFragment) fragmentManager.findFragmentById(R.id.fragment_vide_compression_file_chooser);
        this.fragment.delegate = this;

        vu.delegate = this;

        /*
        this.buttonShowInfo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showInfo();
            }
        });
         */
    }

    // ***************
    /*
    private void showInfo()  {
        String path = this.fragment.getPath();
        Toast.makeText(this, "Path: " + path, Toast.LENGTH_LONG).show();
    }

     */

    // ***************
    public void selectedFiles(@NonNull Uri... uris)
    {
        Log.i("", "ok here");
        compressVideo(uris);

    }
    // ***************

    public void selectedFile(String path)
    {
        /*
        String outputData = compressVideo(path);

        Integer sourceBitRate = VideoUtilsMediaCodec.getBitRate(path);
        Integer targetBitRate = VideoUtilsMediaCodec.getBitRate(outputData);

        Log.i("", "changed bitrate from " + Integer.toString(sourceBitRate) + ", to " + Integer.toString(targetBitRate) );

         */

    }

    // ***************
    public void cancelFileSelection()
    {

    }

    // ***************
    public void permissionDenied()
    {

    }

    // ***************
    public void errorSelectingFile(Exception e)
    {

    }


    // ***************
    private String compressVideo(@NonNull Uri... uris)
    {


        File downLoadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);



        /*
        Log.w("", "REMOVE ME");
        File outputFile = new File( downLoadDir, "1.mp4");
        if (outputFile.exists())
        {
            outputFile.delete();
        }

        try {
            outputFile.createNewFile();
        }
        catch (IOException e)
        {
            Log.w("", "Could not create the file");
        }
        */
        File outputFile = null;

        try {
            outputFile = File.createTempFile("output", ".mp4");
        }
        catch (IOException e)
        {
            Log.w("", e.getMessage());
            return "";
        }



        // String outputPath = file.getPath();
        // vu.delegate = this;

        vu.transcode(getApplicationContext(), outputFile, uris);


        return outputFile.getAbsolutePath();
    }

    // ***************
    //  Return the output file path (the same if the input file bitrate is already low)
    // ***************
    //  Deprected funcs. work on it later
    /*
    private String compressVideo(String inputPath)
    {
        if (VideoUtilsMediaCodec.shouldReEncode(inputPath))
        {
            // Re-encode then!
            VideoUtils vu = new VideoUtils();


            File downLoadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);



            // String uuid = UUID.randomUUID().toString();
            // File file = new File( downLoadDir, uuid+".mp4");
            // String outputPath = file.getPath();

            Log.w("", "REMOVE ME");
            File file = new File( downLoadDir, "1.mp4");
            if (file.exists())
            {
                file.delete();
            }
            String outputPath = file.getPath();

            vu.delegate = this;


            vu.reEncodeVideo(inputPath, outputPath, getApplicationContext());



            return outputPath;
        }
        else
        {
            return inputPath;
        }




    }
     */

    @Override
    public void onTranscodeProgress(double progress) {
        Log.i("", "Progressed to " + String.valueOf(progress));
    }

    @Override
    public void onTranscodeCompleted(int successCode) {
        Toast.makeText(this, "Completed", Toast.LENGTH_LONG);
    }

    @Override
    public void onTranscodeCanceled() {
        Log.w("", "Cancelled");
    }

    @Override
    public void onTranscodeFailed(@NonNull Throwable exception) {
        Log.w("", "Failed with error = " + exception.getMessage());
    }
}