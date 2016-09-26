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
package org.jooby.internal.parser.bean;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.lang.reflect.Type;

class BeanMethodPath implements BeanPath {
  private String path;

  private Method method;

  BeanPath setter;

  public BeanMethodPath(final String path, final Method method) {
    this.path = path;
    this.method = method;
    this.method.setAccessible(true);
  }

  @Override
  public void set(final Object bean, final Object... args)
      throws Throwable {
    if (setter != null) {
      setter.set(bean, args);
    } else {
      method.invoke(bean, args);
    }
  }

  @Override
  public Object get(final Object bean, final Object... args) throws Throwable {
    return method.invoke(bean, args);
  }

  @Override
  public Type settype() {
    if (method.getParameterCount() == 0) {
      return method.getGenericReturnType();
    }
    return method.getGenericParameterTypes()[0];
  }

  @Override
  public Type type() {
    return method.getGenericReturnType();
  }

  @Override
  public AnnotatedElement setelem() {
    return method;
  }

  @Override
  public String toString() {
    return path;
  }
}