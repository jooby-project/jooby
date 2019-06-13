/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal;

import io.jooby.Context;
import io.jooby.Cookie;
import io.jooby.FlashMap;
import io.jooby.Value;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

import static io.jooby.Cookie.decode;
import static java.util.Collections.emptyMap;

public class FlashMapImpl extends HashMap<String, String> implements FlashMap {

  private Context ctx;

  private boolean keep;

  private Cookie template;

  private Map<String, String> initialScope;

  public FlashMapImpl(Context ctx, Cookie template) {
    Value cookie = ctx.cookie(template.getName());
    Map<String, String> seed = cookie.isMissing() ? emptyMap() : decode(cookie.value());
    super.putAll(seed);
    this.ctx = ctx;
    this.template = template;
    this.initialScope = seed;
    if (seed.size() > 0) {
      Cookie sync = toCookie();
      if (sync != null) {
        ctx.setResponseCookie(sync);
      }
    }
  }

  @Override public FlashMap keep() {
    if (size() > 0) {
      Cookie cookie = this.template.clone().setValue(Cookie.encode(this));
      ctx.setResponseCookie(cookie);
    }
    return this;
  }

  private Cookie toCookie() {
    // 1. no change detect
    if (this.equals(initialScope)) {
      // 1.a. existing data available, discard
      if (this.size() > 0) {
        return template.clone().setMaxAge(0);
      }
    } else {
      // 2. change detected
      if (this.size() == 0) {
        // 2.a everything was removed from app logic
        return template.clone().setMaxAge(0);
      } else {
        // 2.b there is something to see in the next request
        return template.clone().setValue(Cookie.encode(this));
      }
    }
    return null;
  }

  private void syncCookie() {
    Cookie cookie = toCookie();
    if (cookie != null) {
      ctx.setResponseCookie(cookie);
    }
  }

  @Override public String compute(String key,
      BiFunction<? super String, ? super String, ? extends String> remappingFunction) {
    String result = super.compute(key, remappingFunction);
    syncCookie();
    return result;
  }

  @Override public String computeIfAbsent(String key,
      Function<? super String, ? extends String> mappingFunction) {
    String result = super.computeIfAbsent(key, mappingFunction);
    syncCookie();
    return result;
  }

  @Override public String computeIfPresent(String key,
      BiFunction<? super String, ? super String, ? extends String> remappingFunction) {
    String result = super.computeIfPresent(key, remappingFunction);
    syncCookie();
    return result;
  }

  @Override public String merge(String key, String value,
      BiFunction<? super String, ? super String, ? extends String> remappingFunction) {
    String result = super.merge(key, value, remappingFunction);
    syncCookie();
    return result;
  }

  @Override public String put(String key, String value) {
    String result = super.put(key, value);
    syncCookie();
    return result;
  }

  @Override public String putIfAbsent(String key, String value) {
    String result = super.putIfAbsent(key, value);
    syncCookie();
    return result;
  }

  @Override public void putAll(Map<? extends String, ? extends String> m) {
    super.putAll(m);
    syncCookie();
  }

  @Override public boolean remove(Object key, Object value) {
    boolean result = super.remove(key, value);
    syncCookie();
    return result;
  }

  @Override public String remove(Object key) {
    String result = super.remove(key);
    syncCookie();
    return result;
  }

  @Override public boolean replace(String key, String oldValue, String newValue) {
    boolean result = super.replace(key, oldValue, newValue);
    syncCookie();
    return result;
  }

  @Override public String replace(String key, String value) {
    String result = super.replace(key, value);
    syncCookie();
    return result;
  }

  @Override
  public void replaceAll(BiFunction<? super String, ? super String, ? extends String> function) {
    super.replaceAll(function);
    syncCookie();
  }

}

