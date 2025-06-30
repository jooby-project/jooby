/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.buffer;

public class BufferOptions {
  private int size;

  private boolean directBuffers;

  public BufferOptions() {
    long maxMemory = Runtime.getRuntime().maxMemory();
    // smaller than 64mb of ram we use 512b buffers
    if (maxMemory < 64 * 1024 * 1024) {
      // use 512b buffers
      directBuffers = false;
      size = 512;
      // 128mb
    } else if (maxMemory < 128 * 1024 * 1024) {
      // use 1k buffers
      directBuffers = true;
      size = 1024;
    } else if (maxMemory < 512 * 1024 * 1024) {
      // use 4k buffers
      directBuffers = true;
      size = 4096;
    } else {
      // use 16k buffers for best performance
      // as 16k is generally the max amount of data that can be sent in a single write() call
      directBuffers = true;
      size =
          1024 * 16 - 20; // the 20 is to allow some space for protocol headers, see UNDERTOW-1209
    }
  }

  /**
   * Heap buffer of 512kb initial size.
   *
   * @return Heap buffer of 512kb initial size.
   */
  public static BufferOptions small() {
    return new BufferOptions().setDirectBuffers(false).setSize(512);
  }

  public static BufferOptions defaults() {
    return new BufferOptions();
  }

  /**
   * Default and initial buffer size.
   *
   * @return Buffer size.
   */
  public int getSize() {
    return size;
  }

  /**
   * Set default and initial buffer size.
   *
   * @param size The default initial buffer size.
   * @return This options.
   */
  public BufferOptions setSize(int size) {
    this.size = size;
    return this;
  }

  public boolean isDirectBuffers() {
    return directBuffers;
  }

  public BufferOptions setDirectBuffers(boolean directBuffers) {
    this.directBuffers = directBuffers;
    return this;
  }

  @Override
  public String toString() {
    return "{size: " + size + ", direct: " + directBuffers + '}';
  }
}
