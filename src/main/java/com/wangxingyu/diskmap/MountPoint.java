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


import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.os.Environment;
import android.os.StatFs;
import android.util.Log;

import com.wangxingyu.diskmap.entity.FileSystemEntry;
import com.wangxingyu.diskmap.entity.FileSystemRoot;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.TreeMap;

public class MountPoint {
    static int checksum = 0;
    private static MountPoint defaultStorage;
    private static Map<String, MountPoint> mountPoints = new TreeMap<>();
    private static Map<String, MountPoint> rootedMountPoints = new TreeMap<>();
    private static boolean init = false;
    private static MountPoint honeycombSdcard;
    final FileSystemEntry.ExcludeFilter excludeFilter;
    final String root;
    final boolean hasApps2SD;
    final boolean rootRequired;
    final String fsType;
    String title;

    MountPoint(String title, String root, FileSystemEntry.ExcludeFilter excludeFilter, boolean hasApps2SD, boolean rootRequired, String fsType) {
        this.title = title;
        this.root = root;
        this.excludeFilter = excludeFilter;
        this.hasApps2SD = hasApps2SD;
        this.rootRequired = rootRequired;
        this.fsType = fsType;
    }

    public static MountPoint getHoneycombSdcard(Context context) {
        initMountPoints(context);
        return honeycombSdcard;
    }

    public static Map<String, MountPoint> getMountPoints(Context context) {
        initMountPoints(context);
        return mountPoints;
    }

    public static Map<String, MountPoint> getRootedMountPoints(Context context) {
        initMountPoints(context);
        return rootedMountPoints;
    }

    public static MountPoint getNormal(Context context, String rootPath) {
        initMountPoints(context);
        return mountPoints.get(rootPath);
    }

    public static MountPoint forPath(Context context, String path) {
        Log.d("diskusage", "Looking for mount point for path: " + path);
        initMountPoints(context);
        MountPoint match = null;
        path = FileSystemRoot.withSlash(path);
        for (MountPoint m : mountPoints.values()) {
            if (path.contains(FileSystemRoot.withSlash(m.root))) {
                if (match == null || match.root.length() < m.root.length()) {
                    Log.d("diskusage", "MATCH:" + m.root);
                    match = m;
                }
            }
        }
        for (MountPoint m : rootedMountPoints.values()) {
            if (path.contains(FileSystemRoot.withSlash(m.root))) {
                if (match == null || match.root.length() < m.root.length()) {
                    match = m;
                    Log.d("diskusage", "MATCH:" + m.root);
                }
            }
        }

        // FIXME: quick hack
        if (match == null) {
            Log.d("diskusage", "Use honeycomb hack for /data");
            match = mountPoints.get("/data");
        }
        return match;
    }

    public static MountPoint getRooted(Context context, String rootPath) {
        initMountPoints(context);
        return rootedMountPoints.get(rootPath);
    }

    public static String storageCardPath() {
        try {
            return Environment.getExternalStorageDirectory().getCanonicalPath();
        } catch (Exception e) {
            return Environment.getExternalStorageDirectory().getAbsolutePath();
        }
    }

    private static boolean isEmulated(String fsType) {
        return fsType.equals("sdcardfs") || fsType.equals("fuse");
    }

    private static void initMountPoints(Context context) {
        if (init) return;
        init = true;
        String storagePath = storageCardPath();
        Log.d("diskusage", "StoragePath: " + storagePath);

        ArrayList<MountPoint> mountPointsList = new ArrayList<>();
        HashSet<String> excludePoints = new HashSet<>();
        if (storagePath != null) {
            defaultStorage = new MountPoint(titleStorageCard(context), storagePath, null, false, false, "");
            mountPointsList.add(defaultStorage);
            mountPoints.put(storagePath, defaultStorage);
        }

        try {
            // FIXME: debug
            checksum = 0;
            BufferedReader reader = new BufferedReader(new FileReader("/proc/mounts"));
//      BufferedReader reader = new BufferedReader(new InputStreamReader(context.getResources().openRawResource(R.raw.mounts_honeycomb), "UTF-8"));
            String line;
            while ((line = reader.readLine()) != null) {
                checksum += line.length();
                Log.d("diskusage", "line: " + line);
                String[] parts = line.split(" +");
                if (parts.length < 3) continue;
                String mountPoint = parts[1];
                Log.d("diskusage", "Mount point: " + mountPoint);
                String fsType = parts[2];

                StatFs stat = null;
                try {
                    stat = new StatFs(mountPoint);
                } catch (Exception e) {
                }

                if (!(fsType.equals("vfat") || fsType.equals("tntfs") || fsType.equals("exfat") || fsType.equals("texfat") || isEmulated(fsType)) || mountPoint.startsWith("/mnt/asec") || mountPoint.startsWith("/firmware") || mountPoint.startsWith("/mnt/secure") || mountPoint.startsWith("/data/mac") || stat == null || (mountPoint.endsWith("/legacy") && isEmulated(fsType))) {
                    Log.d("diskusage", String.format("Excluded based on fsType=%s or black list", fsType));
                    excludePoints.add(mountPoint);

                    // Default storage is not vfat, removing it (real honeycomb)
                    if (mountPoint.equals(storagePath)) {
                        mountPointsList.remove(defaultStorage);
                        mountPoints.remove(mountPoint);
                    }
                    if (/*rooted &&*/ !mountPoint.startsWith("/mnt/asec/")) {
                        mountPointsList.add(new MountPoint(mountPoint, mountPoint, null, false, true, fsType));
                    }
                } else {
                    Log.d("diskusage", "Mount point is good");
                    mountPointsList.add(new MountPoint(mountPoint, mountPoint, null, false, false, fsType));
                }
            }

            for (MountPoint mountPoint : mountPointsList) {
                String prefix = mountPoint.root + "/";
                boolean has_apps2sd = false;
                ArrayList<String> excludes = new ArrayList<>();
                String mountPointName = new File(mountPoint.root).getName();

                for (MountPoint otherMountPoint : mountPointsList) {
                    if (otherMountPoint.root.startsWith(prefix)) {
                        excludes.add(mountPointName + "/" + otherMountPoint.root.substring(prefix.length()));
                    }
                }
                for (String otherMountPoint : excludePoints) {
                    if (otherMountPoint.equals(prefix + ".android_secure")) {
                        has_apps2sd = true;
                    }
                    if (otherMountPoint.startsWith(prefix)) {
                        excludes.add(mountPointName + "/" + otherMountPoint.substring(prefix.length()));
                    }
                }
                MountPoint newMountPoint = new MountPoint(mountPoint.root, mountPoint.root, new FileSystemEntry.ExcludeFilter(excludes), has_apps2sd, mountPoint.rootRequired, mountPoint.fsType);
                if (mountPoint.rootRequired) {
                    rootedMountPoints.put(mountPoint.root, newMountPoint);
                } else {
                    mountPoints.put(mountPoint.root, newMountPoint);
                }
            }
        } catch (Exception e) {
            Log.e("diskusage", "Failed to get mount points", e);
        }
        final int sdkVersion = Build.VERSION.SDK_INT;

        try {
            addMediaPaths(context);
        } catch (Throwable t) {
            Log.e("diskusage", "Adding media paths", t);
        }

        MountPoint storageCard = mountPoints.get(storageCardPath());
        if (sdkVersion >= Build.VERSION_CODES.HONEYCOMB && (storageCard == null || isEmulated(storageCard.fsType))) {
            mountPoints.remove(storageCardPath());
            // No real /sdcard in honeycomb
            honeycombSdcard = defaultStorage;
            mountPoints.put("/data", new MountPoint(titleStorageCard(context), "/data", null, false, false, ""));
        }

        if (!mountPoints.isEmpty()) {
            defaultStorage = mountPoints.values().iterator().next();
            defaultStorage.title = titleStorageCard(context);
        }
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    public static File[] getMediaStoragePaths(Context context) {
        try {
            return context.getExternalFilesDirs(Environment.DIRECTORY_DCIM);
        } catch (Throwable t) {
            return new File[0];
        }
    }

    public static String canonicalPath(File file) {
        try {
            return file.getCanonicalPath();
        } catch (Exception e) {
            return file.getAbsolutePath();
        }
    }

    private static void addMediaPaths(Context context) {
        File[] mediaStoragePaths = getMediaStoragePaths(context);
        for (File file : mediaStoragePaths) {
            while (file != null) {
                String canonical = canonicalPath(file);

                if (mountPoints.containsKey(canonical)) {
                    break;
                }

                MountPoint rootedMountPoint = rootedMountPoints.get(canonical);
                if (rootedMountPoint != null) {
                    mountPoints.put(canonical, new MountPoint(canonical, canonical, null, false, false, rootedMountPoint.fsType));
                    break;
                }
                if (canonical.equals("/")) break;
                file = file.getParentFile();
            }
        }
    }

    private static String titleStorageCard(Context context) {
        return context.getString(R.string.storage_card);
    }

    public static void reset() {
        defaultStorage = null;
        mountPoints = new TreeMap<>();
        init = false;
    }

    public FileSystemEntry.ExcludeFilter getExcludeFilter() {
        return excludeFilter;
    }

    public String getRoot() {
        return root;
    }
}
