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

import java.util.HashMap;
import java.util.Map;

import org.jooby.MediaType;
import org.jooby.Mutant;
import org.jooby.Parser;
import org.jooby.Parser.ParamReference;
import org.jooby.Status;
import org.jooby.internal.reqparam.ParserExecutor;

import com.google.common.collect.ImmutableMap;
import com.google.inject.TypeLiteral;

/**
 * Default mutant implementation.
 *
 * NOTE: This class isn't thread-safe.
 *
 * @author edgar
 */
public class MutantImpl implements Mutant {

  private final Map<Object, Object> results = new HashMap<>(1);

  private final ParserExecutor parser;

  private Object data;

  private Status errStatus;

  private MediaType type;

  public MutantImpl(final ParserExecutor parser, final MediaType type, final Object data,
      final Status errStatus) {
    this.parser = parser;
    this.type = type;
    this.data = data;
    this.errStatus = errStatus;
  }

  public MutantImpl(final ParserExecutor parser, final MediaType contentType, final Object data) {
    this(parser, contentType, data, Status.BAD_REQUEST);
  }

  public MutantImpl(final ParserExecutor parser, final Object data) {
    this(parser, MediaType.plain, data);
  }

  @Override
  public <T> T to(final TypeLiteral<T> type) {
    return to(type, this.type);
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T> T to(final TypeLiteral<T> type, final MediaType mtype) {
    T result = (T) results.get(type);
    if (result == null) {
      result = parser.convert(type, mtype, data, errStatus);
      results.put(type, result);
    }
    return result;
  }

  @SuppressWarnings("unchecked")
  @Override
  public Map<String, Mutant> toMap() {
    if (data instanceof Map) {
      return (Map<String, Mutant>) data;
    }
    if (data instanceof Parser.ParamReference) {
      Parser.ParamReference<?> param = (ParamReference<?>) data;
      return ImmutableMap.of(param.name(), this);
    }
    return ImmutableMap.of("body", this);
  }

  @SuppressWarnings("rawtypes")
  @Override
  public boolean isSet() {
    if (data instanceof ParamReferenceImpl) {
      return ((ParamReferenceImpl) data).size() > 0;
    }
    if (data instanceof BodyReferenceImpl) {
      return ((BodyReferenceImpl) data).length() > 0;
    }
    return ((Map) data).size() > 0;
  }

  @Override
  public String toString() {
    return data.toString();
  }

}
