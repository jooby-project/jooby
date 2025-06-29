/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby;

import java.util.function.BiFunction;

import io.jooby.value.Value;

/**
 * List of possible parameter sources supported by {@link Context#lookup(String, ParamSource...)}.
 *
 * @see io.jooby.annotation.Param
 * @see Context#lookup(String, ParamSource...)
 */
public enum ParamSource {

  /** Source equivalent to {@link Context#path(String)}. */
  PATH(Context::path),

  /** Source equivalent to {@link Context#header(String)}. */
  HEADER(Context::header),

  /** Source equivalent to {@link Context#cookie(String)}. */
  COOKIE(Context::cookie),

  /** Source equivalent to {@link Context#flash(String)}. */
  FLASH(Context::flash),

  /** Source equivalent to {@link Context#session(String)}. */
  SESSION(Context::session),

  /** Source equivalent to {@link Context#query(String)}. */
  QUERY(Context::query),

  /** Source equivalent to {@link Context#form(String)}. */
  FORM(Context::form);

  final BiFunction<Context, String, Value> provider;

  ParamSource(BiFunction<Context, String, Value> provider) {
    this.provider = provider;
  }
}
