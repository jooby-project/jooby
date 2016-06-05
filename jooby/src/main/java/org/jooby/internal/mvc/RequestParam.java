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
package org.jooby.internal.mvc;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.Optional;

import javax.inject.Named;

import org.jooby.Cookie;
import org.jooby.Err;
import org.jooby.Mutant;
import org.jooby.Request;
import org.jooby.Response;
import org.jooby.Session;
import org.jooby.mvc.Body;
import org.jooby.mvc.Header;
import org.jooby.mvc.Local;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.google.inject.TypeLiteral;
import com.google.inject.util.Types;

@SuppressWarnings({"rawtypes", "unchecked" })
public class RequestParam {

  private interface GetValue {

    Object apply(Request req, Response rsp, RequestParam param) throws Exception;

  }

  private static final TypeLiteral<Header> headerType = TypeLiteral.get(Header.class);

  private static final TypeLiteral<Body> bodyType = TypeLiteral.get(Body.class);

  private static final TypeLiteral<Local> localType = TypeLiteral.get(Local.class);

  private static final Map<Object, GetValue> injector;

  static {
    Builder<Object, GetValue> builder = ImmutableMap.<Object, GetValue> builder();
    /**
     * Body
     */
    builder.put(bodyType, (req, rsp, param) -> req.body().to(param.type));
    /**
     * Request
     */
    builder.put(TypeLiteral.get(Request.class), (req, rsp, param) -> req);
    /**
     * Response
     */
    builder.put(TypeLiteral.get(Response.class), (req, rsp, param) -> rsp);
    /**
     * Session
     */
    builder.put(TypeLiteral.get(Session.class), (req, rsp, param) -> req.session());
    builder.put(TypeLiteral.get(Types.newParameterizedType(Optional.class, Session.class)),
        (req, rsp, param) -> req.ifSession());
    /**
     * Cookie
     */
    builder.put(TypeLiteral.get(Cookie.class), (req, rsp, param) -> req.cookies().stream()
        .filter(c -> c.name().equalsIgnoreCase(param.name)).findFirst().get());
    builder.put(TypeLiteral.get(Types.listOf(Cookie.class)), (req, rsp, param) -> req.cookies());
    builder.put(TypeLiteral.get(Types.newParameterizedType(Optional.class, Cookie.class)),
        (req, rsp, param) -> req.cookies().stream()
            .filter(c -> c.name().equalsIgnoreCase(param.name)).findFirst());
    /**
     * Header
     */
    builder.put(headerType, (req, rsp, param) -> req.header(param.name).to(param.type));

    /**
     * Local
     */
    builder.put(localType, (req, rsp, param) -> {
      if (param.type.getRawType() == Map.class) {
        return req.attributes();
      }
      Optional local = req.ifGet(param.name);
      if (param.optional) {
        return local;
      }
      return local.get();
    });

    injector = builder.build();
  }

  public final String name;

  public final TypeLiteral type;

  private final GetValue strategy;

  private boolean optional;

  public RequestParam(final Parameter parameter, final String name) {
    this(parameter, name, parameter.getParameterizedType());
  }

  public RequestParam(final AnnotatedElement elem, final String name, final Type type) {
    this.name = name;
    this.type = TypeLiteral.get(type);
    this.optional = this.type.getRawType() == Optional.class;
    final TypeLiteral strategyType;
    if (elem.getAnnotation(Header.class) != null) {
      strategyType = headerType;
    } else if (elem.getAnnotation(Body.class) != null) {
      strategyType = bodyType;
    } else if (elem.getAnnotation(Local.class) != null) {
      strategyType = localType;
    } else {
      strategyType = this.type;
    }
    this.strategy = injector.getOrDefault(strategyType, param());
  }

  public Object value(final Request req, final Response rsp) throws Throwable {
    return strategy.apply(req, rsp, this);
  }

  public static String nameFor(final Parameter param) {
    String name = findName(param);
    return name == null ? (param.isNamePresent() ? param.getName() : null) : name;
  }

  private static String findName(final AnnotatedElement elem) {
    Named named = elem.getAnnotation(Named.class);
    if (named == null) {
      com.google.inject.name.Named gnamed = elem
          .getAnnotation(com.google.inject.name.Named.class);
      if (gnamed == null) {
        Header header = elem.getAnnotation(Header.class);
        if (header == null) {
          return null;
        }
        return Strings.emptyToNull(header.value());
      }
      return gnamed.value();
    }
    return Strings.emptyToNull(named.value());
  }

  private static final GetValue param() {
    return (req, rsp, param) -> {
      Mutant mutant = req.param(param.name);
      if (mutant.isSet() || param.optional) {
        return mutant.to(param.type);
      }
      try {
        return req.params().to(param.type);
      } catch (Err ex) {
        // force parsing
        return mutant.to(param.type);
      }
    };
  }

}
