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

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
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

    @NonNull @Override
    public <T> T to(@NonNull Class<T> type) {
      return validate(type);
    }

    protected <T> T validate(@NonNull Class<T> type) {
      // Call empty version to let bean validator to run
      return BeanValidator.apply(
          ctx, ctx.getValueFactory().convert(type, this, ConversionHint.Empty));
    }

    @Nullable @Override
    public <T> T toNullable(@NonNull Class<T> type) {
      return validate(type);
    }

    @NonNull @Override
    public <T> List<T> toList(@NonNull Class<T> type) {
      return BeanValidator.apply(ctx, super.toList(type));
    }

    @NonNull @Override
    public <T> Set<T> toSet(@NonNull Class<T> type) {
      return BeanValidator.apply(ctx, super.toSet(type));
    }
  }

  private static class ValidatedBody extends ValidatedValue implements Body {
    public ValidatedBody(Context ctx, Body body) {
      super(ctx, body);
    }

    @NonNull @Override
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

    @NonNull @Override
    public ReadableByteChannel channel() {
      return ((Body) delegate).channel();
    }

    @NonNull @Override
    public InputStream stream() {
      return ((Body) delegate).stream();
    }

    @NonNull @Override
    public <T> T to(@NonNull Type type) {
      // Call nullable version to let bean validator to run
      return BeanValidator.apply(ctx, ((Body) delegate).toNullable(type));
    }

    @NonNull @Override
    public <T> T to(@NonNull Class<T> type) {
      // Call nullable version to let bean validator to run
      return BeanValidator.apply(ctx, ((Body) delegate).toNullable(type));
    }

    @Nullable @Override
    public <T> T toNullable(@NonNull Type type) {
      return BeanValidator.apply(ctx, ((Body) delegate).toNullable(type));
    }
  }

  private static class ValidatedQueryString extends ValidatedValue implements QueryString {
    public ValidatedQueryString(Context ctx, QueryString delegate) {
      super(ctx, delegate);
    }

    @Override
    public @NonNull <T> T toEmpty(@NonNull Class<T> type) {
      return validate(type);
    }

    @NonNull @Override
    public String queryString() {
      return ((QueryString) delegate).queryString();
    }
  }

  private static class ValidatedFormdata extends ValidatedValue implements Formdata {
    public ValidatedFormdata(Context ctx, Formdata delegate) {
      super(ctx, delegate);
    }

    @Override
    public void put(@NonNull String path, @NonNull Value value) {
      ((Formdata) delegate).put(path, value);
    }

    @Override
    public void put(@NonNull String path, @NonNull String value) {
      ((Formdata) delegate).put(path, value);
    }

    @Override
    public void put(@NonNull String path, @NonNull Collection<String> values) {
      ((Formdata) delegate).put(path, values);
    }

    @Override
    public void put(@NonNull String name, @NonNull FileUpload file) {
      ((Formdata) delegate).put(name, file);
    }

    @NonNull @Override
    public List<FileUpload> files() {
      return ((Formdata) delegate).files();
    }

    @NonNull @Override
    public List<FileUpload> files(@NonNull String name) {
      return ((Formdata) delegate).files(name);
    }

    @NonNull @Override
    public FileUpload file(@NonNull String name) {
      return ((Formdata) delegate).file(name);
    }
  }

  /**
   * Creates a new forwarding context.
   *
   * @param context Source context.
   */
  public ValidationContext(@NonNull Context context) {
    super(context);
  }

  @NonNull @Override
  public <T> T body(@NonNull Type type) {
    return body().to(type);
  }

  @NonNull @Override
  public <T> T body(@NonNull Class<T> type) {
    return body().to(type);
  }

  @NonNull @Override
  public Value path() {
    return new ValidatedValue(ctx, super.path());
  }

  @NonNull @Override
  public Body body() {
    return new ValidatedBody(ctx, super.body());
  }

  @NonNull @Override
  public <T> T query(@NonNull Class<T> type) {
    return query().toEmpty(type);
  }

  @NonNull @Override
  public QueryString query() {
    return new ValidatedQueryString(ctx, super.query());
  }

  @NonNull @Override
  public <T> T form(@NonNull Class<T> type) {
    return form().to(type);
  }

  @NonNull @Override
  public Formdata form() {
    return new ValidatedFormdata(ctx, super.form());
  }

  @NonNull @Override
  public Value header() {
    return new ValidatedValue(ctx, super.header());
  }
}
