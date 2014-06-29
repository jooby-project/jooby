package jooby.internal;

import static java.util.Objects.requireNonNull;

import java.util.Arrays;

import jooby.Request;

public class ParameterDefinition {

  private enum Strategy {
    CHAR {
      @Override
      public boolean apply(final Class<?> type) {
        return type == char.class || type == Character.class;
      }

      @Override
      public Object get(final Request request, final String name, final Class<?> type) {
        String value = request.param(name);
        return value.charAt(0);
      }
    },

    BOOL {
      @Override
      public boolean apply(final Class<?> type) {
        return type == boolean.class || type == Boolean.class;
      }

      @Override
      public Object get(final Request request, final String name, final Class<?> type) {
        String value = request.param(name);
        if ("true".equals(value)) {
          return Boolean.TRUE;
        } else if ("false".equals(value)) {
          return Boolean.FALSE;
        } else {
          // TODO: add URI
          throw new IllegalArgumentException(name + "=" + value);
        }
      }
    },

    BYTE {
      @Override
      public boolean apply(final Class<?> type) {
        return type == byte.class || type == Byte.class;
      }

      @Override
      public Object get(final Request request, final String name, final Class<?> type) {
        return Byte.valueOf(request.param(name));
      }
    },

    SHORT {
      @Override
      public boolean apply(final Class<?> type) {
        return type == short.class || type == Short.class;
      }

      @Override
      public Object get(final Request request, final String name, final Class<?> type) {
        return Short.valueOf(request.param(name));
      }
    },

    INT {
      @Override
      public boolean apply(final Class<?> type) {
        return type == int.class || type == Integer.class;
      }

      @Override
      public Object get(final Request request, final String name, final Class<?> type) {
        return Integer.valueOf(request.param(name));
      }
    },

    FLOAT {
      @Override
      public boolean apply(final Class<?> type) {
        return type == float.class || type == Float.class;
      }

      @Override
      public Object get(final Request request, final String name, final Class<?> type) {
        return Float.valueOf(request.param(name));
      }
    },

    DOUBLE {
      @Override
      public boolean apply(final Class<?> type) {
        return type == double.class || type == Double.class;
      }

      @Override
      public Object get(final Request request, final String name, final Class<?> type) {
        return Double.valueOf(request.param(name));
      }
    },

    STRING {
      @Override
      public boolean apply(final Class<?> type) {
        return type == String.class;
      }

      @Override
      public Object get(final Request request, final String name, final Class<?> type) {
        return request.param(name);
      }
    },

    ENUM {
      @Override
      public boolean apply(final Class<?> type) {
        return Enum.class.isAssignableFrom(type);
      }

      @SuppressWarnings({"rawtypes", "unchecked" })
      @Override
      public Object get(final Request request, final String name, final Class<?> type) {
        String value = request.param(name).toUpperCase();
        return Enum.valueOf((Class<Enum>) type, value);
      }
    };

    public abstract Object get(Request request, String name, Class<?> type);

    public boolean apply(final Class<?> type) {
      return false;
    }
  }

  private String name;

  private Class<?> type;

  private Strategy strategy;

  public ParameterDefinition(final String name, final Class<?> type) {
    this.name = requireNonNull(name, "The name is required.");
    this.type = requireNonNull(type, "The type is required.");

    this.strategy = Arrays.stream(Strategy.values()).filter(s -> s.apply(type)).findFirst().get();
  }

  public Object get(final Request request) {
    return strategy.get(request, name, type);
  }
}
