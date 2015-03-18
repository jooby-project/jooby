package org.jooby.internal.ftl;

import static java.util.Objects.requireNonNull;

import com.google.common.cache.Cache;

import freemarker.cache.CacheStorage;

public class GuavaCacheStorage implements CacheStorage {

  private Cache<Object, Object> cache;

  public GuavaCacheStorage(final Cache<Object, Object> cache) {
    this.cache = requireNonNull(cache, "Cache is required.");
  }

  @Override
  public Object get(final Object key) {
    return cache.getIfPresent(key);
  }

  @Override
  public void put(final Object key, final Object value) {
    cache.put(key, value);

  }

  @Override
  public void remove(final Object key) {
    cache.invalidate(key);
  }

  @Override
  public void clear() {
    cache.invalidateAll();
  }

}
