/**
 * DiskUsage - displays sdcard usage on android.
 * Copyright (C) 2016 wangxingyu
 * <p/>
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * <p/>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p/>
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package com.wangxingyu.diskmap;

import android.content.Intent;
import android.os.Bundle;
import android.os.StatFs;
import android.util.Log;

import com.wangxingyu.diskmap.entity.FileSystemEntry;
import com.wangxingyu.diskmap.entity.FileSystemFreeSpace;
import com.wangxingyu.diskmap.entity.FileSystemPackage;
import com.wangxingyu.diskmap.entity.FileSystemRoot;
import com.wangxingyu.diskmap.entity.FileSystemSpecial;
import com.wangxingyu.diskmap.entity.FileSystemSuperRoot;
import com.wangxingyu.diskmap.entity.FileSystemSystemSpace;

import java.util.ArrayList;

public class AppUsage extends DiskUsage {
    private AppFilter pendingFilter;

    FileSystemSuperRoot wrapApps(FileSystemSpecial appsElement, AppFilter filter, int displayBlockSize) {
        long freeSize = 0;
        long allocatedSpace = 0;
        long systemSize = 0;
        Log.d("diskusage", "memory = " + filter.memory);
        if (filter.memory == AppFilter.App2SD.INTERNAL) {
            StatFs data = new StatFs("/data");
            long dataBlockSize = data.getBlockSizeLong();
            freeSize = data.getAvailableBlocksLong() * dataBlockSize;
            allocatedSpace = data.getBlockCountLong() * dataBlockSize - freeSize;
        }

        if (allocatedSpace > 0) {
            systemSize = allocatedSpace - appsElement.getSizeInBlocks() * displayBlockSize;
        }

//    if (filter.useSD) {
//      FileSystemRoot newRoot = new FileSystemRoot(displayBlockSize);
//      newRoot.setChildren(new FileSystemEntry[] { appsElement }, displayBlockSize);
//      return newRoot;
//    }

        ArrayList<FileSystemEntry> entries = new ArrayList<>();
        entries.add(appsElement);
        if (systemSize > 0) {
            entries.add(new FileSystemSystemSpace("System data", systemSize, displayBlockSize));
        }
        if (freeSize > 0) {
            entries.add(new FileSystemFreeSpace("Free space", freeSize, displayBlockSize));
        }

        FileSystemEntry[] internalArray = entries.toArray(new FileSystemEntry[]{});
        String name = "Data";
        if (filter.memory == AppFilter.App2SD.BOTH) {
            name = "Data & Storage";
        } else if (filter.memory == AppFilter.App2SD.APPS2SD) {
            name = "Storage";
        }
        FileSystemEntry internalElement = FileSystemRoot.makeNode(name, "/Apps").setChildren(internalArray, displayBlockSize);

        FileSystemSuperRoot newRoot = new FileSystemSuperRoot(displayBlockSize);
        newRoot.setChildren(new FileSystemEntry[]{internalElement}, displayBlockSize);
        return newRoot;
    }

    @Override
    FileSystemSuperRoot scan() {
        AppFilter filter = pendingFilter;
        int displayBlockSize = 512;
        FileSystemEntry[] appsArray = loadApps2SD(false, filter, displayBlockSize);
        FileSystemSpecial appsElement = new FileSystemSpecial("Applications", appsArray, displayBlockSize);
        appsElement.filter = filter;
        return wrapApps(appsElement, filter, displayBlockSize);
    }

    @Override
    protected void onCreate(Bundle icicle) {
        pendingFilter = AppFilter.loadSavedAppFilter(this);
        super.onCreate(icicle);
        Log.d("diskusage", "onCreate");
    }

    private FileSystemSpecial getAppsElement(FileSystemState view) {
        FileSystemEntry root = view.masterRoot;
        FileSystemEntry apps = root.children[0].children[0];
        if (apps instanceof FileSystemPackage) {
            apps = apps.parent;
        }
        return (FileSystemSpecial) apps;
    }

    private void updateFilter(AppFilter newFilter) {
        // FIXME: hack
        if (fileSystemState == null) {
            pendingFilter = newFilter;
            return;
        }

        int displayBlockSize = fileSystemState.masterRoot.getDisplayBlockSize();
        FileSystemSpecial appsElement = getAppsElement(fileSystemState);
        if (newFilter.equals(appsElement.filter)) {
            return;
        }
        for (FileSystemEntry entry : appsElement.children) {
            FileSystemPackage pkg = (FileSystemPackage) entry;
            pkg.applyFilter(newFilter, displayBlockSize);
        }
        java.util.Arrays.sort(appsElement.children, FileSystemEntry.COMPARE);

        appsElement = new FileSystemSpecial(appsElement.name, appsElement.children, displayBlockSize);
        appsElement.filter = newFilter;

        FileSystemSuperRoot newRoot = wrapApps(appsElement, newFilter, displayBlockSize);
        getPersistantState().root = newRoot;
        afterLoadAction.clear();
        fileSystemState.startZoomAnimationInRenderThread(newRoot, true, false);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        Log.d("diskusage", "onSaveInstanceState");

        if (fileSystemState == null) return;
        fileSystemState.killRenderThread();
        FileSystemSpecial appsElement = getAppsElement(fileSystemState);
        outState.putParcelable("filter", appsElement.filter);
    }

    @Override
    protected void onRestoreInstanceState(Bundle inState) {
        super.onRestoreInstanceState(inState);
        Log.d("diskusage", "onRestoreInstanceState");
        AppFilter newFilter = inState.getParcelable("filter");
        if (newFilter != null) updateFilter(newFilter);
    }

    @Override
    public void onActivityResult(int a, int result, Intent i) {
        super.onActivityResult(a, result, i);
        AppFilter newFilter = AppFilter.loadSavedAppFilter(this);
        updateFilter(newFilter);
    }

}
