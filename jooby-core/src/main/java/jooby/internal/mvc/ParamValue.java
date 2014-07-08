package jooby.internal.mvc;

import static java.util.Objects.requireNonNull;

import java.util.Optional;

import javax.inject.Named;

import jooby.Cookie;
import jooby.HttpException;
import jooby.HttpStatus;
import jooby.Request;
import jooby.internal.MutableCookie;
import jooby.mvc.Body;
import jooby.mvc.Header;

class ParamValue {

  private interface Strategy {
    Object get(Request request, ParamDef param) throws Exception;
  }

  private enum AnnotationStrategy implements Strategy {
    PARAM {
      @Override
      public Object get(final Request request, final ParamDef param) throws Exception {
        return request.param(param.name(), param.parameterizedType());
      }
    },

    BODY {
      @Override
      public Object get(final Request request, final ParamDef param) throws Exception {
        return request.get(param.type());
      }
    },

    HEADER {
      @Override
      public Object get(final Request request, final ParamDef param) throws Exception {
        Optional<Header> header = param.getAnnotation(Header.class);
        String name = header.get().value();
        return request.header(name.isEmpty() ? param.name() : name, param.parameterizedType());
      }
    },

    COOKIE {
      @Override
      public Object get(final Request request, final ParamDef param) throws Exception {
        Cookie cookie = request.cookie(param.name());
        if (param.type() == Optional.class) {
          return Optional.ofNullable(cookie);
        }
        return Optional.ofNullable(cookie)
            .orElseThrow(() -> new HttpException(HttpStatus.BAD_REQUEST));
      }
    };
  }

  private ParamDef parameter;

  private Strategy strategy;

  public ParamValue(final ParamDef parameter) {
    this.parameter = requireNonNull(parameter, "A parameter is required.");

    if (parameter.annotations().size() == 0 || parameter.hasAnnotation(Named.class)) {
      // param or cookie
      if (parameter.typeIs(MutableCookie.class)) {
        strategy = AnnotationStrategy.COOKIE;
      } else {
        strategy = AnnotationStrategy.PARAM;
      }
    } else if (parameter.hasAnnotation(Body.class)) {
      strategy = AnnotationStrategy.BODY;
    } else if (parameter.hasAnnotation(Header.class)) {
      strategy = AnnotationStrategy.HEADER;
    }
  }

  public Object get(final Request request) throws Exception {
    return strategy.get(request, parameter);
  }

}
