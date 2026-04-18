/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.validation;

import java.io.InputStream;
import java.lang.reflect.Type;
import java.nio.channels.ReadableByteChannel;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.jspecify.annotations.Nullable;

import io.jooby.*;
import io.jooby.value.ConversionHint;
import io.jooby.value.Value;

/**
 * Wrap a context and run {@link BeanValidator#validate(Context, Object)} over HTTP request objects.
 *
 * @author edgar
 * @since 3.1.1
 */
public class ValidationContext extends ForwardingContext {

  private static class ValidatedValue extends ForwardingValue {
    protected final Context ctx;

    public ValidatedValue(Context ctx, Value delegate) {
      super(delegate);
      this.ctx = ctx;
    }

    @Override
    public <T> T to(Class<T> type) {
      return validate(type);
    }

    protected <T> T validate(Class<T> type) {
      // Call empty version to let bean validator to run
      return BeanValidator.apply(
          ctx, ctx.getValueFactory().convert(type, this, ConversionHint.Empty));
    }

    @Nullable @Override
    public <T> T toNullable(Class<T> type) {
      return validate(type);
    }

    @Override
    public <T> List<T> toList(Class<T> type) {
      return BeanValidator.apply(ctx, super.toList(type));
    }

    @Override
    public <T> Set<T> toSet(Class<T> type) {
      return BeanValidator.apply(ctx, super.toSet(type));
    }
  }

  private static class ValidatedBody extends ValidatedValue implements Body {
    public ValidatedBody(Context ctx, Body body) {
      super(ctx, body);
    }

    @Override
    public byte[] bytes() {
      return ((Body) delegate).bytes();
    }

    @Override
    public boolean isInMemory() {
      return ((Body) delegate).isInMemory();
    }

    @Override
    public long getSize() {
      return ((Body) delegate).getSize();
    }

    @Override
    public ReadableByteChannel channel() {
      return ((Body) delegate).channel();
    }

    @Override
    public InputStream stream() {
      return ((Body) delegate).stream();
    }

    @Override
    public <T> T to(Type type) {
      // Call nullable version to let bean validator to run
      return BeanValidator.apply(ctx, ((Body) delegate).toNullable(type));
    }

    @Override
    public <T> T to(Class<T> type) {
      // Call nullable version to let bean validator to run
      return BeanValidator.apply(ctx, ((Body) delegate).toNullable(type));
    }

    @Nullable @Override
    public <T> T toNullable(Type type) {
      return BeanValidator.apply(ctx, ((Body) delegate).toNullable(type));
    }
  }

  private static class ValidatedQueryString extends ValidatedValue implements QueryString {
    public ValidatedQueryString(Context ctx, QueryString delegate) {
      super(ctx, delegate);
    }

    @Override
    public <T> T toEmpty(Class<T> type) {
      return validate(type);
    }

    @Override
    public String queryString() {
      return ((QueryString) delegate).queryString();
    }
  }

  private static class ValidatedFormdata extends ValidatedValue implements Formdata {
    public ValidatedFormdata(Context ctx, Formdata delegate) {
      super(ctx, delegate);
    }

    @Override
    public void put(String path, Value value) {
      ((Formdata) delegate).put(path, value);
    }

    @Override
    public void put(String path, String value) {
      ((Formdata) delegate).put(path, value);
    }

    @Override
    public void put(String path, Collection<String> values) {
      ((Formdata) delegate).put(path, values);
    }

    @Override
    public void put(String name, FileUpload file) {
      ((Formdata) delegate).put(name, file);
    }

    @Override
    public List<FileUpload> files() {
      return ((Formdata) delegate).files();
    }

    @Override
    public List<FileUpload> files(String name) {
      return ((Formdata) delegate).files(name);
    }

    @Override
    public FileUpload file(String name) {
      return ((Formdata) delegate).file(name);
    }
  }

  /**
   * Creates a new forwarding context.
   *
   * @param context Source context.
   */
  public ValidationContext(Context context) {
    super(context);
  }

  @Override
  public <T> T body(Type type) {
    return body().to(type);
  }

  @Override
  public <T> T body(Class<T> type) {
    return body().to(type);
  }

  @Override
  public Value path() {
    return new ValidatedValue(ctx, super.path());
  }

  @Override
  public Body body() {
    return new ValidatedBody(ctx, super.body());
  }

  @Override
  public <T> T query(Class<T> type) {
    return query().toEmpty(type);
  }

  @Override
  public QueryString query() {
    return new ValidatedQueryString(ctx, super.query());
  }

  @Override
  public <T> T form(Class<T> type) {
    return form().to(type);
  }

  @Override
  public Formdata form() {
    return new ValidatedFormdata(ctx, super.form());
  }

  @Override
  public Value header() {
    return new ValidatedValue(ctx, super.header());
  }
}
