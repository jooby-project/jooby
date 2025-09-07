/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.netty;

import java.nio.charset.StandardCharsets;

import edu.umd.cs.findbugs.annotations.NonNull;
import io.jooby.MediaType;
import io.netty.util.AsciiString;

public class NettyString implements CharSequence {
  static final CharSequence server = NettyString.of("N");
  static final CharSequence CONTENT_TYPE = NettyString.of("content-type");
  static final CharSequence CONTENT_LENGTH = NettyString.of("content-length");
  static final CharSequence ZERO = NettyString.of("0");
  static final CharSequence TEXT_PLAIN = NettyString.of("text/plain;charset=utf-8");
  static final CharSequence JSON = NettyString.of("application/json");
  static final CharSequence DATE = NettyString.of("date");
  static final CharSequence SERVER = NettyString.of("server");

  final byte[] bytes;
  private final String value;
  private final int hashCode;
  private final int length;

  public NettyString(String value) {
    this.value = value;
    this.hashCode = AsciiString.hashCode(value);
    this.bytes = value.getBytes(StandardCharsets.US_ASCII);
    this.length = value.length();
  }

  public static NettyString of(String value) {
    return new NettyString(value);
  }

  @Override
  public int length() {
    return length;
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
  public boolean equals(Object obj) {
    if (obj != null && obj.getClass() == NettyString.class) {
      return value.equals(((NettyString) obj).value);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return hashCode;
  }

  @Override
  @NonNull public String toString() {
    return value;
  }

  public static CharSequence valueOf(MediaType contentType) {
    if (MediaType.text.equals(contentType)) {
      return TEXT_PLAIN;
    } else if (MediaType.json.equals(contentType)) {
      return JSON;
    }
    return contentType.toContentTypeHeader();
  }
}
