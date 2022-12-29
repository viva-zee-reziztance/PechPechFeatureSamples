package com.example.pechpechfeaturesamples;

import static android.app.Activity.RESULT_OK;

import android.content.ClipData;
import android.content.ContentResolver;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;


import android.os.Environment;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link FileChooserFragment#newInstance} factory method to
 * create an instance of this fragment.
 */

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;

import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class FileChooserFragment extends Fragment {

    private static final int MY_REQUEST_CODE_PERMISSION = 1000;
    private static final int MY_RESULT_CODE_FILECHOOSER = 2000;

    private Button buttonBrowse;
    private EditText editTextPath;

    // Are we selecing video or audio (for test only)
    public boolean selectingVideo = true;


    private static final String LOG_TAG = "AndroidExample";

    public FileSelect delegate = null;

    private static final int REQUEST_CODE_PICK = 1;
    private static final int REQUEST_CODE_PICK_AUDIO = 5;

    // ***************

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_file_chooser, container, false);

        this.editTextPath = (EditText) rootView.findViewById(R.id.editText_path);
        this.buttonBrowse = (Button) rootView.findViewById(R.id.button_browse);

        this.buttonBrowse.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {

                if (selectingVideo)
                {
                    Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
                            .setDataAndType(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, "video/*")
                            .putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
                    startActivityForResult(intent, REQUEST_CODE_PICK);

                }
                else
                {
                    /*
                    // https://stackoverflow.com/questions/23777501/intent-action-pick-crashed-with-type-audio
                    Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Audio.Media.EXTERNAL_CONTENT_URI)
                            .setDataAndType(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, "audio/*")
                            .putExtra(Intent.EXTRA_ALLOW_MULTIPLE, false);
                    startActivityForResult(intent, REQUEST_CODE_PICK);
                    */

                    // This worked but returned uri doesnt work for soundpool
                    // interesting ... doesnt android support Action.pick for audio?
                    Intent intent_upload = new Intent();
                    intent_upload.setType("audio/*");


                    intent_upload.setAction(Intent.ACTION_GET_CONTENT);


                    //intent_upload.setAction(Intent.ACTION_PICK);
                    startActivityForResult(intent_upload,REQUEST_CODE_PICK);
                }
            }

        });
        return rootView;
    }


    // ***************
    @Override
    public void onActivityResult(int requestCode, int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_PICK
                && resultCode == RESULT_OK
                && data != null) {

            if (selectingVideo)
            {
                if (data.getClipData() != null) {
                    ClipData clipData = data.getClipData();
                    List<Uri> uris = new ArrayList<>();
                    for (int i = 0; i < clipData.getItemCount(); i++) {
                        uris.add(clipData.getItemAt(i).getUri());
                    }


                    delegate.selectedFiles(uris.toArray(new Uri[0]));
                    // transcode(uris.toArray(new Uri[0]));
                } else if (data.getData() != null) {
                    // transcode(data.getData());
                    delegate.selectedFiles(data.getData());
                }

            }
            else
            {
                // It seems selecting audio is somehow different
                // and soundpool doesnt like the uri returned from uri.getPath(
                // ContentResolver contentResolver = delegate.getContentResolver();
                String path = FileHelper.getRealPathFromURI(getContext(), data.getData());
                delegate.selectedFile( path );
            }


        }

        /*
        if (requestCode == REQUEST_CODE_PICK_AUDIO
                && resultCode == RESULT_OK
                && data != null
                && data.getData() != null) {
            mAudioReplacementUri = data.getData();
            mAudioReplaceView.setText(mAudioReplacementUri.toString());

        }
        */

    }
}