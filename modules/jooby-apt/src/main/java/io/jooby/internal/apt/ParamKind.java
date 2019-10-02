/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.apt;

import io.jooby.Context;
import io.jooby.Formdata;
import io.jooby.Multipart;
import io.jooby.apt.Annotations;
import io.jooby.internal.apt.asm.ContextParamWriter;
import io.jooby.internal.apt.asm.NamedParamWriter;
import io.jooby.internal.apt.asm.BodyWriter;
import io.jooby.internal.apt.asm.FileUploadWriter;
import io.jooby.internal.apt.asm.ObjectTypeWriter;
import io.jooby.internal.apt.asm.ParamWriter;

import javax.lang.model.type.TypeMirror;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Set;

public enum ParamKind {
  TYPE {
    @Override public Method valueObject(ParamDefinition param) {
      throw new UnsupportedOperationException(param.toString());
    }

    @Override public ParamWriter newWriter() {
      return new ObjectTypeWriter();
    }
  },

  FILE_UPLOAD {
    @Override public Method valueObject(ParamDefinition param) throws NoSuchMethodException {
      throw new UnsupportedOperationException(param.toString());
    }

    @Override public ParamWriter newWriter() {
      return new FileUploadWriter();
    }
  },

  PATH_PARAM {
    @Override public Set<String> annotations() {
      return Annotations.PATH_PARAMS;
    }

    @Override public Method valueObject(ParamDefinition param) throws NoSuchMethodException {
      return Context.class.getDeclaredMethod("path");
    }

    @Override public Method singleValue(ParamDefinition param) throws NoSuchMethodException {
      return Context.class.getDeclaredMethod("path", String.class);
    }

    @Override public ParamWriter newWriter() {
      return new NamedParamWriter();
    }
  },

  CONTEXT_PARAM {
    @Override public Set<String> annotations() {
      return Annotations.CONTEXT_PARAMS;
    }

    @Override public Method valueObject(ParamDefinition param) throws NoSuchMethodException {
      return Context.class.getDeclaredMethod("getAttributes");
    }

    @Override public Method singleValue(ParamDefinition param) throws NoSuchMethodException {
      return Context.class.getDeclaredMethod("attribute", String.class);
    }

    @Override public ParamWriter newWriter() {
      return new ContextParamWriter();
    }
  },

  QUERY_PARAM {
    @Override public Set<String> annotations() {
      return Annotations.QUERY_PARAMS;
    }

    @Override public Method valueObject(ParamDefinition param) throws NoSuchMethodException {
      return Context.class.getDeclaredMethod("query");
    }

    @Override public Method singleValue(ParamDefinition param) throws NoSuchMethodException {
      return Context.class.getDeclaredMethod("query", String.class);
    }

    @Override public ParamWriter newWriter() {
      return new NamedParamWriter();
    }
  },
  COOKIE_PARAM {
    @Override public Set<String> annotations() {
      return Annotations.COOKIE_PARAMS;
    }

    @Override public Method valueObject(ParamDefinition param) throws NoSuchMethodException {
      throw new UnsupportedOperationException();
    }

    @Override public Method singleValue(ParamDefinition param) throws NoSuchMethodException {
      return Context.class.getDeclaredMethod("cookie", String.class);
    }

    @Override public ParamWriter newWriter() {
      return new NamedParamWriter();
    }
  },
  HEADER_PARAM {
    @Override public Set<String> annotations() {
      return Annotations.HEADER_PARAMS;
    }

    @Override public Method valueObject(ParamDefinition param) throws NoSuchMethodException {
      throw new UnsupportedOperationException();
    }

    @Override public Method singleValue(ParamDefinition param) throws NoSuchMethodException {
      return Context.class.getDeclaredMethod("header", String.class);
    }

    @Override public ParamWriter newWriter() {
      return new NamedParamWriter();
    }
  },
  FLASH_PARAM {
    @Override public Set<String> annotations() {
      return Annotations.FLASH_PARAMS;
    }

    @Override public Method valueObject(ParamDefinition param) throws NoSuchMethodException {
      return Context.class.getDeclaredMethod("flash");
    }

    @Override public Method singleValue(ParamDefinition param) throws NoSuchMethodException {
      return Context.class.getDeclaredMethod("flash", String.class);
    }

    @Override public ParamWriter newWriter() {
      return new NamedParamWriter();
    }
  },
  FORM_PARAM {
    @Override public Set<String> annotations() {
      return Annotations.FORM_PARAMS;
    }

    @Override public Method valueObject(ParamDefinition param) throws NoSuchMethodException {
      return Context.class.getDeclaredMethod("multipart");
    }

    @Override public Method singleValue(ParamDefinition param) throws NoSuchMethodException {
      return Context.class.getDeclaredMethod("multipart", String.class);
    }

    @Override public ParamWriter newWriter() {
      return new NamedParamWriter();
    }
  },
  SESSION_PARAM {
    @Override public Set<String> annotations() {
      return Collections.emptySet();
    }

    @Override public Method valueObject(ParamDefinition param) throws NoSuchMethodException {
      if (param.isOptional()) {
        return Context.class.getDeclaredMethod("sessionOrNull");
      }
      return Context.class.getDeclaredMethod("session");
    }
  },
  ROUTE_PARAM {
    @Override public Set<String> annotations() {
      return Collections.emptySet();
    }

    @Override public Method valueObject(ParamDefinition param) throws NoSuchMethodException {
      return Context.class.getDeclaredMethod("getRoute");
    }
  },

  BODY_PARAM {
    @Override public Method valueObject(ParamDefinition param) throws NoSuchMethodException {
      return Context.class.getDeclaredMethod("body");
    }

    @Override public ParamWriter newWriter() {
      return new BodyWriter();
    }
  };

  public Set<String> annotations() {
    return Collections.emptySet();
  }

  public ParamWriter newWriter() {
    throw new UnsupportedOperationException();
  }

  public abstract Method valueObject(ParamDefinition param) throws NoSuchMethodException;

  public Method singleValue(ParamDefinition param) throws NoSuchMethodException {
    throw new UnsupportedOperationException("No value method for: '" + param + "'");
  }

  public static ParamKind forTypeInjection(ParamDefinition param) {
    TypeMirror type = param.isOptional() ?
        param.getType().getArguments().get(0).getRawType() :
        param.getType().getRawType();
    String rawType = type.toString().replace(Formdata.class.getName(), Multipart.class.getName());
    for (ParamKind value : values()) {
      try {
        if (value.valueObject(param).getReturnType().getName().equals(rawType)) {
          return value;
        }
      } catch (NoSuchMethodException | UnsupportedOperationException x) {
        // ignored it
      }
    }
    throw new UnsupportedOperationException("No type injection for: '" + param + "'");
  }
}
