/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.dbscheduler;

import java.io.*;

import com.github.kagkarlsson.scheduler.exceptions.SerializationException;
import com.github.kagkarlsson.scheduler.serializer.Serializer;

/**
 * Java serializer that uses a class loader (not Class.forname) for class lookup. Works better with
 * jooby-run restarts.
 */
public class ClassLoaderJavaSerializer implements Serializer {

  private final ClassLoader classLoader;

  public ClassLoaderJavaSerializer(ClassLoader classLoader) {
    this.classLoader = classLoader;
  }

  public byte[] serialize(Object data) {
    if (data == null) return null;
    try (var bos = new ByteArrayOutputStream();
        var out = new ObjectOutputStream(bos)) {
      out.writeObject(data);
      return bos.toByteArray();
    } catch (Exception e) {
      throw new SerializationException("Failed to serialize object", e);
    }
  }

  public <T> T deserialize(Class<T> clazz, byte[] serializedData) {
    if (serializedData == null) return null;
    try (var bis = new ByteArrayInputStream(serializedData);
        var in = joobyObjectInputStream(bis, classLoader)) {
      return clazz.cast(in.readObject());
    } catch (Exception e) {
      throw new SerializationException("Failed to deserialize object", e);
    }
  }

  private static ObjectInputStream joobyObjectInputStream(
      ByteArrayInputStream in, ClassLoader classLoader) throws IOException {
    return new ObjectInputStream(in) {
      @Override
      protected Class<?> resolveClass(ObjectStreamClass desc) throws ClassNotFoundException {
        return classLoader.loadClass(desc.getName());
      }
    };
  }
}
