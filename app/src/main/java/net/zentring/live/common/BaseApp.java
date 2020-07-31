package net.zentring.live.common;

import android.app.Application;

import net.zentring.live.data;

import org.xutils.x;

public class BaseApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        x.Ext.init(this);
        x.Ext.setDebug(false);
    }
}
