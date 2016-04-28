package com.wangxingyu.diskmap;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.View;

import com.wangxingyu.diskmap.entity.FileSystemEntry;
import com.wangxingyu.diskmap.entity.FileSystemFile;
import com.wangxingyu.diskmap.entity.FileSystemSpecial;
import com.wangxingyu.diskmap.entity.FileSystemSuperRoot;


public abstract class DiskUsageMenu {
    protected final DiskUsage diskusage;
    protected String searchPattern;
    protected FileSystemSuperRoot masterRoot;
    protected MenuItem searchMenuItem;
    protected MenuItem showMenuItem;
    protected MenuItem rescanMenuItem;
    protected MenuItem deleteMenuItem;
    protected MenuItem rendererMenuItem;
    protected MenuItem filterMenuItem;
    SearchManager searchManager = new SearchManager(this);
    private FileSystemEntry selectedEntity;

    public DiskUsageMenu(DiskUsage diskusage) {
        this.diskusage = diskusage;
    }

    public static DiskUsageMenu getInstance(DiskUsage diskusage) {
        return new DiskUsageMenuHoneycomb(diskusage);
    }

    public abstract void onCreate();

    public abstract boolean readyToFinish();

    public abstract void searchRequest();

    public abstract MenuItem makeSearchMenuEntry(Menu menu);

    public final void onSaveInstanceState(Bundle outState) {
        outState.putString("search", searchPattern);
    }

    public final void onRestoreInstanceState(Bundle inState) {
        searchPattern = inState.getString("search");
    }

    public void wrapAndSetContentView(View view, FileSystemSuperRoot newRoot) {
        this.masterRoot = newRoot;
        updateMenu();
    }

    public void applyPattern(String searchQuery) {
        if (searchQuery == null || masterRoot == null) return;

        if (searchQuery.length() == 0) {
            searchManager.cancelSearch();
            finishedSearch(masterRoot, searchQuery);
        } else {
            searchManager.search(searchQuery);
        }
    }

    public boolean finishedSearch(FileSystemSuperRoot newRoot, String searchQuery) {
        boolean matched = newRoot != null;
        if (!matched) newRoot = masterRoot;
        diskusage.applyPatternNewRoot(newRoot, searchQuery);
        return matched;
    }

    public void update(FileSystemEntry position) {
        this.selectedEntity = position;
        updateMenu();
    }

    private String getString(int id) {
        return diskusage.getString(id);
    }

    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.clear();
        searchMenuItem = makeSearchMenuEntry(menu);

        showMenuItem = menu.add(getString(R.string.button_show));
        showMenuItem.setOnMenuItemClickListener(new OnMenuItemClickListener() {
            public boolean onMenuItemClick(MenuItem item) {
                if (selectedEntity != null) {
                    diskusage.view(selectedEntity);
                }
                return true;
            }
        });
        rescanMenuItem = menu.add(getString(R.string.button_rescan));
        rescanMenuItem.setOnMenuItemClickListener(new OnMenuItemClickListener() {
            public boolean onMenuItemClick(MenuItem item) {
                diskusage.rescan();
                return true;
            }
        });

        deleteMenuItem = menu.add(getString(R.string.button_delete));
        deleteMenuItem.setOnMenuItemClickListener(new OnMenuItemClickListener() {
            public boolean onMenuItemClick(MenuItem item) {
                diskusage.askForDeletion(selectedEntity);
                return true;
            }
        });

        rendererMenuItem = menu.add("Renderer");
        rendererMenuItem.setOnMenuItemClickListener(new OnMenuItemClickListener() {
            public boolean onMenuItemClick(MenuItem item) {
                diskusage.rendererManager.switchRenderer(masterRoot);
                return true;
            }
        });

        filterMenuItem = menu.add(getString(R.string.change_filter));
        filterMenuItem.setOnMenuItemClickListener(new OnMenuItemClickListener() {
            public boolean onMenuItemClick(MenuItem item) {
                diskusage.showFilterDialog();
                return true;
            }
        });

        updateMenu();
        return true;
    }

    private void updateMenu() {
        if (showMenuItem == null) return;

        if (diskusage.fileSystemState == null) {
            searchMenuItem.setEnabled(false);
            showMenuItem.setEnabled(false);
            rescanMenuItem.setEnabled(false);
            deleteMenuItem.setEnabled(false);
            rendererMenuItem.setEnabled(false);
            return;
        }

        if (diskusage.fileSystemState.sdcardIsEmpty()) {
            searchMenuItem.setEnabled(false);
            showMenuItem.setEnabled(false);
            rescanMenuItem.setEnabled(true);
            deleteMenuItem.setEnabled(false);
            rendererMenuItem.setEnabled(false);
        }

        rendererMenuItem.setEnabled(true);
        final boolean isGPU = diskusage.fileSystemState.isGPU();
        rendererMenuItem.setTitle(isGPU ? "Software Renderer" : "Hardware Renderer");

        rescanMenuItem.setEnabled(true);
        searchMenuItem.setEnabled(true);


        boolean view = !(selectedEntity == diskusage.fileSystemState.masterRoot.children[0] || selectedEntity instanceof FileSystemSpecial);
        showMenuItem.setEnabled(view);

        boolean fileOrNotSearching = searchPattern == null || selectedEntity.children == null;
        deleteMenuItem.setEnabled(view && selectedEntity instanceof FileSystemFile && fileOrNotSearching);

        boolean isAppUsage = diskusage instanceof AppUsage;

        deleteMenuItem.setVisible(!isAppUsage);
        filterMenuItem.setVisible(isAppUsage);
    }
}
