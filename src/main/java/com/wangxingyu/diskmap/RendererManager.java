package com.wangxingyu.diskmap;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.View;

import com.wangxingyu.diskmap.entity.FileSystemSuperRoot;
import com.wangxingyu.diskmap.opengl.FileSystemViewGPU;


public class RendererManager {
    private static final String HW_RENDERER = "hw_renderer";

    private final DiskUsage diskusage;
    private boolean hwRenderer;
    private boolean rendererChanged = false;

    public RendererManager(DiskUsage diskusage) {
        this.diskusage = diskusage;
    }

    private SharedPreferences getPrefs() {
        return diskusage.getSharedPreferences("settings", Context.MODE_PRIVATE);
    }


    public void switchRenderer(final FileSystemSuperRoot root) {
        diskusage.fileSystemState.killRenderThread();
        if (hwRenderer) {
            finishRendererSwitch(root);
            return;
        }

        finishRendererSwitch(root);
    }

    public void finishRendererSwitch(FileSystemSuperRoot root) {
        hwRenderer = !hwRenderer;
        rendererChanged = true;
        makeView(diskusage.fileSystemState, root);
    }

    public void makeView(FileSystemState eventHandler, FileSystemSuperRoot root) {
        View view;
        if (hwRenderer) {
            view = new FileSystemViewGPU(diskusage, eventHandler);
        } else {
            view = new FileSystemViewCPU(diskusage, eventHandler);
        }
        diskusage.menu.wrapAndSetContentView(view, root);
        view.requestFocus();
    }


    public void onResume() {
        hwRenderer = getPrefs().getBoolean(HW_RENDERER, true);
    }

    public void onPause() {
        if (rendererChanged) {
            getPrefs().edit().putBoolean(HW_RENDERER, hwRenderer).commit();
        }
    }
}
