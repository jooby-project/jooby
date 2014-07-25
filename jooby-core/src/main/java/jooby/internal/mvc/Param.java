package jooby.internal.mvc;

import static java.util.Objects.requireNonNull;

import java.lang.reflect.Parameter;
import java.util.Optional;

import jooby.Cookie;
import jooby.HttpException;
import jooby.HttpStatus;
import jooby.Request;
import jooby.mvc.Body;
import jooby.mvc.Header;

import com.google.inject.TypeLiteral;

public class Param {

  private enum Strategy {
    PARAM {
      @Override
      public Object get(final Request request, final Param param) throws Exception {
        return request.param(param.name).get(
            TypeLiteral.get(param.parameter.getParameterizedType()));
      }
    },

    BODY {
      @Override
      public Object get(final Request request, final Param param) throws Exception {
        return request.body(TypeLiteral.get(param.parameter.getParameterizedType()));
      }
    },

    HEADER {
      @Override
      public Object get(final Request request, final Param param) throws Exception {
        return request.header(param.name).get(
            TypeLiteral.get(param.parameter.getParameterizedType()));
      }
    },

    COOKIE {
      @Override
      public Object get(final Request request, final Param param) throws Exception {
        Cookie cookie = request.cookie(param.name);
        if (param.parameter.getType() == Optional.class) {
          return Optional.ofNullable(cookie);
        }
        return Optional.ofNullable(cookie)
            .orElseThrow(
                () -> new HttpException(HttpStatus.BAD_REQUEST, "Missing cookie: " + param.name));
      }
    };

    public abstract Object get(final Request request, final Param param) throws Exception;

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
    } else {
      // param or cookie
      if (parameter.getType() == Cookie.class) {
        strategy = Strategy.COOKIE;
      } else {
        strategy = Strategy.PARAM;
      }
    }
  }

  public Object get(final Request request) throws Exception {
    return strategy.get(request, this);
  }

  @Override
  public String toString() {
    return parameter.getType() + " " + name;
  }

}
