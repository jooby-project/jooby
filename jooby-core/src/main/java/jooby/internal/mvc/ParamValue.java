package jooby.internal.mvc;

import static java.util.Objects.requireNonNull;

import java.util.Optional;

import jooby.Cookie;
import jooby.HttpException;
import jooby.HttpStatus;
import jooby.Request;
import jooby.mvc.Body;
import jooby.mvc.Header;

import com.google.inject.TypeLiteral;

class ParamValue {

  private interface Strategy {
    Object get(Request request, ParamDef param) throws Exception;
  }

  private enum AnnotationStrategy implements Strategy {
    PARAM {
      @Override
      public Object get(final Request request, final ParamDef param) throws Exception {
        return request.param(param.name()).get(TypeLiteral.get(param.parameterizedType()));
      }
    },


    BODY {
      @Override
      public Object get(final Request request, final ParamDef param) throws Exception {
        return request.body(param.type());
      }
    },

    HEADER {
      @Override
      public Object get(final Request request, final ParamDef param) throws Exception {
        return request.header(param.name()).get(TypeLiteral.get(param.parameterizedType()));
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
            .orElseThrow(
                () -> new HttpException(HttpStatus.BAD_REQUEST, "Missing cookie: " + param.name()));
      }
    };
  }

  private ParamDef parameter;

  private Strategy strategy;

  public ParamValue(final ParamDef parameter) {
    this.parameter = requireNonNull(parameter, "A parameter is required.");

    if (parameter.hasAnnotation(Body.class)) {
      strategy = AnnotationStrategy.BODY;
    } else if (parameter.hasAnnotation(Header.class)) {
      strategy = AnnotationStrategy.HEADER;
    } else {
      // param or cookie
      if (parameter.type() == Cookie.class) {
        strategy = AnnotationStrategy.COOKIE;
      } else {
        strategy = AnnotationStrategy.PARAM;
      }
    }
  }

  public Object get(final Request request) throws Exception {
    return strategy.get(request, parameter);
  }

}
