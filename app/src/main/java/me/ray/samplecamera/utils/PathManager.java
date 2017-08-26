package me.ray.samplecamera.utils;

import android.content.Context;
import android.support.v4.content.ContextCompat;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;


/**
 * Created by Asura on 2017/6/19.
 */
public class PathManager {
    private final Context context;
    private File rootDir;
    private File cacheDir;
    private final SimpleDateFormat dateFormat;

    public PathManager(Context context) {
        this.context = context;
        File[] externalCacheDirs = ContextCompat.getExternalCacheDirs(context);

        if (externalCacheDirs.length > 1 && externalCacheDirs[1] != null) {
            cacheDir = externalCacheDirs[1];
            rootDir = new File(externalCacheDirs[1].getParentFile(), "sync");
        } else {
            cacheDir = externalCacheDirs[0];
            rootDir = new File(externalCacheDirs[0].getParentFile(), "sync");
        }
        dateFormat = TimeUtils.dateFormater_full_packed.get();
    }

    public File generateCameraImage() {
        String name = String.format("IMG_%s.jpg", dateFormat.format(new Date()));
        return new File(cacheDir, name);
    }

    public File generateCropImage() {
        File cropDir = new File(rootDir, "crop");
        if (!cropDir.exists()) {
            cropDir.mkdirs();
        }
        String name = String.format("IMG_%s_crop.jpg", dateFormat.format(new Date()));
        return new File(cropDir, name);
    }

    public File generateTemplatePreviewImage() {
        File resultDir = new File(rootDir, "result");
        if (!resultDir.exists()) {
            resultDir.mkdirs();
        }
        String name = String.format("IMG_%s_result.jpg", dateFormat.format(new Date()));
        return new File(resultDir, name);
    }

}
