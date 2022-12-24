package com.example.pechpechfeaturesamples;



import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMetadataRetriever;
import android.media.MediaMuxer;
import android.net.Uri;
import android.util.Log;
import android.view.Surface;

import com.otaliastudios.transcoder.Transcoder;
import com.otaliastudios.transcoder.TranscoderListener;
import com.otaliastudios.transcoder.TranscoderOptions;
import com.otaliastudios.transcoder.common.TrackStatus;
import com.otaliastudios.transcoder.common.TrackType;
import com.otaliastudios.transcoder.internal.utils.Logger;
import com.otaliastudios.transcoder.source.DataSource;
import com.otaliastudios.transcoder.source.TrimDataSource;
import com.otaliastudios.transcoder.source.UriDataSource;
import com.otaliastudios.transcoder.strategy.DefaultAudioStrategy;
import com.otaliastudios.transcoder.strategy.DefaultVideoStrategy;
import com.otaliastudios.transcoder.strategy.RemoveTrackStrategy;
import com.otaliastudios.transcoder.strategy.TrackStrategy;
import com.otaliastudios.transcoder.resize.AspectRatioResizer;
import com.otaliastudios.transcoder.resize.FractionResizer;
import com.otaliastudios.transcoder.resize.PassThroughResizer;
import com.otaliastudios.transcoder.validator.DefaultValidator;

// It seems ffmpeg & javacv are mutually exclusive
/*
import com.arthenica.mobileffmpeg.Config;
import com.arthenica.mobileffmpeg.FFmpeg;
import com.arthenica.mobileffmpeg.FFprobe;
import com.arthenica.mobileffmpeg.MediaInformation;
import static com.arthenica.mobileffmpeg.Config.RETURN_CODE_CANCEL;
import static com.arthenica.mobileffmpeg.Config.RETURN_CODE_SUCCESS;

// Some random samles here https://www.tabnine.com/code/java/methods/org.bytedeco.javacv.FFmpegFrameGrabber/start
// Also javacv documentation http://bytedeco.org/javacv/apidocs/
import org.bytedeco.javacv.FrameGrabber;
import org.bytedeco.javacv.OpenCVFrameGrabber;

*/


import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

// https://docs.opencv.org/3.4/javadoc/org/opencv/videoio/VideoWriter.html
/*
import org.opencv.video.Video;
import org.opencv.videoio.VideoWriter;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.Videoio;
*/


// Some interesting examples using call backs in https://developer.android.com/reference/android/media/MediaCodec#asynchronous-processing-using-buffers


// There are some other examples in https://android.googlesource.com/platform/cts/+/refs/heads/android10-release/tests/tests/media/src/android/media/cts/EncodeDecodeTest.java

//  Again trying ideas from https://github.com/hoolrory/AndroidVideoSamples/blob/master/CommonVideoLibrary/src/com/roryhool/commonvideolibrary/VideoResampler.java
//  Also some ideas on the highlevel of stuff from https://www.youtube.com/watch?v=nIhkNE6B2k0&ab_channel=Touchlab

// Failed at least in

public class VideoUtilsMediaCodec
{
    static String LOG_TAG = "VideoUtilsMediaCodec";

    // I think saw it somethere 3500 kbps is ok for 720p
    static int DEFAULT_VIDEO_BITRATE =  1024 * 3500;

    private static final int TIMEOUT_USEC = 10000;

    static int DEFAULT_IFRAME_INTERVAL_SEC = 10;

    // 720p is 1280x720 pix ... just saying
    static int WIDTH_720P = 1280;
    static int HEIGHT_720P = 720;

    MediaCodec encoder = null;
    MediaCodec decoder = null;


    // This didnt work
    // private OutputSurface outputSurface = null;
    // private InputSurface inputSurface = null;

    // The surface to connect encoder/decoder
    //  https://stackoverflow.com/questions/29773326/is-it-possible-how-to-feed-mediacodec-decoded-frames-to-mediacodec-encoder-direc
    private Surface surface = null;

    MediaMuxer muxer = null;

    // Corresponding to mTrackIndex
    int trackIndex = -1;


    MediaFormat mOutputFormat = null;


    // ***************
    public static int getDuration(String path)
    {
        Uri uri = Uri.fromFile(new File(path));
        return MediaHelper.GetDuration(uri);
    }

    // ***************
    //  Gets the video bit rate for an existing file
    //  NO CHECKING WILL BE DONE ON THE PATH. IT IS YOUR RESPONSIBILITY
    //  To avoid multiple allocation of mediametadataretriever we send it as the param
    public static int getBitRate(String path) {

        // This absolutely works

        /*
        MediaMetadataRetriever metadataRetriever = new MediaMetadataRetriever();

        //  http://www.java2s.com/example/java-api/android/media/mediametadataretriever/metadata_key_bitrate-0.html
        metadataRetriever.setDataSource(path);
        String tmp = metadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE);
        return Integer.parseInt(tmp);
        */

        Uri uri = Uri.fromFile(new File(path));
        return MediaHelper.GetBitRate(uri);

        // https://github.com/hoolrory/AndroidVideoSamples/blob/master/CommonVideoLibrary/src/com/roryhool/commonvideolibrary/VideoResampler.java
        //  https://github.com/hoolrory/AndroidVideoSamples/blob/master/VideoManipulation/src/com/roryhool/videomanipulation/ResampleActivity.java
        //  https://android.googlesource.com/platform/cts/+/jb-mr2-release/tests/tests/media/src/android/media/cts/DecodeEditEncodeTest.java
        //  https://stackoverflow.com/questions/15950610/video-compression-on-android-using-new-mediacodec-library
        // >>> https://stackoverflow.com/questions/22076742/android-mediacodec-reduce-mp4-video-size

    }

    // ***************
    //  Magically (or based on some policy) tells you, whether you should re-encode some videos
    //  I think I saw somewhere that for instance Whatsapp do NOT reencode if the video size < 16MB
    //  Assumes THE FILE EXISTS. No checking is done on the path
    public static Boolean shouldReEncode(String inputPath)
    {
        // You see some samples here https://restream.io/blog/what-is-video-bitrate/
        int br = VideoUtilsMediaCodec.getBitRate(inputPath);
        // Very naive (3500kbs) for target framerate of HD 720p, 60fps
        return (br > VideoUtilsMediaCodec.DEFAULT_VIDEO_BITRATE);
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
    //  Given a path to a file, we feed data to our encoder to re-encode data to a lower but acceptable
    //  quality
    //  Also an interesting point https://stackoverflow.com/questions/25901345/mediacodec-android-how-to-do-decoding-encoding-from-buffer-to-buffer
    /*
        So you're trying to decode data to a ByteBuffer and then pass that to the encoder?
        That only works on certain devices; many will not accept their own output as input. This is why DecodeEditEncode uses a Surface.
     */

    private void startFeedingEncoderSurface(String inputPath)
    {


        // Create a decoder/extractor
        MediaExtractor extractor = new MediaExtractor();
        try {
            extractor.setDataSource( inputPath );
        } catch ( IOException e ) {
            e.printStackTrace();

            // TODO show an error?
            return;
        }

        int trackIndex = getVideoTrackIndex(extractor);
        extractor.selectTrack( trackIndex );

        MediaFormat clipFormat = extractor.getTrackFormat( trackIndex );


        // I dont know what it is ... but i leave it since i might revisit/require it later

        // if ( clip.getStartTime() != -1 ) {
        //    extractor.seekTo( clip.getStartTime() * 1000, MediaExtractor.SEEK_TO_PREVIOUS_SYNC );
        //    clip.setStartTime( extractor.getSampleTime() / 1000 );
        //}

        try {
            decoder = MediaCodec.createDecoderByType( MediaHelper.MIME_TYPE_AVC );

            /*
            // Didnt work
            outputSurface = new OutputSurface();
            decoder.configure( clipFormat, outputSurface.getSurface(), null, 0 );
             */

            surface = encoder.createInputSurface();
            decoder.configure(clipFormat, surface, null, 0);

            decoder.start();

            resampleVideo( extractor, decoder, inputPath );

        }
        catch (IllegalStateException e)
        {
            Log.w(LOG_TAG, e.getMessage());
            Log.w(LOG_TAG, e.getLocalizedMessage());
        }

        catch (Exception e) {
            // TODO maybe report to the caller?
            Log.w(VideoUtilsMediaCodec.LOG_TAG, e.getMessage());
        }
        finally {

            /*
            if (outputSurface != null)
            {
                outputSurface.release();
            }
             */

            if (surface != null)
            {
                surface.release();
            }
            if ( decoder != null ) {
                decoder.stop();
                decoder.release();
            }

            if ( extractor != null ) {
                extractor.release();
                extractor = null;
            }
        }

    }

    // ***************
    private void setupMuxer(String outputPath) {

        try {
            muxer = new MediaMuxer( outputPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4 );
        } catch ( IOException ioe ) {
            throw new RuntimeException( "MediaMuxer creation failed", ioe );
        }
    }
    // ***************
    private void resampleVideo( MediaExtractor extractor,
                                MediaCodec decoder,
                                String inputPath) {
        ByteBuffer[] decoderInputBuffers = decoder.getInputBuffers();
        ByteBuffer[] encoderOutputBuffers = encoder.getOutputBuffers();
        MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
        int inputChunk = 0;
        int outputCount = 0;

        long endTime = getDuration(inputPath);

        /*
        long endTime = clip.getEndTime();

        if ( endTime == -1 ) {
            endTime = clip.getVideoDuration();
        }
        */


        boolean outputDoneNextTimeWeCheck = false;

        boolean outputDone = false;
        boolean inputDone = false;
        boolean decoderDone = false;

        while ( !outputDone ) {
            // Feed more data to the decoder.
            if ( !inputDone ) {
                int inputBufIndex = decoder.dequeueInputBuffer( TIMEOUT_USEC );
                if ( inputBufIndex >= 0 ) {
                    if ( extractor.getSampleTime() / 1000 >= endTime ) {
                        // End of stream -- send empty frame with EOS flag set.
                        decoder.queueInputBuffer( inputBufIndex, 0, 0, 0L, MediaCodec.BUFFER_FLAG_END_OF_STREAM );
                        inputDone = true;
                    } else {
                        // Copy a chunk of input to the decoder. The first chunk should have
                        // the BUFFER_FLAG_CODEC_CONFIG flag set.
                        ByteBuffer inputBuf = decoderInputBuffers[inputBufIndex];
                        inputBuf.clear();

                        int sampleSize = extractor.readSampleData( inputBuf, 0 );
                        if ( sampleSize < 0 ) {
                            Log.d( LOG_TAG, "InputBuffer BUFFER_FLAG_END_OF_STREAM" );
                            decoder.queueInputBuffer( inputBufIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM );
                        } else {
                            Log.d( LOG_TAG, "InputBuffer ADVANCING" );
                            decoder.queueInputBuffer( inputBufIndex, 0, sampleSize, extractor.getSampleTime(), 0 );
                            extractor.advance();
                        }

                        inputChunk++;
                    }
                } else {
                    Log.w( LOG_TAG, "input buffer not available" );
                }
            }

            // Assume output is available. Loop until both assumptions are false.
            boolean decoderOutputAvailable = !decoderDone;
            boolean encoderOutputAvailable = true;
            while ( decoderOutputAvailable || encoderOutputAvailable ) {
                // Start by draining any pending output from the encoder. It's important to
                // do this before we try to stuff any more data in.
                int encoderStatus = encoder.dequeueOutputBuffer( info, TIMEOUT_USEC );
                if ( encoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER ) {
                    // no output available yet
                    encoderOutputAvailable = false;
                } else if ( encoderStatus == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED ) {
                    encoderOutputBuffers = encoder.getOutputBuffers();
                } else if ( encoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED ) {

                    MediaFormat newFormat = encoder.getOutputFormat();

                    trackIndex = muxer.addTrack( newFormat );
                    muxer.start();
                } else if ( encoderStatus < 0 ) {
                    // fail( "unexpected result from encoder.dequeueOutputBuffer: " + encoderStatus );
                } else { // encoderStatus >= 0
                    ByteBuffer encodedData = encoderOutputBuffers[encoderStatus];
                    if ( encodedData == null ) {
                        // fail( "encoderOutputBuffer " + encoderStatus + " was null" );
                    }
                    // Write the data to the output "file".
                    if ( info.size != 0 ) {
                        encodedData.position( info.offset );
                        encodedData.limit( info.offset + info.size );
                        outputCount++;

                        muxer.writeSampleData( trackIndex, encodedData, info );

                        Log.d( LOG_TAG, "encoder output " + info.size + " bytes" );
                    }
                    outputDone = ( info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM ) != 0;

                    encoder.releaseOutputBuffer( encoderStatus, false );
                }

                if ( outputDoneNextTimeWeCheck ) {
                    outputDone = true;
                }

                if ( encoderStatus != MediaCodec.INFO_TRY_AGAIN_LATER ) {
                    // Continue attempts to drain output.
                    continue;
                }
                // Encoder is drained, check to see if we've got a new frame of output from
                // the decoder. (The output is going to a Surface, rather than a ByteBuffer,
                // but we still get information through BufferInfo.)
                if ( !decoderDone ) {
                    int decoderStatus = decoder.dequeueOutputBuffer( info, TIMEOUT_USEC );
                    if ( decoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER ) {
                        // no output available yet
                        Log.w( LOG_TAG, "no output from decoder available" );
                        decoderOutputAvailable = false;
                    } else if ( decoderStatus == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED ) {
                        // decoderOutputBuffers = decoder.getOutputBuffers();
                        Log.d( LOG_TAG, "decoder output buffers changed (we don't care)" );
                    } else if ( decoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED ) {
                        // expected before first buffer of data
                        MediaFormat newFormat = decoder.getOutputFormat();
                        Log.d( LOG_TAG, "decoder output format changed: " + newFormat );
                    } else if ( decoderStatus < 0 ) {
                        // fail( "unexpected result from decoder.dequeueOutputBuffer: " + decoderStatus );
                    } else { // decoderStatus >= 0
                        Log.d( LOG_TAG, "surface decoder given buffer " + decoderStatus + " (size=" + info.size + ")" );
                        // The ByteBuffers are null references, but we still get a nonzero
                        // size for the decoded data.
                        boolean doRender = ( info.size != 0 );
                        // As soon as we call releaseOutputBuffer, the buffer will be forwarded
                        // to SurfaceTexture to convert to a texture. The API doesn't
                        // guarantee that the texture will be available before the call
                        // returns, so we need to wait for the onFrameAvailable callback to
                        // fire. If we don't wait, we risk rendering from the previous frame.
                        decoder.releaseOutputBuffer( decoderStatus, doRender );
                        /*
                        if ( doRender ) {
                            // This waits for the image and renders it after it arrives.
                            Log.d( LOG_TAG, "awaiting frame" );
                            outputSurface.awaitNewImage();
                            outputSurface.drawImage();
                            // Send it to the encoder.

                            long nSecs = info.presentationTimeUs * 1000;

                            if ( clip.getStartTime() != -1 ) {
                                nSecs = ( info.presentationTimeUs - ( clip.getStartTime() * 1000 ) ) * 1000;
                            }

                            Log.d( "this", "Setting presentation time " + nSecs / ( 1000 * 1000 ) );
                            nSecs = Math.max( 0, nSecs );

                            mEncoderPresentationTimeUs += ( nSecs - mLastSampleTime );

                            mLastSampleTime = nSecs;

                            mInputSurface.setPresentationTime( mEncoderPresentationTimeUs );
                            if ( VERBOSE )
                                Log.d( TAG, "swapBuffers" );
                            mInputSurface.swapBuffers();
                        }
                         */
                        if ( ( info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM ) != 0 ) {
                            // mEncoder.signalEndOfInputStream();
                            outputDoneNextTimeWeCheck = true;
                        }
                    }
                }
            }
        }
        if ( inputChunk != outputCount ) {
            // throw new RuntimeException( "frame lost: " + inputChunk + " in, " + outputCount + " out" );
        }
    }

    // ***************
    private void releaseOutputResources() {

        /*
        if ( inputSurface != null ) {
            inputSurface.release();
        }
         */

        // should I??? why the base code had two releases and called in different places?
        if (surface != null)
        {
            surface.release();
        }

        if ( encoder != null ) {
            encoder.stop();
            encoder.release();
        }

        /*
        if ( mMuxer != null ) {
            mMuxer.stop();
            mMuxer.release();
            mMuxer = null;
        }
         */
    }
    // ***************
    //  default overload
    public void reEncodeVideo(String inputPath, String outputPath)
    {
        MediaMetadataRetriever metadataRetriever = new MediaMetadataRetriever();
        metadataRetriever.setDataSource(inputPath);

        // Extract the rest of stuff
        int frameRate = VideoUtilsMediaCodec.getFrameRate(inputPath);

        try
        {
            reEncodeVideo(inputPath, outputPath, frameRate, VideoUtilsMediaCodec.DEFAULT_VIDEO_BITRATE,
                    VideoUtilsMediaCodec.WIDTH_720P, VideoUtilsMediaCodec.HEIGHT_720P);
        }
        catch  (Exception e)
        {
            Log.w(VideoUtilsMediaCodec.LOG_TAG, e.getMessage());
            //Todo maybe show an error?
        }

    }

    // ***************
    private void setupEncoder(int frameRate,
                              int bitRate,
                              int width,
                              int height) throws IOException
    {
        //  Some examples https://github.com/PhilLab/Android-MediaCodec-Examples/blob/master/EncodeDecodeTest.java
        //  https://developer.android.com/reference/android/media/MediaCodec

        MediaFormat outputFormat = MediaFormat.createVideoFormat( MediaHelper.MIME_TYPE_AVC, width, height );
        outputFormat.setInteger( MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface );
        outputFormat.setInteger( MediaFormat.KEY_BIT_RATE, bitRate );

        outputFormat.setInteger( MediaFormat.KEY_FRAME_RATE, frameRate );

        outputFormat.setInteger( MediaFormat.KEY_I_FRAME_INTERVAL, VideoUtilsMediaCodec.DEFAULT_IFRAME_INTERVAL_SEC );

        // Here we might throw
        encoder = MediaCodec.createEncoderByType( MediaHelper.MIME_TYPE_AVC );

        encoder.configure( outputFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE );
        encoder.start();
        Log.i(VideoUtilsMediaCodec.LOG_TAG, "Started encoder ...");
    }

    // ***************
    //  Testing ideas from https://github.com/hoolrory/AndroidVideoSamples/blob/master/CommonVideoLibrary/src/com/roryhool/commonvideolibrary/VideoResampler.java
    //  Might throw exception
    //  Something like resampleVideo() in https://github.com/hoolrory/AndroidVideoSamples/blob/71d2c49bd7dc7ae2cd5169a600fe7577ea8c4d77/CommonVideoLibrary/src/com/roryhool/commonvideolibrary/VideoResampler.java#L193
    public void reEncodeVideoSurface(          String inputPath,
                                               String outputPath,
                                               int frameRate,
                                               int bitRate,
                                               int width,
                                               int height) throws IOException {

        setupEncoder(frameRate, bitRate, width, height);
        //setupEncoder(frameRate, bitRate, 480, 320);

        Log.w(LOG_TAG, "NOTE TODO OK CHANGE HERE ASAP");

        setupMuxer(outputPath);

        // Now we need to feed to
        startFeedingEncoderSurface(inputPath);

        // Send end of input
        encoder.signalEndOfInputStream();

        // cleanup
        releaseOutputResources();

    }

    // ***************
    // https://developer.android.com/reference/android/media/MediaCodec#asynchronous-processing-using-buffers
    private void setupDecoderBuffer(String inputPath)
    {
        MediaExtractor extractor = new MediaExtractor();
        try {
            extractor.setDataSource( inputPath );
        } catch ( IOException e ) {
            e.printStackTrace();

            // TODO show an error?
            return;
        }

        int trackIndex = getVideoTrackIndex(extractor);
        extractor.selectTrack( trackIndex );

        MediaFormat clipFormat = extractor.getTrackFormat( trackIndex );

        try {
            decoder = MediaCodec.createDecoderByType( MediaHelper.MIME_TYPE_AVC );

            decoder.setCallback(new MediaCodec.Callback() {
                // https://developer.android.com/reference/android/media/MediaCodec.Callback
                @Override
                public void onInputBufferAvailable(MediaCodec mc, int inputBufferId)
                {
                    ByteBuffer inputBuffer = decoder.getInputBuffer(inputBufferId);
                    // fill inputBuffer with valid data

                    //codec.queueInputBuffer(inputBufferId, …);
                    Log.i(LOG_TAG, "input buffer avai");
                }

                @Override
                public void onOutputBufferAvailable(MediaCodec codec, int index, MediaCodec.BufferInfo info)
                {
                    // ByteBuffer outputBuffer = decoder.getOutputBuffer(outputBufferId);
                    // MediaFormat bufferFormat = decoder.getOutputFormat(outputBufferId); // option A

                    // bufferFormat is equivalent to mOutputFormat
                    // outputBuffer is ready to be processed or rendered.
                    // decoder.releaseOutputBuffer(outputBufferId, …);
                    Log.i(LOG_TAG, "output buffer avai");
                }

                @Override
                public void onOutputFormatChanged(MediaCodec codec, MediaFormat format)
                {
                    // Subsequent data will conform to new format.
                    // Can ignore if using getOutputFormat(outputBufferId)
                    mOutputFormat = format; // option B
                }

                @Override
                public void onError(MediaCodec codec, MediaCodec.CodecException e){
                    Log.w(LOG_TAG, e.getMessage());
                }
            });


            decoder.configure(clipFormat, surface, null, 0);

            decoder.start();
            // resampleVideo( extractor, decoder, inputPath );

        }
        catch (IllegalStateException e)
        {
            Log.w(LOG_TAG, e.getMessage());
            Log.w(LOG_TAG, e.getLocalizedMessage());
        }

        catch (Exception e) {
            // TODO maybe report to the caller?
            Log.w(VideoUtilsMediaCodec.LOG_TAG, e.getMessage());
        }
        finally {
            decoder.stop();
            decoder.release();
        }
    }

    // ***************
    //  Testing ideas from https://github.com/hoolrory/AndroidVideoSamples/blob/master/CommonVideoLibrary/src/com/roryhool/commonvideolibrary/VideoResampler.java
    //  Might throw exception
    //  Something like resampleVideo() in https://github.com/hoolrory/AndroidVideoSamples/blob/71d2c49bd7dc7ae2cd5169a600fe7577ea8c4d77/CommonVideoLibrary/src/com/roryhool/commonvideolibrary/VideoResampler.java#L193
    public void reEncodeVideo(          String inputPath,
                                        String outputPath,
                                        int frameRate,
                                        int bitRate,
                                        int width,
                                        int height) throws IOException {

        setupDecoderBuffer(inputPath);





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


