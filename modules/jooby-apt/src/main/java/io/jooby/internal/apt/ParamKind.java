/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.apt;

import java.util.Collections;
import java.util.Set;

import javax.lang.model.type.TypeMirror;

import org.objectweb.asm.Type;

import io.jooby.internal.apt.asm.BodyWriter;
import io.jooby.internal.apt.asm.ContextParamWriter;
import io.jooby.internal.apt.asm.FileUploadWriter;
import io.jooby.internal.apt.asm.NamedParamWriter;
import io.jooby.internal.apt.asm.ObjectTypeWriter;
import io.jooby.internal.apt.asm.ParamLookupWriter;
import io.jooby.internal.apt.asm.ParamWriter;

public enum ParamKind {
  TYPE {
    @Override
    public MethodDescriptor valueObject(ParamDefinition param) {
      throw new UnsupportedOperationException(param.toString());
    }

    @Override
    public ParamWriter newWriter() {
      return new ObjectTypeWriter();
    }
  },

  FILE_UPLOAD {
    @Override
    public MethodDescriptor valueObject(ParamDefinition param) throws NoSuchMethodException {
      throw new UnsupportedOperationException(param.toString());
    }

    @Override
    public ParamWriter newWriter() {
      return new FileUploadWriter();
    }
  },

  PATH_PARAM {
    @Override
    public Set<String> annotations() {
      return Annotations.PATH_PARAMS;
    }

    @Override
    public MethodDescriptor valueObject(ParamDefinition param) throws NoSuchMethodException {
      return MethodDescriptor.Context.pathMap();
    }

    @Override
    public MethodDescriptor singleValue(ParamDefinition param) throws NoSuchMethodException {
      return MethodDescriptor.Context.path();
    }

    @Override
    public ParamWriter newWriter() {
      return new NamedParamWriter();
    }
  },

  CONTEXT_PARAM {
    @Override
    public Set<String> annotations() {
      return Annotations.CONTEXT_PARAMS;
    }

    @Override
    public MethodDescriptor valueObject(ParamDefinition param) throws NoSuchMethodException {
      return MethodDescriptor.Context.getAttributes();
    }

    @Override
    public MethodDescriptor singleValue(ParamDefinition param) throws NoSuchMethodException {
      return MethodDescriptor.Context.getAttribute();
    }

    @Override
    public ParamWriter newWriter() {
      return new ContextParamWriter();
    }
  },
  SESSION_ATTRIBUTE_PARAM {
    @Override
    public Set<String> annotations() {
      return Annotations.SESSION_PARAMS;
    }

    @Override
    public MethodDescriptor valueObject(ParamDefinition param) throws NoSuchMethodException {
      if (param.isOptional()) {
        return MethodDescriptor.Context.sessionOrNull();
      }
      return MethodDescriptor.Context.sessionMap();
    }

    @Override
    public MethodDescriptor singleValue(ParamDefinition param) throws NoSuchMethodException {
      return MethodDescriptor.Context.session();
    }

    @Override
    public ParamWriter newWriter() {
      return new NamedParamWriter();
    }
  },
  QUERY_PARAM {
    @Override
    public Set<String> annotations() {
      return Annotations.QUERY_PARAMS;
    }

    @Override
    public MethodDescriptor valueObject(ParamDefinition param) throws NoSuchMethodException {
      return MethodDescriptor.Context.queryString();
    }

    @Override
    public MethodDescriptor singleValue(ParamDefinition param) throws NoSuchMethodException {
      return MethodDescriptor.Context.query();
    }

    @Override
    public ParamWriter newWriter() {
      return new NamedParamWriter();
    }
  },
  COOKIE_PARAM {
    @Override
    public Set<String> annotations() {
      return Annotations.COOKIE_PARAMS;
    }

    @Override
    public MethodDescriptor valueObject(ParamDefinition param) throws NoSuchMethodException {
      throw new UnsupportedOperationException();
    }

    @Override
    public MethodDescriptor singleValue(ParamDefinition param) throws NoSuchMethodException {
      return MethodDescriptor.Context.cookie();
    }

    @Override
    public ParamWriter newWriter() {
      return new NamedParamWriter();
    }
  },
  HEADER_PARAM {
    @Override
    public Set<String> annotations() {
      return Annotations.HEADER_PARAMS;
    }

    @Override
    public MethodDescriptor valueObject(ParamDefinition param) throws NoSuchMethodException {
      throw new UnsupportedOperationException();
    }

    @Override
    public MethodDescriptor singleValue(ParamDefinition param) throws NoSuchMethodException {
      return MethodDescriptor.Context.header();
    }

    @Override
    public ParamWriter newWriter() {
      return new NamedParamWriter();
    }
  },
  FLASH_PARAM {
    @Override
    public Set<String> annotations() {
      return Annotations.FLASH_PARAMS;
    }

    @Override
    public MethodDescriptor valueObject(ParamDefinition param) throws NoSuchMethodException {
      return MethodDescriptor.Context.flashMap();
    }

    @Override
    public MethodDescriptor singleValue(ParamDefinition param) throws NoSuchMethodException {
      return MethodDescriptor.Context.flash();
    }

    @Override
    public ParamWriter newWriter() {
      return new NamedParamWriter();
    }
  },
  FORM_PARAM {
    @Override
    public Set<String> annotations() {
      return Annotations.FORM_PARAMS;
    }

    @Override
    public MethodDescriptor valueObject(ParamDefinition param) throws NoSuchMethodException {
      return MethodDescriptor.Context.formBody();
    }

    @Override
    public MethodDescriptor singleValue(ParamDefinition param) throws NoSuchMethodException {
      return MethodDescriptor.Context.form();
    }

    @Override
    public ParamWriter newWriter() {
      return new NamedParamWriter();
    }
  },
  PARAM_LOOKUP {
    @Override
    public Set<String> annotations() {
      return Annotations.PARAM_LOOKUP;
    }

    @Override
    public MethodDescriptor singleValue(ParamDefinition param) throws NoSuchMethodException {
      return MethodDescriptor.Context.lookup();
    }

    @Override
    public ParamWriter newWriter() {
      return new ParamLookupWriter();
    }

    @Override
    public String httpNameMemberName() {
      return "name";
    }
  },

  ROUTE_PARAM {
    @Override
    public Set<String> annotations() {
      return Collections.emptySet();
    }

    @Override
    public MethodDescriptor valueObject(ParamDefinition param) throws NoSuchMethodException {
      return MethodDescriptor.Context.getRoute();
    }
  },

  BODY_PARAM {
    @Override
    public MethodDescriptor valueObject(ParamDefinition param) throws NoSuchMethodException {
      return MethodDescriptor.Context.body();
    }

    @Override
    public ParamWriter newWriter() {
      return new BodyWriter();
    }
  };

  public Set<String> annotations() {
    return Collections.emptySet();
  }

  public ParamWriter newWriter() {
    throw new UnsupportedOperationException();
  }

  public MethodDescriptor valueObject(ParamDefinition param) throws NoSuchMethodException {
    throw new UnsupportedOperationException("No value object method for: '" + param + "'");
  }

  public MethodDescriptor singleValue(ParamDefinition param) throws NoSuchMethodException {
    throw new UnsupportedOperationException("No single value method for: '" + param + "'");
  }

  public String httpNameMemberName() {
    return "value";
  }

  public static ParamKind forTypeInjection(ParamDefinition param) {
    TypeMirror type =
        param.isOptional()
            ? param.getType().getArguments().get(0).getRawType()
            : param.getType().getRawType();
    String rawType = type.toString();
    for (ParamKind value : values()) {
      try {
        MethodDescriptor descriptor = value.valueObject(param);
        Type returnType = descriptor.getReturnType();
        // handle class names or array primitive class names
        if (returnType.getClassName().equals(rawType)
            || returnType.getDescriptor().equals(rawType)) {
          return value;
        }
      } catch (NoSuchMethodException | UnsupportedOperationException x) {
        // ignored it
      }
    }
    throw new UnsupportedOperationException("No type injection for: '" + param + "'");
  }
}
