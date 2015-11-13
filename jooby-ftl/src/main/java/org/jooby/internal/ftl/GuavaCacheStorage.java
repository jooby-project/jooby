/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
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
