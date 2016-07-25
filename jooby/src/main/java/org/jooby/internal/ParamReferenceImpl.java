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
package org.jooby.internal;

import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import org.jooby.Parser;

public class ParamReferenceImpl<T> implements Parser.ParamReference<T> {

  private String type;

  private String name;

  private List<T> values;

  public ParamReferenceImpl(final String type, final String name, final List<T> values) {
    this.type = type;
    this.name = name;
    this.values = values;
  }

  @Override
  public String type() {
    return type;
  }

  @Override
  public String name() {
    return name;
  }

  @Override
  public T first() {
    return get(0);
  }

  @Override
  public T last() {
    return get(values.size() - 1);
  }

  @Override
  public T get(final int index) {
    if (index >= 0 && index < values.size()) {
      return values.get(index);
    }
    throw new NoSuchElementException(name);
  }

  @Override
  public Iterator<T> iterator() {
    return values.iterator();
  }

  @Override
  public int size() {
    return values.size();
  }

  @Override
  public String toString() {
    return values.toString();
  }

}
