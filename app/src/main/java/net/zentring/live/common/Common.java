package net.zentring.live.common;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import net.zentring.live.download.DownloadManager;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class Common {
    public static DownloadManager downloadManager;

    public enum DOWN_LOAD_GRAPHIC_STATUS {
        success(0),
        error(1),
        cancel(2);
        private int iNum = 0;

        DOWN_LOAD_GRAPHIC_STATUS(int iNum) {
            this.iNum = iNum;
        }

        public int toNumber() {
            return this.iNum;
        }
    }

    public enum WHETHER_UP_GRAPHIC_STATUS {
        can(0),
        cannot(1);
        private int iNum = 0;

        WHETHER_UP_GRAPHIC_STATUS(int iNum) {
            this.iNum = iNum;
        }

        public int toNumber() {
            return this.iNum;
        }
    }

    public static Bitmap getBitmap(String filePath) {
        File outputFile = new File(filePath);
        byte[] bytes = new byte[(int) outputFile.length() + 1];
        FileInputStream inputStream = null;
        try {
            inputStream = new FileInputStream(outputFile.getAbsoluteFile());
            inputStream.read(bytes);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
    }
}
