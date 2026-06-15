package com.example.mapmemories.systemHelpers;

import android.content.Context;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

public class MediaCacheManager {
    private static final String MEDIA_DIR = "ghost_media";

    public static String saveToInternal(Context context, InputStream inputStream, String fileName) {
        try {
            File directory = new File(context.getFilesDir(), MEDIA_DIR);
            if (!directory.exists()) directory.mkdirs();

            File file = new File(directory, fileName);
            try (OutputStream output = new FileOutputStream(file)) {
                byte[] buffer = new byte[4096];
                int read;
                while ((read = inputStream.read(buffer)) != -1) {
                    output.write(buffer, 0, read);
                }
                output.flush();
            }
            return file.getAbsolutePath();
        } catch (Exception e) {
            return null;
        }
    }

    public static void clearCache(Context context) {
        File directory = new File(context.getFilesDir(), MEDIA_DIR);
        if (directory.exists()) {
            for (File file : directory.listFiles()) file.delete();
        }
    }
}