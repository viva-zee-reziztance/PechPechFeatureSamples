package com.example.pechpechfeaturesamples;

import android.content.ContentResolver;
import android.net.Uri;

import androidx.annotation.NonNull;

//  Interface that we have selected a file
interface FileSelect
{
    // Selected a file with the given path
    public void selectedFile(String path);
    public void selectedFiles(@NonNull Uri... uris);
    public void cancelFileSelection();
    public void permissionDenied();
    public void errorSelectingFile(Exception e);
    // public ContentResolver getContentResolver();
};
