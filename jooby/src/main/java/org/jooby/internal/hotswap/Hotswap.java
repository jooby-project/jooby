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
/**
 * This copy of Woodstox XML processor is licensed under the
 * Apache (Software) License, version 2.0 ("the License").
 * See the License for details about distribution rights, and the
 * specific rights regarding derivate works.
 *
 * You may obtain a copy of the License at:
 *
 * http://www.apache.org/licenses/
 *
 * A copy is also included in the downloadable source code package
 * containing Woodstox, in file "ASL2.0", under the same directory
 * as this file.
 */
package org.jooby.internal.hotswap;

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.jooby.internal.AppManager;
import org.jooby.internal.RouteHandler;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.name.Names;
import com.typesafe.config.Config;

public class Hotswap implements HotswapScanner.Listener {

  /** The logging system. */
  private final Logger log = LoggerFactory.getLogger(getClass());

  private final ReadWriteLock lock = new ReentrantReadWriteLock();

  private HotswapScanner scanner;

  private String appClass;

  private List<String> hash;

  private Injector injector;

  private AppManager appManager;

  private List<String> extenstions;

  public Hotswap(final Injector injector) throws IOException {
    this.injector = requireNonNull(injector, "An injector is required.");
    this.appClass = injector.getInstance(Key.get(String.class, Names.named("internal.appClass")));
    this.appManager = injector.getInstance(AppManager.class);
    Config config = injector.getInstance(Config.class);
    this.scanner = new HotswapScanner(this, config);
    this.extenstions = config.getStringList("hotswap.reload.ext");
    this.hash = hash(appClass);
  }

  public Injector reload(final String resource) {
    try {
      lock.writeLock().lock();
      String ext = "";
      int dot = resource.lastIndexOf('.');
      if (dot > 0) {
        ext = resource.substring(dot + 1);
      }
      if (!extenstions.contains(ext)) {
        log.debug("ignoring {}", resource);
        return injector;
      }
      String className = resource.replace('/', '.').replace(".class", "");
      final Injector result;
      if (className.equals(appClass)) {
        // let's check if we need a bounce for app
        result = tryApp(className);
      } else {
        // we can't tell, so just reload
        result = appManager.execute(AppManager.RESTART);
      }
      if (result != null && result != injector) {
        // update internal injector
        this.injector = result;
      }
      return this.injector;
    } catch (Throwable ex) {
      log.error("reload failed with...", ex);
      appManager.execute(AppManager.STOP);
      return null;
    } finally {
      lock.writeLock().unlock();
    }
  }

  private Injector tryApp(final String className) throws IOException {
    List<String> newHash = hash(className);
    boolean reload = this.hash.size() != newHash.size() || !this.hash.containsAll(newHash);
    if (reload) {
      this.hash.clear();
      this.hash.addAll(newHash);
      return appManager.execute(AppManager.RESTART);
    }
    return null;
  }

  public RouteHandler handler() {
    try {
      lock.readLock().lock();
      return injector.getInstance(RouteHandler.class);
    } finally {
      lock.readLock().unlock();
    }
  }

  public void stop() {
    try {
      lock.readLock().lock();
      scanner.stop();
    } finally {
      lock.readLock().unlock();
    }
  }

  @Override
  public void changed(final String resource) {
    reload(resource);
  }

  public void start() {
    scanner.start();
  }

  protected List<String> hash(final String classname) throws IOException {
    return bytecode(classname);
  }

  private static List<String> bytecode(final String classname) throws IOException {
    StringBuilder buff = new StringBuilder();
    List<String> result = new ArrayList<String>();
    new ClassReader(classname).accept(new ClassVisitor(Opcodes.ASM5) {
      @Override
      public MethodVisitor visitMethod(final int access, final String name, final String desc,
          final String signature,
          final String[] exceptions) {
        if (!"<init>".equals(name)) {
          return null;
        }
        return new MethodVisitor(Opcodes.ASM5) {
          @Override
          public void visitLabel(final Label label) {
            if (buff.length() > 0) {
              result.add(buff.toString());
            }
            buff.setLength(0);
          }

          @Override
          public void visitLdcInsn(final Object cst) {
            buff.append("LDC ");
            if (cst instanceof Type) {
              buff.append(((Type) cst).getDescriptor()).append(".class");
            } else {
              buff.append(cst);
            }
            buff.append(' ');
          }

          @Override
          public void visitMethodInsn(final int opcode, final String owner, final String name,
              final String desc,
              final boolean itf) {
            buff.append(opcode).append(' ');
            buff.append(owner);
            buff.append('.').append(name).append(' ');
            buff.append(desc).append(' ');
          }

          @Override
          public void visitInvokeDynamicInsn(final String name, final String desc,
              final Handle bsm,
              final Object... bsmArgs) {
            buff.append("INVOKEDYNAMIC").append(' ');
            buff.append(name).append(desc).append(' ');
          }
        };
      }
    }, 0);
    return result;
  }

}
