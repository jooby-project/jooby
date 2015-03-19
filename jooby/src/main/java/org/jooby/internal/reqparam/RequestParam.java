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
package org.jooby.internal.reqparam;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.Optional;

import javax.inject.Named;

import org.jooby.Cookie;
import org.jooby.Mutant;
import org.jooby.Request;
import org.jooby.Response;
import org.jooby.Session;
import org.jooby.mvc.Header;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.inject.TypeLiteral;
import com.google.inject.util.Types;

@SuppressWarnings({"rawtypes", "unchecked" })
public class RequestParam {

  private interface GetValue {

    Object apply(Request req, Response rsp, RequestParam param) throws Exception;

  }

  private static final TypeLiteral<Header> headerType = TypeLiteral.get(Header.class);

  private static final Map<Object, GetValue> injector = ImmutableMap.<Object, GetValue> builder()
      /**
       * Request
       */
      .put(TypeLiteral.get(Request.class), (req, rsp, param) -> req)
      /**
       * Response
       */
      .put(TypeLiteral.get(Response.class), (req, rsp, param) -> rsp)
      /**
       * Session
       */
      .put(TypeLiteral.get(Session.class), (req, rsp, param) -> req.session())
      .put(TypeLiteral.get(Types.newParameterizedType(Optional.class, Session.class)),
          (req, rsp, param) -> req.ifSession()
      )
      /**
       * Cookie
       */
      .put(TypeLiteral.get(Cookie.class), (req, rsp, param) -> req.cookie(param.name).get())
      .put(TypeLiteral.get(Types.listOf(Cookie.class)), (req, rsp, param) -> req.cookies())
      .put(TypeLiteral.get(Types.newParameterizedType(Optional.class, Cookie.class)),
          (req, rsp, param) -> req.cookie(param.name)
      )
      /**
       * Header
       */
      .put(headerType, (req, rsp, param) -> req.header(param.name).to(param.type))
      .build();

  public final String name;

  public final TypeLiteral type;

  private final GetValue strategy;

  private final boolean optional;

  public RequestParam(final Field field) {
    this(field, field.getName(), field.getGenericType());
  }

  public RequestParam(final Parameter parameter, final String name) {
    this(parameter, name, parameter.getParameterizedType());
  }

  public RequestParam(final AnnotatedElement elem, final String name, final Type type) {
    this.name = name;
    this.type = TypeLiteral.get(type);
    this.optional = this.type.getRawType() == Optional.class;
    this.strategy = injector.getOrDefault(Optional.ofNullable(elem.getAnnotation(Header.class))
        .map(header -> headerType)
        .orElse(this.type), param());
  }

  public Object value(final Request req, final Response rsp) throws Exception {
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
      Mutant value = req.param(param.name);
      if (value.isPresent() || param.optional) {
        return value.to(param.type);
      } else {
        String method = req.method();
        if (method.equals("POST") || method.equals("PUT")) {
          return req.body(param.type);
        } else {
          return req.params(param.type.getRawType());
        }
      }
    };
  }

}
