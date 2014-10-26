package org.jooby.internal.mvc;

import static java.util.Objects.requireNonNull;

import java.lang.reflect.Parameter;
import java.util.Optional;

import org.jooby.Cookie;
import org.jooby.Request;
import org.jooby.Response;
import org.jooby.Route;
import org.jooby.mvc.Body;
import org.jooby.mvc.Header;

import com.google.inject.TypeLiteral;

public class Param {

  private enum Strategy {
    PARAM {
      @Override
      public Object get(final Request req, final Response resp, final Param param) throws Exception {
        return req.param(param.name).to(
            TypeLiteral.get(param.parameter.getParameterizedType()));
      }
    },

    BODY {
      @Override
      public Object get(final Request req, final Response resp, final Param param) throws Exception {
        return req.body(TypeLiteral.get(param.parameter.getParameterizedType()));
      }
    },

    HEADER {
      @Override
      public Object get(final Request req, final Response resp, final Param param) throws Exception {
        return req.header(param.name).to(
            TypeLiteral.get(param.parameter.getParameterizedType()));
      }
    },

    COOKIE {
      @Override
      public Object get(final Request req, final Response resp, final Param param)
          throws Exception {
        Optional<Cookie> cookie = req.cookie(param.name);
        if (param.parameter.getType() == Optional.class) {
          return cookie;
        }
        return cookie
            .orElseThrow(
                () -> new Route.Err(Response.Status.BAD_REQUEST, "Missing cookie: " + param.name));
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

  }

  public final String name;

  public final Parameter parameter;

  private Strategy strategy;

  public Param(final String name, final Parameter parameter) {
    this.name = requireNonNull(name, "A parameter's name is required.");

    this.parameter = requireNonNull(parameter, "A parameter is required.");

    if (parameter.getAnnotation(Body.class) != null) {
      this.strategy = Strategy.BODY;
    } else if (parameter.getAnnotation(Header.class) != null) {
      strategy = Strategy.HEADER;
    } else if (parameter.getType() == Response.class) {
      strategy = Strategy.RESPONSE;
    } else if (parameter.getType() == Request.class) {
      strategy = Strategy.REQUEST;
    } else if (parameter.getType() == Cookie.class) {
      strategy = Strategy.COOKIE;
    } else {
      strategy = Strategy.PARAM;
    }
  }

  public Object get(final Request request, final Response response) throws Exception {
    return strategy.get(request, response, this);
  }

  @Override
  public String toString() {
    return parameter.getType() + " " + name;
  }

}
