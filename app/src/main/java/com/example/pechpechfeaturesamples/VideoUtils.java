package com.example.pechpechfeaturesamples;

import android.content.Context;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMetadataRetriever;
import android.media.MediaMuxer;
import android.net.Uri;
import android.util.Log;
import android.view.Surface;

import androidx.annotation.NonNull;

import com.example.pechpechfeaturesamples.MediaHelper;
import com.otaliastudios.transcoder.Transcoder;
import com.otaliastudios.transcoder.TranscoderListener;
import com.otaliastudios.transcoder.TranscoderOptions;
import com.otaliastudios.transcoder.common.TrackStatus;
import com.otaliastudios.transcoder.resize.AspectRatioResizer;
import com.otaliastudios.transcoder.resize.FractionResizer;
import com.otaliastudios.transcoder.resize.PassThroughResizer;
import com.otaliastudios.transcoder.source.DataSource;
import com.otaliastudios.transcoder.source.TrimDataSource;
import com.otaliastudios.transcoder.source.UriDataSource;
import com.otaliastudios.transcoder.strategy.DefaultAudioStrategy;
import com.otaliastudios.transcoder.strategy.DefaultVideoStrategy;
import com.otaliastudios.transcoder.strategy.RemoveTrackStrategy;
import com.otaliastudios.transcoder.strategy.TrackStrategy;
import com.otaliastudios.transcoder.validator.DefaultValidator;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;

import kotlin.collections.ArraysKt;
import kotlin.collections.ArraysKt;

public class VideoUtils
{
    static String LOG_TAG = "VideoUtils";

    // I think saw it somethere 3500 kbps is ok for 720p
    static int DEFAULT_VIDEO_BITRATE =  1024 * 3500;
    static int MAX_VIDEO_FRAMERATE = 30;

    private static final int TIMEOUT_USEC = 10000;

    static int DEFAULT_IFRAME_INTERVAL_SEC = 10;

    // 720p is 1280x720 pix ... just saying
    static int WIDTH_720P = 1280;
    static int HEIGHT_720P = 720;


    public TranscoderListener delegate = null;


    private Future<Void> mTranscodeFuture;

    private TrackStrategy mTranscodeVideoStrategy = null;
    private TrackStrategy mTranscodeAudioStrategy = null;

    private boolean mIsAudioOnly;

    // ***************
    //  Magically (or based on some policy) tells you, whether you should re-encode some videos
    //  I think I saw somewhere that for instance Whatsapp do NOT reencode if the video size < 16MB
    //  Assumes THE FILE EXISTS. No checking is done on the path
    public static Boolean shouldReEncode(String inputPath)
    {
        // You see some samples here https://restream.io/blog/what-is-video-bitrate/
        Uri uri = Uri.fromFile(new File(inputPath));
        int br = MediaHelper.GetBitRate(uri);


        // Very naive (3500kbs) for target framerate of HD 720p, 60fps
        return (br > DEFAULT_VIDEO_BITRATE);
    }

    // ***************
    private int getVideoTrackIndex(MediaExtractor extractor) {

        for ( int trackIndex = 0; trackIndex < extractor.getTrackCount(); trackIndex++ ) {
            MediaFormat format = extractor.getTrackFormat( trackIndex );

            String mime = format.getString( MediaFormat.KEY_MIME );
            if ( mime != null ) {
                if ( mime.equals( "video/avc" ) ) {
                    return trackIndex;
                }
            }
        }

        return -1;
    }


    // ***************
    //  default overload
    public void reEncodeVideo(Uri inputPathUri, File outputFile, Context context)
    {

        String inputPathString = inputPathUri.getPath();
        // MediaMetadataRetriever metadataRetriever = new MediaMetadataRetriever();
        // metadataRetriever.setDataSource(inputPathString);

        // Extract the rest of stuff
        int frameRate = getFrameRate(inputPathString);


        try
        {
            reEncodeVideo(inputPathUri, outputFile, context, Math.min(MAX_VIDEO_FRAMERATE, frameRate), DEFAULT_VIDEO_BITRATE, WIDTH_720P, HEIGHT_720P);
        }
        catch  (Exception e)
        {
            Log.w(LOG_TAG, e.getMessage());
            //Todo maybe show an error?
        }

    }

    // ***************
    //  The constructor, get the context
    /*
    public VideoUtils()
    {
        //this.context = context;
    }
    */

    // ***************
    // https://github.com/natario1/Transcoder/blob/main/demo/src/main/java/com/otaliastudios/transcoder/demo/TranscoderActivity.java
    private void setAudioPolicy()
    {



        /*
            // This failed ... debug why? maybe sample rate?
            boolean removeAudio = false; // fails
            // Or 2 as the setereo?
            int channels = DefaultAudioStrategy.CHANNELS_AS_INPUT;


            int sampleRate = 48000;

            mTranscodeAudioStrategy = DefaultAudioStrategy.builder()
                    .channels(channels)
                    .sampleRate(sampleRate)
                    .build();

         */


        boolean removeAudio = true;
        if (removeAudio) {
            mTranscodeAudioStrategy = new RemoveTrackStrategy();
        } else {
            // Or 2 as the setereo?
            int channels = DefaultAudioStrategy.CHANNELS_AS_INPUT;


            int sampleRate = 48000;

            mTranscodeAudioStrategy = DefaultAudioStrategy.builder()
                    .channels(channels)
                    .sampleRate(sampleRate)
                    .build();
        }



        // mTranscodeAudioStrategy = DefaultAudioStrategy.builder().build();

    }

    // ***************
    private void setVideoPolicy()
    {
        mTranscodeVideoStrategy = new DefaultVideoStrategy.Builder().addResizer(new PassThroughResizer())
                .frameRate(DefaultVideoStrategy.DEFAULT_FRAME_RATE) //set the framerate
                .keyFrameInterval(DEFAULT_IFRAME_INTERVAL_SEC)
                .build();


        // mTranscodeVideoStrategy = new DefaultVideoStrategy.Builder().build();
    }


    // ***************
    //  outputFile must exist
    public void transcode(Context context, File outputFile, Uri... uris)
    {
        TranscoderOptions.Builder builder = Transcoder.into(outputFile.getAbsolutePath());


        // Adjust the video/audio policy here
        setVideoPolicy();
        setAudioPolicy();

        List<DataSource> sources = ArraysKt.map(uris, uri -> new UriDataSource(context, uri));


        // This is for the case if there multiple source ... just maybe if we wanted for instance do the montage?
        for (DataSource source : sources) {
            builder.addDataSource(source);
        }

        Log.d("", "Added source?");

        mTranscodeFuture = builder.setListener(delegate)
                .setAudioTrackStrategy(mTranscodeAudioStrategy)
                .setVideoTrackStrategy(mTranscodeVideoStrategy)
                //.setVideoRotation(rotation)
                /*.setValidator(new DefaultValidator() {
                    @Override
                    public boolean validate(@NonNull TrackStatus videoStatus, @NonNull TrackStatus audioStatus) {
                        //mIsAudioOnly = !videoStatus.isTranscoding();
                        return super.validate(videoStatus, audioStatus);
                    }
                })
                */

                //.setSpeed(speed)
                .transcode();
    }

    // ***************
    //  Testing ideas from https://github.com/natario1/Transcoder/blob/main/demo/src/main/java/com/otaliastudios/transcoder/demo/TranscoderActivity.java
    public void reEncodeVideo(          Uri inputPathUri,
                                        File outputFile, //Must be already exist
                                        Context context,
                                        int frameRate,
                                        int bitRate,
                                        int width,
                                        int height)  {




        TranscoderOptions.Builder builder = Transcoder.into(outputFile.getAbsolutePath());





        // Adjust the video/audio policy here
        setVideoPolicy();
        setAudioPolicy();

        // well also think about replacing audio here
        List<DataSource> sources = new ArrayList<DataSource>();
        sources.add(new UriDataSource(context, inputPathUri));

        sources.set(0, new TrimDataSource(sources.get(0), 0, 0));
        // Note still builder is not set
        /*
        if (mAudioReplacementUri == null) {
            for (DataSource source : sources) {
                builder.addDataSource(source);
            }
        } else {
            for (DataSource source : sources) {
                builder.addDataSource(TrackType.VIDEO, source);
            }
            builder.addDataSource(TrackType.AUDIO, this, mAudioReplacementUri);
        }
        LOG.e("Starti
         */

        // This is for the case if there multiple source ... just maybe if we wanted for instance do the montage?
        for (DataSource source : sources) {
            builder.addDataSource(source);
        }

        Log.d("", "Added source?");

        mTranscodeFuture = builder.setListener(delegate)
                .setAudioTrackStrategy(mTranscodeAudioStrategy)
                .setVideoTrackStrategy(mTranscodeVideoStrategy)
                //.setVideoRotation(rotation)
                .setValidator(new DefaultValidator() {
                    @Override
                    public boolean validate(@NonNull TrackStatus videoStatus, @NonNull TrackStatus audioStatus) {
                        mIsAudioOnly = !videoStatus.isTranscoding();
                        boolean res = super.validate(videoStatus, audioStatus);
                        return res;
                    }
                })
                //.setSpeed(speed)
                .transcode();
    }



    // ***************
    //  https://stackoverflow.com/questions/42204944/how-to-get-frame-rate-of-video-in-android-os
    //  TODO maybe another overload to pass the metadataretriever
    //  Returns a negative number if failed
    public  static int getFrameRate(String inputPath)
    {
        /*
        // NOTE: THIS DIDNT WORK
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        String frameRateStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CAPTURE_FRAMERATE);
        return Integer.parseInt(frameRateStr);
        */

        MediaExtractor extractor = new MediaExtractor();
        int frameRate = 24; //may be default
        try {
            //Adjust data source as per the requirement if file, URI, etc.
            extractor.setDataSource(inputPath);
            int numTracks = extractor.getTrackCount();
            for (int i = 0; i < numTracks; ++i) {
                MediaFormat format = extractor.getTrackFormat(i);
                String mime = format.getString(MediaFormat.KEY_MIME);
                if (mime.startsWith("video/")) {
                    if (format.containsKey(MediaFormat.KEY_FRAME_RATE)) {
                        frameRate = format.getInteger(MediaFormat.KEY_FRAME_RATE);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            frameRate = -1;
        }finally {
            //Release stuff
            extractor.release();
        }
        return  frameRate;
    }

}


