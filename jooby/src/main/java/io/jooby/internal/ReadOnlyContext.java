/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Date;

import io.jooby.Context;
import io.jooby.Cookie;
import io.jooby.FileDownload;
import io.jooby.ForwardingContext;
import io.jooby.MediaType;
import io.jooby.Sender;
import io.jooby.SneakyThrows;
import io.jooby.StatusCode;

public class ReadOnlyContext extends ForwardingContext {
  private static final String MESSAGE = "The response has already been started";

  public ReadOnlyContext(Context context) {
    super(context);
  }

  @Override
  public boolean isResponseStarted() {
    return true;
  }

  @Override
  public Context send(Path file) {
    throw new IllegalStateException(MESSAGE);
  }

  @Override
  public Context send(byte[] data) {
    throw new IllegalStateException(MESSAGE);
  }

  @Override
  public Context send(String data) {
    throw new IllegalStateException(MESSAGE);
  }

  @Override
  public Context send(ByteBuffer data) {
    throw new IllegalStateException(MESSAGE);
  }

  @Override
  public Context send(FileChannel file) {
    throw new IllegalStateException(MESSAGE);
  }

  @Override
  public Context send(FileDownload file) {
    throw new IllegalStateException(MESSAGE);
  }

  @Override
  public Context send(InputStream input) {
    throw new IllegalStateException(MESSAGE);
  }

  @Override
  public Context send(StatusCode statusCode) {
    throw new IllegalStateException(MESSAGE);
  }

  @Override
  public Context send(ReadableByteChannel channel) {
    throw new IllegalStateException(MESSAGE);
  }

  @Override
  public Context send(String data, Charset charset) {
    throw new IllegalStateException(MESSAGE);
  }

  @Override
  public Context sendError(Throwable cause) {
    throw new IllegalStateException(MESSAGE);
  }

  @Override
  public Context sendError(Throwable cause, StatusCode statusCode) {
    throw new IllegalStateException(MESSAGE);
  }

  @Override
  public Context sendRedirect(String location) {
    throw new IllegalStateException(MESSAGE);
  }

  @Override
  public Context sendRedirect(StatusCode redirect, String location) {
    throw new IllegalStateException(MESSAGE);
  }

  @Override
  public Context render(Object value) {
    throw new IllegalStateException(MESSAGE);
  }

  @Override
  public Context responseStream(SneakyThrows.Consumer<OutputStream> consumer) throws Exception {
    throw new IllegalStateException(MESSAGE);
  }

  @Override
  public Context responseStream(MediaType contentType, SneakyThrows.Consumer<OutputStream> consumer)
      throws Exception {
    throw new IllegalStateException(MESSAGE);
  }

  @Override
  public Context responseWriter(MediaType contentType, SneakyThrows.Consumer<PrintWriter> consumer)
      throws Exception {
    throw new IllegalStateException(MESSAGE);
  }

  @Override
  public Context responseWriter(SneakyThrows.Consumer<PrintWriter> consumer) throws Exception {
    throw new IllegalStateException(MESSAGE);
  }

  @Override
  public OutputStream responseStream() {
    throw new IllegalStateException(MESSAGE);
  }

  @Override
  public OutputStream responseStream(MediaType contentType) {
    throw new IllegalStateException(MESSAGE);
  }

  @Override
  public PrintWriter responseWriter() {
    throw new IllegalStateException(MESSAGE);
  }

  @Override
  public PrintWriter responseWriter(MediaType contentType) {
    throw new IllegalStateException(MESSAGE);
  }

  @Override
  public Sender responseSender() {
    throw new IllegalStateException(MESSAGE);
  }

  @Override
  public Context removeResponseHeader(String name) {
    throw new IllegalStateException(MESSAGE);
  }

  @Override
  public Context setResponseCookie(Cookie cookie) {
    throw new IllegalStateException(MESSAGE);
  }

  @Override
  public Context setResponseHeader(String name, Date value) {
    throw new IllegalStateException(MESSAGE);
  }

  @Override
  public Context setResponseCode(int statusCode) {
    throw new IllegalStateException(MESSAGE);
  }

  @Override
  public Context setResponseCode(StatusCode statusCode) {
    throw new IllegalStateException(MESSAGE);
  }

  @Override
  public Context setResponseHeader(String name, Object value) {
    throw new IllegalStateException(MESSAGE);
  }

  @Override
  public Context setResponseHeader(String name, String value) {
    throw new IllegalStateException(MESSAGE);
  }

  @Override
  public Context setResponseHeader(String name, Instant value) {
    throw new IllegalStateException(MESSAGE);
  }

  @Override
  public Context setResponseLength(long length) {
    throw new IllegalStateException(MESSAGE);
  }

  @Override
  public Context setResponseType(String contentType) {
    throw new IllegalStateException(MESSAGE);
  }

  @Override
  public Context setResponseType(MediaType contentType) {
    throw new IllegalStateException(MESSAGE);
  }

  @Override
  public Context setDefaultResponseType(MediaType contentType) {
    throw new IllegalStateException(MESSAGE);
  }
}
