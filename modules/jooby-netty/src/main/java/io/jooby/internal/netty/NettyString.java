/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.netty;

import java.nio.charset.StandardCharsets;

import edu.umd.cs.findbugs.annotations.NonNull;

public class NettyString implements CharSequence {

  final byte[] bytes;
  private final String value;

  public NettyString(String value) {
    this.value = value;
    this.bytes = value.getBytes(StandardCharsets.US_ASCII);
  }

  public static NettyString of(String value) {
    return new NettyString(value);
  }

  @Override
  public int length() {
    return value.length();
  }

  @Override
  public char charAt(int index) {
    return value.charAt(index);
  }

  @Override
  @NonNull public CharSequence subSequence(int start, int end) {
    return value.subSequence(start, end);
  }

  @Override
  @NonNull public String toString() {
    return value;
  }
}
