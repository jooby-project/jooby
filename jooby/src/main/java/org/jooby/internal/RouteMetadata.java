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
package org.jooby.internal;

import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.jooby.Env;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import com.google.common.base.Throwables;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.io.Closeables;
import com.google.common.io.Resources;
import com.google.common.util.concurrent.UncheckedExecutionException;

public class RouteMetadata implements ParameterNameProvider {

  private static final String[] NO_ARG = new String[0];

  private final LoadingCache<Class<?>, Map<String, Object>> cache;

  public RouteMetadata(final Env env) {
    CacheLoader<Class<?>, Map<String, Object>> loader = CacheLoader
        .from(RouteMetadata::extractMetadata);

    cache = env.name().equals("dev")
        ? CacheBuilder.newBuilder().maximumSize(0).build(loader)
        : CacheBuilder.newBuilder().build(loader);
  }

  @Override
  public String[] names(final Executable exec) {
    Map<String, Object> md = md(exec);
    String key = paramsKey(exec);
    return (String[]) md.get(key);
  }

  public int startAt(final Executable exec) {
    Map<String, Object> md = md(exec);
    return (Integer) md.getOrDefault(startAtKey(exec), -1);
  }

  private Map<String, Object> md(final Executable exec) {
    try {
      return cache.getUnchecked(exec.getDeclaringClass());
    } catch (UncheckedExecutionException ex) {
      throw Throwables.propagate(ex.getCause());
    }
  }

  private static Map<String, Object> extractMetadata(final Class<?> owner) {
    InputStream stream = null;
    try {
      Map<String, Object> md = new HashMap<>();
      stream = Resources.getResource(owner, classfile(owner)).openStream();
      new ClassReader(stream).accept(visitor(md), 0);
      return md;
    } catch (Exception ex) {
      // won't happen, but...
      throw new IllegalStateException("Can't read class: " + owner.getName(), ex);
    } finally {
      Closeables.closeQuietly(stream);
    }
  }

  private static String classfile(final Class<?> owner) {
    StringBuilder sb = new StringBuilder();
    Class<?> dc = owner.getDeclaringClass();
    while (dc != null) {
      sb.insert(0, dc.getSimpleName()).append("$");
      dc = dc.getDeclaringClass();
    }
    sb.append(owner.getSimpleName());
    sb.append(".class");
    return sb.toString();
  }

  private static ClassVisitor visitor(final Map<String, Object> md) {
    return new ClassVisitor(Opcodes.ASM5) {

      @Override
      public MethodVisitor visitMethod(final int access, final String name,
          final String desc, final String signature, final String[] exceptions) {
        boolean isPublic = ((access & Opcodes.ACC_PUBLIC) > 0) ? true : false;
        if (!isPublic) {
          // ignore
          return null;
        }
        final String seed = name + desc;
        Type[] args = Type.getArgumentTypes(desc);
        String[] names = args.length == 0 ? NO_ARG : new String[args.length];
        md.put(paramsKey(seed), names);

        int minIdx = ((access & Opcodes.ACC_STATIC) > 0) ? 0 : 1;
        int maxIdx = Arrays.stream(args).mapToInt(Type::getSize).sum();

        return new MethodVisitor(Opcodes.ASM5) {

          private int i = 0;

          private boolean skipLocalTable = false;

          @Override
          public void visitParameter(final String name, final int access) {
            skipLocalTable = true;
            // save current parameter
            names[i] = name;
            // move to next
            i += 1;
          }

          @Override
          public void visitLineNumber(final int line, final Label start) {
            // save line number
            md.putIfAbsent(startAtKey(seed), line);
          }

          @Override
          public void visitLocalVariable(final String name, final String desc,
              final String signature,
              final Label start, final Label end, final int index) {
            if (!skipLocalTable) {
              if (index >= minIdx && index <= maxIdx) {
                // save current parameter
                names[i] = name;
                // move to next
                i += 1;
              }
            }
          }

        };
      }

    };
  }

  private static String paramsKey(final Executable exec) {
    return paramsKey(key(exec));
  }

  private static String paramsKey(final String key) {
    return key + ".params";
  }

  private static String startAtKey(final Executable exec) {
    return startAtKey(key(exec));
  }

  private static String startAtKey(final String key) {
    return key + ".startAt";
  }

  @SuppressWarnings("rawtypes")
  private static String key(final Executable exec) {
    if (exec instanceof Method) {
      return exec.getName() + Type.getMethodDescriptor((Method) exec);
    } else {
      return "<init>" + Type.getConstructorDescriptor((Constructor) exec);
    }
  }
}
