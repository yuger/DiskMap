package com.wangxingyu.diskmap;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;

import com.wangxingyu.diskmap.entity.FileSystemEntry;

import java.util.Map;
import java.util.Set;


public class ShowHideMountPointsActivity extends PreferenceActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.mount_points_ignore_list);
        getPreferenceScreen().setOrderingAsAdded(true);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Map<String, MountPoint> mountPoints = MountPoint.getRootedMountPoints(this);
        PreferenceScreen prefs = getPreferenceScreen();
        prefs.removeAll();
        SharedPreferences shprefs = getSharedPreferences("ignore_list", Context.MODE_PRIVATE);
        Map<String, ?> ignoreList = shprefs.getAll();
        Set<String> ignores = ignoreList.keySet();

        for (MountPoint mountPoint : mountPoints.values()) {
            CheckBoxPreference pref = new CheckBoxPreference(this);
            FileSystemEntry.setupStrings(this);
            DiskUsage.FileSystemStats stats = new DiskUsage.FileSystemStats(mountPoint);
            pref.setSummary(stats.formatUsageInfo());
            pref.setTitle(mountPoint.root);
            pref.setChecked(!ignores.contains(mountPoint.root));
            prefs.addPreference(pref);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        PreferenceScreen prefs = getPreferenceScreen();
        SharedPreferences shprefs = getSharedPreferences("ignore_list", Context.MODE_PRIVATE);
        Editor editor = shprefs.edit();
        editor.clear();

        for (int i = 0; i < prefs.getPreferenceCount(); i++) {
            CheckBoxPreference pref = (CheckBoxPreference) prefs.getPreference(i);
            String root = pref.getTitle().toString();
            if (!pref.isChecked()) {
                editor.putBoolean(root, true);
            }
        }
        editor.apply();
    }
}
