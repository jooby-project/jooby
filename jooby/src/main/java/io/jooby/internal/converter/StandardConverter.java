/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.converter;

import java.lang.reflect.Type;

import edu.umd.cs.findbugs.annotations.NonNull;
import io.jooby.Value;
import io.jooby.value.Converter;
import io.jooby.value.ValueFactory;

public enum StandardConverter implements Converter {
  String {
    @Override
    protected void add(ValueFactory factory) {
      factory.put(java.lang.String.class, this);
    }

    @Override
    public Object convert(@NonNull Type type, @NonNull Value value) {
      return value.valueOrNull();
    }
  },
  Int {
    @Override
    protected void add(ValueFactory factory) {
      factory.put(int.class, this);
    }

    @Override
    public Object convert(@NonNull Type type, @NonNull Value value) {
      return value.intValue();
    }
  },
  IntNullable {
    @Override
    protected void add(ValueFactory factory) {
      factory.put(Integer.class, this);
    }

    @Override
    public Object convert(@NonNull Type type, @NonNull Value value) {
      return value.isMissing() ? null : value.intValue();
    }
  },
  Long {
    @Override
    protected void add(ValueFactory factory) {
      factory.put(long.class, this);
    }

    @Override
    public Object convert(@NonNull Type type, @NonNull Value value) {
      return value.longValue();
    }
  },
  LongNullable {
    @Override
    protected void add(ValueFactory factory) {
      factory.put(Long.class, this);
    }

    @Override
    public Object convert(@NonNull Type type, @NonNull Value value) {
      return value.isMissing() ? null : value.longValue();
    }
  },
  Float {
    @Override
    protected void add(ValueFactory factory) {
      factory.put(float.class, this);
    }

    @Override
    public Object convert(@NonNull Type type, @NonNull Value value) {
      return value.floatValue();
    }
  },
  FloatNullable {
    @Override
    protected void add(ValueFactory factory) {
      factory.put(Float.class, this);
    }

    @Override
    public Object convert(@NonNull Type type, @NonNull Value value) {
      return value.isMissing() ? null : value.floatValue();
    }
  },
  Double {
    @Override
    protected void add(ValueFactory factory) {
      factory.put(double.class, this);
    }

    @Override
    public Object convert(@NonNull Type type, @NonNull Value value) {
      return value.doubleValue();
    }
  },
  DoubleNullable {
    @Override
    protected void add(ValueFactory factory) {
      factory.put(Double.class, this);
    }

    @Override
    public Object convert(@NonNull Type type, @NonNull Value value) {
      return value.isMissing() ? null : value.doubleValue();
    }
  },
  Boolean {
    @Override
    protected void add(ValueFactory factory) {
      factory.put(boolean.class, this);
    }

    @Override
    public Object convert(@NonNull Type type, @NonNull Value value) {
      return value.booleanValue();
    }
  },
  BooleanNullable {
    @Override
    protected void add(ValueFactory factory) {
      factory.put(Boolean.class, this);
    }

    @Override
    public Object convert(@NonNull Type type, @NonNull Value value) {
      return value.isMissing() ? null : value.booleanValue();
    }
  },
  Byte {
    @Override
    protected void add(ValueFactory factory) {
      factory.put(byte.class, this);
    }

    @Override
    public Object convert(@NonNull Type type, @NonNull Value value) {
      return value.byteValue();
    }
  },
  ByteNullable {
    @Override
    protected void add(ValueFactory factory) {
      factory.put(Byte.class, this);
    }

    @Override
    public Object convert(@NonNull Type type, @NonNull Value value) {
      return value.isMissing() ? null : value.byteValue();
    }
  };

  protected abstract void add(ValueFactory factory);

  public static void register(ValueFactory factory) {
    for (var converter : values()) {
      converter.add(factory);
    }
  }
}
