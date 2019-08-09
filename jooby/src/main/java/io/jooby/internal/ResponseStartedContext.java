/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal;

import io.jooby.AttachedFile;
import io.jooby.Context;
import io.jooby.Cookie;
import io.jooby.ForwardingContext;
import io.jooby.MediaType;
import io.jooby.Sender;
import io.jooby.SneakyThrows;
import io.jooby.StatusCode;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
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

public class ResponseStartedContext extends ForwardingContext {
  private static final String MESSAGE = "The response has already been started";

  public ResponseStartedContext(@Nonnull Context context) {
    super(context);
  }

  @Nonnull @Override public Context send(@Nonnull Path file) {
    throw new IllegalStateException(MESSAGE);
  }

  @Nonnull @Override public Context send(@Nonnull byte[] data) {
    throw new IllegalStateException(MESSAGE);
  }

  @Nonnull @Override public Context send(@Nonnull String data) {
    throw new IllegalStateException(MESSAGE);
  }

  @Nonnull @Override public Context send(@Nonnull ByteBuffer data) {
    throw new IllegalStateException(MESSAGE);
  }

  @Nonnull @Override public Context send(@Nonnull FileChannel file) {
    throw new IllegalStateException(MESSAGE);
  }

  @Nonnull @Override public Context send(@Nonnull AttachedFile file) {
    throw new IllegalStateException(MESSAGE);
  }

  @Nonnull @Override public Context send(@Nonnull InputStream input) {
    throw new IllegalStateException(MESSAGE);
  }

  @Nonnull @Override public Context send(@Nonnull StatusCode statusCode) {
    throw new IllegalStateException(MESSAGE);
  }

  @Nonnull @Override public Context send(@Nonnull ReadableByteChannel channel) {
    throw new IllegalStateException(MESSAGE);
  }

  @Nonnull @Override public Context send(@Nonnull String data, @Nonnull Charset charset) {
    throw new IllegalStateException(MESSAGE);
  }

  @Nonnull @Override public Context sendError(@Nonnull Throwable cause) {
    throw new IllegalStateException(MESSAGE);
  }

  @Nonnull @Override
  public Context sendError(@Nonnull Throwable cause, @Nonnull StatusCode statusCode) {
    throw new IllegalStateException(MESSAGE);
  }

  @Nonnull @Override public Context sendRedirect(@Nonnull String location) {
    throw new IllegalStateException(MESSAGE);
  }

  @Nonnull @Override
  public Context sendRedirect(@Nonnull StatusCode redirect, @Nonnull String location) {
    throw new IllegalStateException(MESSAGE);
  }

  @Nonnull @Override public Context render(@Nonnull Object value) {
    throw new IllegalStateException(MESSAGE);
  }

  @Nonnull @Override
  public Context responseStream(@Nonnull SneakyThrows.Consumer<OutputStream> consumer)
      throws Exception {
    throw new IllegalStateException(MESSAGE);
  }

  @Nonnull @Override public Context responseStream(@Nonnull MediaType contentType,
      @Nonnull SneakyThrows.Consumer<OutputStream> consumer) throws Exception {
    throw new IllegalStateException(MESSAGE);
  }

  @Nonnull @Override public Context responseWriter(@Nonnull MediaType contentType,
      @Nonnull SneakyThrows.Consumer<PrintWriter> consumer) throws Exception {
    throw new IllegalStateException(MESSAGE);
  }

  @Nonnull @Override
  public Context responseWriter(@Nonnull SneakyThrows.Consumer<PrintWriter> consumer)
      throws Exception {
    throw new IllegalStateException(MESSAGE);
  }

  @Nonnull @Override
  public Context responseWriter(@Nonnull MediaType contentType, @Nullable Charset charset,
      @Nonnull SneakyThrows.Consumer<PrintWriter> consumer) throws Exception {
    throw new IllegalStateException(MESSAGE);
  }

  @Nonnull @Override public OutputStream responseStream() {
    throw new IllegalStateException(MESSAGE);
  }

  @Nonnull @Override public OutputStream responseStream(@Nonnull MediaType contentType) {
    throw new IllegalStateException(MESSAGE);
  }

  @Nonnull @Override public PrintWriter responseWriter() {
    throw new IllegalStateException(MESSAGE);
  }

  @Nonnull @Override public PrintWriter responseWriter(@Nonnull MediaType contentType) {
    throw new IllegalStateException(MESSAGE);
  }

  @Nonnull @Override
  public PrintWriter responseWriter(@Nonnull MediaType contentType, @Nullable Charset charset) {
    throw new IllegalStateException(MESSAGE);
  }

  @Nonnull @Override public Sender responseSender() {
    throw new IllegalStateException(MESSAGE);
  }

  @Nonnull @Override public Context removeResponseHeader(@Nonnull String name) {
    throw new IllegalStateException(MESSAGE);
  }

  @Nonnull @Override public Context setResponseCookie(@Nonnull Cookie cookie) {
    throw new IllegalStateException(MESSAGE);
  }

  @Nonnull @Override public Context setResponseHeader(@Nonnull String name, @Nonnull Date value) {
    throw new IllegalStateException(MESSAGE);
  }

  @Nonnull @Override public Context setResponseCode(int statusCode) {
    throw new IllegalStateException(MESSAGE);
  }

  @Nonnull @Override public Context setResponseCode(@Nonnull StatusCode statusCode) {
    throw new IllegalStateException(MESSAGE);
  }

  @Nonnull @Override public Context setResponseHeader(@Nonnull String name, @Nonnull Object value) {
    throw new IllegalStateException(MESSAGE);
  }

  @Nonnull @Override public Context setResponseHeader(@Nonnull String name, @Nonnull String value) {
    throw new IllegalStateException(MESSAGE);
  }

  @Nonnull @Override
  public Context setResponseHeader(@Nonnull String name, @Nonnull Instant value) {
    throw new IllegalStateException(MESSAGE);
  }

  @Nonnull @Override public Context setResponseLength(long length) {
    throw new IllegalStateException(MESSAGE);
  }

  @Nonnull @Override public Context setResponseType(@Nonnull String contentType) {
    throw new IllegalStateException(MESSAGE);
  }

  @Nonnull @Override public Context setResponseType(@Nonnull MediaType contentType) {
    throw new IllegalStateException(MESSAGE);
  }

  @Nonnull @Override
  public Context setResponseType(@Nonnull MediaType contentType, @Nullable Charset charset) {
    throw new IllegalStateException(MESSAGE);
  }

  @Nonnull @Override public Context setDefaultResponseType(@Nonnull MediaType contentType) {
    throw new IllegalStateException(MESSAGE);
  }
}
