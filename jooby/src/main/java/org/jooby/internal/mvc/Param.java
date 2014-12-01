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

import static java.util.Objects.requireNonNull;

import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Optional;

import org.jooby.Cookie;
import org.jooby.Err;
import org.jooby.Mutant;
import org.jooby.Request;
import org.jooby.Response;
import org.jooby.Status;
import org.jooby.Upload;
import org.jooby.Verb;
import org.jooby.mvc.Header;

import com.google.inject.TypeLiteral;

public class Param {

  private enum Strategy {
    PARAM {
      @Override
      public Object get(final Request req, final Response resp, final Param md) throws Exception {
        Mutant param = req.param(md.name);
        TypeLiteral<?> type = TypeLiteral.get(md.parameter.getParameterizedType());
        /**
         * If param is present or ask for an Upload, use/return the param version
         */
        if (param.isPresent()) {
          return param.to(type);
        } else {
          if (typeIs(md.parameter.getParameterizedType(), Upload.class)) {
            return param.to(type);
          }
          if (typeIs(md.parameter.getParameterizedType(), Cookie.class)) {
            Optional<Cookie> cookie = req.cookie(md.name);
            if (md.parameter.getType() == Optional.class) {
              return cookie;
            }
            return cookie
                .orElseThrow(() -> new Err(Status.BAD_REQUEST, "Cookie not found: " + md.name));
          }
          /**
           * If param is missing, check if this is a post/put and delegates to body
           */
          if (req.verb().is(Verb.POST, Verb.PUT)) {
            // assume body is required
            return req.body(type);
          } else {
            // just fail
            return param.to(type);
          }

        }
      }
    },

    HEADER {
      @Override
      public Object get(final Request req, final Response resp, final Param param) throws Exception {
        return req.header(param.name).to(
            TypeLiteral.get(param.parameter.getParameterizedType()));
      }
    },

    REQUEST {
      @Override
      public Object get(final Request request, final Response resp, final Param param)
          throws Exception {
        return request;
      }
    },

    RESPONSE {
      @Override
      public Object get(final Request request, final Response resp, final Param param)
          throws Exception {
        return resp;
      }
    };

    public abstract Object get(final Request req, Response resp, final Param param)
        throws Exception;

    protected boolean typeIs(final Type type, final Class<?> target) {
      if (type == target) {
        return true;
      }
      if (type instanceof ParameterizedType) {
        ParameterizedType parameterizedType = (ParameterizedType) type;
        Type[] args = parameterizedType.getActualTypeArguments();
        if (args == null || args.length == 0) {
          return false;
        }
        return typeIs(args[0], target);
      }
      return false;
    }
  }

  public final String name;

  public final Parameter parameter;

  private Strategy strategy;

  public Param(final String name, final Parameter parameter) {
    this.name = requireNonNull(name, "A parameter's name is required.");

    this.parameter = requireNonNull(parameter, "A parameter is required.");

    if (parameter.getAnnotation(Header.class) != null) {
      strategy = Strategy.HEADER;
    } else if (parameter.getType() == Response.class) {
      strategy = Strategy.RESPONSE;
    } else if (parameter.getType() == Request.class) {
      strategy = Strategy.REQUEST;
    } else {
      strategy = Strategy.PARAM;
    }
  }

  public Object get(final Request request, final Response response) throws Exception {
    return strategy.get(request, response, this);
  }

}
