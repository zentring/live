package net.zentring.live.graphic;

import android.graphics.BitmapFactory;

import net.zentring.live.Utils;
import net.zentring.live.common.Common;
import net.zentring.live.download.DownloadManager;

import org.xutils.ex.DbException;

import java.io.File;

public class HitGraphic {
    private String path;

    public void useGraphic(String url) {
        path = Utils.getHitGraphicPath();
        if (Common.downloadManager == null) {
            Common.downloadManager = DownloadManager.getInstance();
        }

        boolean canUse;
        File file = new File(path);
        if (!file.exists()) {
            canUse = file.mkdirs();
        } else {
            canUse = true;
        }
        if (canUse) {
            final String label = "hit";
            final String sdPath = path + label + ".png";
            try {
                DownloadManager.MyDownLoadHolder myDownLoadHolder = Common.downloadManager.startDownload(
                        url, label,
                        sdPath, true, false, null);
                myDownLoadHolder.onSuccessListener(new DownloadManager.MyDownLoadHolder.OnDoneListener() {
                    @Override
                    public void onDone(int status) {
                        if (status == Common.DOWN_LOAD_GRAPHIC_STATUS.success.toNumber()) {
                            BitmapFactory.Options options = new BitmapFactory.Options();
                            options.inJustDecodeBounds = true;
                            BitmapFactory.decodeFile(sdPath, options);
                            int imgWidth = options.outWidth;
                            int imgHeight = options.outHeight;
                            if (mOnStatusListener != null) {
                                mOnStatusListener.status(Common.WHETHER_UP_GRAPHIC_STATUS.can.toNumber(), sdPath, imgWidth, imgHeight);
                            }
                        } else {
                            mOnStatusListener.status(Common.WHETHER_UP_GRAPHIC_STATUS.cannot.toNumber(), sdPath, 0, 0);
                        }
                    }
                });
            } catch (DbException e) {
                e.printStackTrace();
                mOnStatusListener.status(Common.WHETHER_UP_GRAPHIC_STATUS.cannot.toNumber(), sdPath, 0, 0);
            }
        }
    }

    private OnStatusListener mOnStatusListener;

    public void onStatusListener(OnStatusListener listener) {
        this.mOnStatusListener = listener;
    }

    public interface OnStatusListener {
        void status(int whetherUp, String sdPath, int width, int height);
    }
}
