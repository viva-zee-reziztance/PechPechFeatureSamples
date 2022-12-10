package com.example.pechpechfeaturesamples;

import static com.arthenica.mobileffmpeg.Config.RETURN_CODE_CANCEL;
import static com.arthenica.mobileffmpeg.Config.RETURN_CODE_SUCCESS;

import android.util.Log;

import com.arthenica.mobileffmpeg.Config;
import com.arthenica.mobileffmpeg.FFmpeg;
import com.arthenica.mobileffmpeg.FFprobe;
import com.arthenica.mobileffmpeg.MediaInformation;

import java.io.File;

public class VideoUtils {

    // ***************
    //  Returns true for success & false for failure
    public static Boolean setBitRate(String inputPath, int targetBitrate, String outputPath)
    {
        /*
        // There is a bug in here the file exist
        // Check file exists

        File file = new File(inputPath);
        if (!file.exists())
        {
            Log.w("", "Could not find file at" + inputPath);
            return false;
        }

        */




        //
        String targetBitRateInKB = Integer.toString(targetBitrate/1000) + "k";

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

        MediaInformation info = FFprobe.getMediaInformation(path);

        // Maybe the
        String bitRate = info.getBitrate();



        return Integer.parseInt(bitRate);
    }

}
