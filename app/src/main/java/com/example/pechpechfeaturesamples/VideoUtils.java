package com.example.pechpechfeaturesamples;



import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.util.Log;

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

// https://docs.opencv.org/3.4/javadoc/org/opencv/videoio/VideoWriter.html
import org.opencv.video.Video;
import org.opencv.videoio.VideoWriter;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.Videoio;

//  Again trying ideas from https://github.com/hoolrory/AndroidVideoSamples/blob/master/CommonVideoLibrary/src/com/roryhool/commonvideolibrary/VideoResampler.java
public class VideoUtils
{
    static String LOG_TAG = "VideoUtils";

    // I think saw it somethere 3500 kbps is ok for 720p
    static int DEFAULT_VIDEO_BITRATE =  1024 * 3500;

    static int DEFAULT_IFRAME_INTERVAL_SEC = 10;

    // 720p is 1280x720 pix ... just saying
    static int WIDTH_720P = 1280;
    static int HEIGHT_720P = 720;

    MediaCodec encoder = null;



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
        int br = VideoUtils.getBitRate(inputPath);
        // Very naive (3500kbs) for target framerate of HD 720p, 60fps
        return (br > VideoUtils.DEFAULT_VIDEO_BITRATE);
    }

    // ***************
    //  Given a path to a file, we feed data to our encoder to re-encode data to a lower but acceptable
    //  quality
    private void startFeedingEncoder(String inputPath)
    {

    }

    // ***************
    //  Testing ideas from https://github.com/hoolrory/AndroidVideoSamples/blob/master/CommonVideoLibrary/src/com/roryhool/commonvideolibrary/VideoResampler.java
    //  Might throw exception
    public void reEncodeVideo(String inputPath,
                                        int frameRate,
                                        int bitRate,
                                        int width,
                                        int height) throws IOException {
        //  Some examples https://github.com/PhilLab/Android-MediaCodec-Examples/blob/master/EncodeDecodeTest.java
        //  https://developer.android.com/reference/android/media/MediaCodec

        MediaFormat outputFormat = MediaFormat.createVideoFormat( MediaHelper.MIME_TYPE_AVC, width, height );
        outputFormat.setInteger( MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface );
        outputFormat.setInteger( MediaFormat.KEY_BIT_RATE, bitRate );

        outputFormat.setInteger( MediaFormat.KEY_FRAME_RATE, frameRate );

        outputFormat.setInteger( MediaFormat.KEY_I_FRAME_INTERVAL, VideoUtils.DEFAULT_IFRAME_INTERVAL_SEC );

        // Here we might throw
        encoder = MediaCodec.createEncoderByType( MediaHelper.MIME_TYPE_AVC );

        encoder.configure( outputFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE );
        encoder.start();
        Log.i(VideoUtils.LOG_TAG, "Started encoder ...");

        // Now we need to feed to
        startFeedingEncoder(inputPath);

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

    // ***************
    public Boolean setBitRate(String inputPath,
                                           int targetBitrateInK,
                                           String outputPath)
    {

        // https://stackoverflow.com/questions/32662382/how-to-get-fps-from-video-in-java
        if (VideoUtils.shouldReEncode(inputPath))
        {
            // Maybe dont change the framerate? if possible
            MediaMetadataRetriever metadataRetriever = new MediaMetadataRetriever();
            metadataRetriever.setDataSource(inputPath);

            // Extract the rest of stuff
            int frameRate = VideoUtils.getFrameRate(inputPath);

            try
            {
                reEncodeVideo(inputPath, frameRate, VideoUtils.DEFAULT_VIDEO_BITRATE, VideoUtils.WIDTH_720P, VideoUtils.HEIGHT_720P);
                return  true;
            }
            catch  (Exception e)
            {
                Log.w(VideoUtils.LOG_TAG, e.getMessage());
                return  false;
            }


        }
        else
        {
            //
            return  false;
        }

    }

}


// ***************
//  Returns true for success & false for failure
//  There were two problems with this
//      1) The output bitrate was NOT correct
//      2) The output video could be opened in windows but not in android!!!
    /*
    public static Boolean setBitRateFFMpeg(String inputPath,
                                     int targetBitrateInK,
                                     String outputPath)
    {
        // There is a bug in here the file exist
        // Check file exists

        File file = new File(inputPath);
        if (!file.exists())
        {
            Log.w("", "Could not find file at" + inputPath);
            return false;
        }



        String targetBitRateInKB = Integer.toString(targetBitrateInK) + "k";

        String commandParam = "-i " + inputPath + " -b:v " +  targetBitRateInKB + " " + outputPath;

        int rc = FFmpeg.execute(commandParam);
        if (rc == RETURN_CODE_SUCCESS) {
            Log.i(Config.TAG, "Command execution completed successfully.");
            return true;
        } else if (rc == RETURN_CODE_CANCEL) {
            Log.w(Config.TAG, "Command execution cancelled by user.");
            return false;
        } else {
            Log.w(Config.TAG, String.format("Command execution failed with rc=%d and the output below.", rc));
            Config.printLastCommandOutput(Log.INFO);
            return false;
        }


    }
        // ***************
    //  Well we are testing different classes until we find a working one!
    enum USE
    {
        //FFMPEG,
        JAVAC
    }


    // ***************
    //  Returns true for success & false for failure
    public static Boolean setBitRate(String inputPath,
                                     int targetBitrateInK,
                                     String outputPath,
                                     VideoUtils.USE use)
    {
        switch (use)
        {
            //case FFMPEG: return setBitRateFFMpeg(inputPath, targetBitrateInK, outputPath);
            case JAVAC: return setBitRateJavaCV(inputPath, targetBitrateInK, outputPath);
            default:    return false;
        }
    }
    // ***************
    //  Returns the bitrate
    //  Returns a negative number if failed
    public static int getBitRate(String path) {

        // Check file exists
        File file = new File(path);
        if (!file.exists())
        {
            Log.w("", "Could not find file at" + path);
            return -1;
        }

        // MediaInformation info = FFprobe.getMediaInformation(path);

        // Maybe the
        // String bitRate = info.getBitrate();
    FrameGrabber fg = new OpenCVFrameGrabber(path);
    int bitRateDouble = fg.getVideoBitrate();

    String bitRate = "1";

        return Integer.parseInt(bitRate);
    }

        // There is a bug here  https://github.com/opencv/opencv/issues/18463
        //  The file exists but VideoCapture cant open it??
        /*
        try (
            InputStream inputStream = new FileInputStream(file);
        )
        {
            int byteRead = -1;
            while ((byteRead = inputStream.read()) != -1) {
                Log.i("", Integer.toString(byteRead));
            }

        }
        catch (IOException ex) {
            Log.w(VideoUtils.LOG_TAG, "Could not read file at" + path);
        }
        */

        /*
        VideoCapture cap = new VideoCapture();

        // jeeeeeeeesus https://github.com/opencv/opencv/issues/22920
        //  https://docs.opencv.org/4.6.0/d4/d15/group__videoio__flags__base.html#ga023786be1ee68a9105bf2e48c700294d
        //  but we have another issue here, the flag CAP_ANDROID is unused!!!???

        // if (!cap.open(path, Videoio.CAP_ANY))
        if (!cap.open(path, Videoio.CAP_ANDROID))
        {
            Log.i(VideoUtils.LOG_TAG, "File exists but couldnt open");
            return -1;
        }


        // https://docs.opencv.org/3.4/d4/d15/group__videoio__flags__base.html
        double brf = cap.get(Videoio.CAP_PROP_BITRATE);

        Log.i(VideoUtils.LOG_TAG, Double.toString( cap.get(Videoio.CAP_PROP_FRAME_WIDTH) ) );
        Log.i(VideoUtils.LOG_TAG, Double.toString( cap.get(Videoio.CAP_PROP_FRAME_HEIGHT) ) );

        return (int) brf;
         */
