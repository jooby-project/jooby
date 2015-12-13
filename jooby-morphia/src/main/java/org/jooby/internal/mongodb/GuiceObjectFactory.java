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
package org.jooby.internal.mongodb;

import static java.util.Objects.requireNonNull;

import java.lang.reflect.Constructor;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.mongodb.morphia.Morphia;
import org.mongodb.morphia.ObjectFactory;
import org.mongodb.morphia.mapping.MappedField;
import org.mongodb.morphia.mapping.Mapper;
import org.mongodb.morphia.mapping.MapperOptions;

import com.google.inject.Injector;
import com.mongodb.DBObject;

public class GuiceObjectFactory implements ObjectFactory {

  private Injector injector;

  private ObjectFactory delegate;

  @Inject
  public GuiceObjectFactory(final Injector injector, final Morphia morphia) {
    this.injector = requireNonNull(injector, "Injector is required.");
    MapperOptions options = morphia.getMapper().getOptions();
    this.delegate = options.getObjectFactory();
    options.setObjectFactory(this);
  }

  @Override
  public <T> T createInstance(final Class<T> clazz) {
    if (shouldInject(clazz)) {
      return injector.getInstance(clazz);
    }
    return delegate.createInstance(clazz);
  }

  @Override
  public <T> T createInstance(final Class<T> clazz, final DBObject dbObj) {
    if (shouldInject(clazz)) {
      return injector.getInstance(clazz);
    }
    return delegate.createInstance(clazz, dbObj);
  }

  @Override
  public Object createInstance(final Mapper mapper, final MappedField mf, final DBObject dbObj) {
    Class<?> clazz = mf.getType();
    if (shouldInject(clazz)) {
      return injector.getInstance(clazz);
    }
    return delegate.createInstance(mapper, mf, dbObj);
  }

  @Override
  public Map<?, ?> createMap(final MappedField mf) {
    return delegate.createMap(mf);
  }

  @Override
  public List<?> createList(final MappedField mf) {
    return delegate.createList(mf);
  }

  @Override
  public Set<?> createSet(final MappedField mf) {
    return delegate.createSet(mf);
  }

  private boolean shouldInject(final Class<?> clazz) {
    for(Constructor<?> constructor : clazz.getDeclaredConstructors()) {
      if (constructor.getAnnotation(Inject.class) != null) {
        return true;
      }
    }
    return false;
  }
}
