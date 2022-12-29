package com.example.pechpechfeaturesamples;

import android.media.AudioManager;
import android.media.SoundPool;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;

import java.io.File;



/*

Stock footage provided downloaded from
https://pixabay.com/sound-effects/search/talk/
 */

//  I would love to use https://developer.android.com/reference/android/media/SoundPool
//  But I have concerns as
//      ... Soundpool sounds are expected to be short as they are predecoded into memory. Each decoded sound is internally limited to one megabyte storage, which represents approximately 5.6 seconds at
//      44.1kHz stereo (the duration is proportionally longer at lower sample rates or a channel mask of mono). A decoded audio sound will be truncated if it would exceed the per-sound one megabyte storage space.

//  https://stackoverflow.com/questions/13903318/android-recorded-voice-morphing-or-manipulation-to-funny-voices
public class VoiceChangeSample extends AppCompatActivity implements FileSelect
{
    // ***************
    private FileChooserFragment fragment = null;
    SoundPool soundPool = null;
    private static final int MAX_SOUNDPOOL_STREAMS = 10;
    // ***************
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_voice_change_sample);

        // Add the file browser to the view
        FragmentManager fragmentManager = this.getSupportFragmentManager();
        this.fragment = (FileChooserFragment) fragmentManager.findFragmentById(R.id.fragment_voice_change_sample_file_chooser);

        // We are going to select audio files
        this.fragment.selectingVideo = false;
        this.fragment.delegate = this;

        // this.fragment.delegate = this;
        // vu.delegate = this;

    }

    // ***************

    public void selectedFile(String audioPath)
    {
        //  https://stackoverflow.com/questions/27406048/soundpool-not-play-a-mp3

        soundPool = new SoundPool(MAX_SOUNDPOOL_STREAMS, AudioManager.STREAM_MUSIC, 0);
        boolean isSoundLoaded = false;
        float frequencyPitch = 0.8f; // tweak this. it accepts any number between 0.5f and 2.0f

        /*
        File temp = new File(audioPath);
        if (!temp.exists())
        {
            Log.w("", "wts");
        }
        */


        int soundID = soundPool.load(audioPath, 1);

        int loop = 3;


        soundPool.setOnLoadCompleteListener(new SoundPool.OnLoadCompleteListener() {
            @Override
            public void onLoadComplete(SoundPool soundPool, int sampleId, int status) {
                // https://developer.android.com/reference/android/media/SoundPool.OnLoadCompleteListener
                //  0 == success
                if (status == 0)
                {
                    // https://iopscience.iop.org/article/10.1088/1742-6596/1049/1/012082/pdf
                    soundPool.play(soundID, 1f, 1f, 1, loop, frequencyPitch);
                }
                else
                {
                    // TODO Show some error
                }


                // streamId = soundPool.play(soundId, 1, 1, 1, 3, pitch);
                // soundPool.setLoop(streamId, -1);
                // Log.e("TAG", String.valueOf(streamId));
            }
        });

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
    //  https://stackoverflow.com/questions/13903318/android-recorded-voice-morphing-or-manipulation-to-funny-voices
    //  or http://androidtrainningcenter.blogspot.com/2013/07/sound-pool-tutorial-in-android-changing.html

    public void selectedFiles(@NonNull Uri... uris)
    {



    }


}
