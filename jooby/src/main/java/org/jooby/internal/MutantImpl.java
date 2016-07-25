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

import static javaslang.API.$;
import static javaslang.API.Case;
import static javaslang.API.Match;
import static javaslang.Predicates.instanceOf;

import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

import org.jooby.Err;
import org.jooby.MediaType;
import org.jooby.Mutant;
import org.jooby.Status;
import org.jooby.internal.parser.ParserExecutor;

import com.google.common.collect.ImmutableMap;
import com.google.inject.TypeLiteral;

import javaslang.Tuple;
import javaslang.Tuple3;

/**
 * Default mutant implementation.
 *
 * NOTE: This class isn't thread-safe.
 *
 * @author edgar
 */
public class MutantImpl implements Mutant {

  private static final String REQUIRED = "Required %s is not present";

  private static final String FAILURE = "Failed to parse %s to '%s'";

  private final Map<Object, Object> results = new HashMap<>(1);

  private final ParserExecutor parser;

  private Object data;

  private MediaType type;

  public MutantImpl(final ParserExecutor parser, final MediaType type, final Object data) {
    this.parser = parser;
    this.type = type;
    this.data = data;
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
      try {
        result = parser.convert(type, mtype, data);
        if (result == ParserExecutor.NO_PARSER) {
          Tuple3<String, String, Status> md = md();
          throw new Err(md._3, String.format(FAILURE, md._2, type));
        }
        results.put(type, result);
      } catch (NoSuchElementException ex) {
        Tuple3<String, String, Status> md = md();
        throw new Err.Missing(String.format(REQUIRED, md._2));
      } catch (Err ex) {
        throw ex;
      } catch (Throwable ex) {
        Tuple3<String, String, Status> md = md();
        throw new Err(parser.statusCode(ex), String.format(FAILURE, md._2, type), ex);
      }
    }
    return result;
  }

  @SuppressWarnings("unchecked")
  @Override
  public Map<String, Mutant> toMap() {
    if (data instanceof Map) {
      return (Map<String, Mutant>) data;
    }
    return ImmutableMap.of(md()._1, this);
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

  private Tuple3<String, String, Status> md() {
    return Match(data).of(
        Case(instanceOf(ParamReferenceImpl.class),
            p -> Tuple.of(p.name(), p.type() + " '" + p.name() + "'", Status.BAD_REQUEST)),
        Case(instanceOf(BodyReferenceImpl.class),
            Tuple.of("body", "body", Status.UNSUPPORTED_MEDIA_TYPE)),
        Case($(), Tuple.of("params", "parameters", Status.BAD_REQUEST)));
  }

  @Override
  public String toString() {
    return data.toString();
  }

}
