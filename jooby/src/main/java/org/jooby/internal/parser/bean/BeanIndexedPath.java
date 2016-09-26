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
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import com.google.inject.TypeLiteral;

@SuppressWarnings("rawtypes")
class BeanIndexedPath implements BeanPath {
  private int index;

  private BeanPath path;

  private Type type;

  public BeanIndexedPath(final BeanPath path, final int index, final TypeLiteral ittype) {
    this(path, index, path == null ? ittype.getType() : path.type());
  }

  public BeanIndexedPath(final BeanPath path, final int index, final Type ittype) {
    this.path = path;
    this.index = index;
    this.type = ((ParameterizedType) ittype).getActualTypeArguments()[0];
  }

  @SuppressWarnings("unchecked")
  @Override
  public void set(final Object bean, final Object... args) throws Throwable {
    List list = list(bean);
    list.add(args[0]);
  }

  @SuppressWarnings("unchecked")
  @Override
  public Object get(final Object bean, final Object... args) throws Throwable {
    List list = list(bean);
    if (index >= list.size()) {
      Object item = ((Class) settype()).newInstance();
      list.add(item);
    }
    return list.get(index);
  }

  private List list(final Object bean) throws Throwable {
    List list = (List) (path == null ? bean : path.get(bean));
    if (list == null) {
      list = new ArrayList<>();
      path.set(bean, list);
    }
    return list;
  }

  @Override
  public AnnotatedElement setelem() {
    return path.setelem();
  }

  @Override
  public Type settype() {
    return type;
  }

  @Override
  public Type type() {
    return type;
  }

  @Override
  public String toString() {
    return (path == null ? "" : path.toString()) + "[" + index + "]";
  }
}
