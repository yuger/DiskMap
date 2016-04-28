package com.wangxingyu.diskmap.opengl;


import com.wangxingyu.diskmap.entity.FileSystemEntry;

public class DrawingCache {
  private FileSystemEntry entry;
  private String sizeString;
  public RenderingThread.TextPixels textPixels;
  public RenderingThread.TextPixels sizePixels;

  public DrawingCache(FileSystemEntry entry) {
    this.entry = entry;
  }
  
  public String getSizeString() {
    if (sizeString != null) {
      return sizeString;
    }
    String sizeString = FileSystemEntry.calcSizeStringFromEncoded(
        entry.encodedSize);
    this.sizeString = sizeString;
    return sizeString;
  }

  public void resetSizeString() {
    sizeString = null;
    sizePixels = null;
  }
  
  public void drawText(RenderingThread rt, float x0, float y0, int elementWidth) {
    if (textPixels == null) {
      textPixels = new RenderingThread.TextPixels(entry.name);
    }
    textPixels.draw(rt, x0, y0, elementWidth);
  }
  
  public void drawSize(RenderingThread rt, float x0, float y0, int elementWidth) {
    if (sizePixels == null) {
      sizePixels = new RenderingThread.TextPixels(getSizeString());
    }
    sizePixels.draw(rt, x0, y0, elementWidth);
  }
}
