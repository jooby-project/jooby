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

import edu.umd.cs.findbugs.annotations.NonNull;
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

  public ReadOnlyContext(@NonNull Context context) {
    super(context);
  }

  @Override
  public boolean isResponseStarted() {
    return true;
  }

  @NonNull @Override
  public Context send(@NonNull Path file) {
    throw new IllegalStateException(MESSAGE);
  }

  @NonNull @Override
  public Context send(@NonNull byte[] data) {
    throw new IllegalStateException(MESSAGE);
  }

  @NonNull @Override
  public Context send(@NonNull String data) {
    throw new IllegalStateException(MESSAGE);
  }

  @NonNull @Override
  public Context send(@NonNull ByteBuffer data) {
    throw new IllegalStateException(MESSAGE);
  }

  @NonNull @Override
  public Context send(@NonNull FileChannel file) {
    throw new IllegalStateException(MESSAGE);
  }

  @NonNull @Override
  public Context send(@NonNull FileDownload file) {
    throw new IllegalStateException(MESSAGE);
  }

  @NonNull @Override
  public Context send(@NonNull InputStream input) {
    throw new IllegalStateException(MESSAGE);
  }

  @NonNull @Override
  public Context send(@NonNull StatusCode statusCode) {
    throw new IllegalStateException(MESSAGE);
  }

  @NonNull @Override
  public Context send(@NonNull ReadableByteChannel channel) {
    throw new IllegalStateException(MESSAGE);
  }

  @NonNull @Override
  public Context send(@NonNull String data, @NonNull Charset charset) {
    throw new IllegalStateException(MESSAGE);
  }

  @NonNull @Override
  public Context sendError(@NonNull Throwable cause) {
    throw new IllegalStateException(MESSAGE);
  }

  @NonNull @Override
  public Context sendError(@NonNull Throwable cause, @NonNull StatusCode statusCode) {
    throw new IllegalStateException(MESSAGE);
  }

  @NonNull @Override
  public Context sendRedirect(@NonNull String location) {
    throw new IllegalStateException(MESSAGE);
  }

  @NonNull @Override
  public Context sendRedirect(@NonNull StatusCode redirect, @NonNull String location) {
    throw new IllegalStateException(MESSAGE);
  }

  @NonNull @Override
  public Context render(@NonNull Object value) {
    throw new IllegalStateException(MESSAGE);
  }

  @NonNull @Override
  public Context responseStream(@NonNull SneakyThrows.Consumer<OutputStream> consumer)
      throws Exception {
    throw new IllegalStateException(MESSAGE);
  }

  @NonNull @Override
  public Context responseStream(
      @NonNull MediaType contentType, @NonNull SneakyThrows.Consumer<OutputStream> consumer)
      throws Exception {
    throw new IllegalStateException(MESSAGE);
  }

  @NonNull @Override
  public Context responseWriter(
      @NonNull MediaType contentType, @NonNull SneakyThrows.Consumer<PrintWriter> consumer)
      throws Exception {
    throw new IllegalStateException(MESSAGE);
  }

  @NonNull @Override
  public Context responseWriter(@NonNull SneakyThrows.Consumer<PrintWriter> consumer)
      throws Exception {
    throw new IllegalStateException(MESSAGE);
  }

  @NonNull @Override
  public OutputStream responseStream() {
    throw new IllegalStateException(MESSAGE);
  }

  @NonNull @Override
  public OutputStream responseStream(@NonNull MediaType contentType) {
    throw new IllegalStateException(MESSAGE);
  }

  @NonNull @Override
  public PrintWriter responseWriter() {
    throw new IllegalStateException(MESSAGE);
  }

  @NonNull @Override
  public PrintWriter responseWriter(@NonNull MediaType contentType) {
    throw new IllegalStateException(MESSAGE);
  }

  @NonNull @Override
  public Sender responseSender() {
    throw new IllegalStateException(MESSAGE);
  }

  @NonNull @Override
  public Context removeResponseHeader(@NonNull String name) {
    throw new IllegalStateException(MESSAGE);
  }

  @NonNull @Override
  public Context setResponseCookie(@NonNull Cookie cookie) {
    throw new IllegalStateException(MESSAGE);
  }

  @NonNull @Override
  public Context setResponseHeader(@NonNull String name, @NonNull Date value) {
    throw new IllegalStateException(MESSAGE);
  }

  @NonNull @Override
  public Context setResponseCode(int statusCode) {
    throw new IllegalStateException(MESSAGE);
  }

  @NonNull @Override
  public Context setResponseCode(@NonNull StatusCode statusCode) {
    throw new IllegalStateException(MESSAGE);
  }

  @NonNull @Override
  public Context setResponseHeader(@NonNull String name, @NonNull Object value) {
    throw new IllegalStateException(MESSAGE);
  }

  @NonNull @Override
  public Context setResponseHeader(@NonNull String name, @NonNull String value) {
    throw new IllegalStateException(MESSAGE);
  }

  @NonNull @Override
  public Context setResponseHeader(@NonNull String name, @NonNull Instant value) {
    throw new IllegalStateException(MESSAGE);
  }

  @NonNull @Override
  public Context setResponseLength(long length) {
    throw new IllegalStateException(MESSAGE);
  }

  @NonNull @Override
  public Context setResponseType(@NonNull String contentType) {
    throw new IllegalStateException(MESSAGE);
  }

  @NonNull @Override
  public Context setResponseType(@NonNull MediaType contentType) {
    throw new IllegalStateException(MESSAGE);
  }

  @NonNull @Override
  public Context setDefaultResponseType(@NonNull MediaType contentType) {
    throw new IllegalStateException(MESSAGE);
  }
}
