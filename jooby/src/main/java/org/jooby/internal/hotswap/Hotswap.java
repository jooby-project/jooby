package org.jooby.internal.hotswap;

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.jooby.Jooby;
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

  private HotswapScanner scanner;

  private final ReadWriteLock lock = new ReentrantReadWriteLock();

  private Class<? extends Jooby> appClass;

  private List<String> hash;

  private Injector injector;

  private AppManager appManager;

  @SuppressWarnings("unchecked")
  public Hotswap(final Injector injector) throws IOException {
    this.injector = requireNonNull(injector, "An injector is required.");
    this.appClass = injector.getInstance(Key.get(Class.class, Names.named("internal.appClass")));
    this.appManager = injector.getInstance(AppManager.class);
    this.scanner = new HotswapScanner(this, injector.getInstance(Config.class));
    this.hash = hash(appClass.getName());
  }

  public Injector reload(final String resource) {
    try {
      lock.writeLock().lock();
      String className = resource.replace('/', '.').replace(".class", "");
      final Injector result;
      if (className.equals(appClass.getName())) {
        // let's check if we need a bounce for app
        result = tryApp(appClass);
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
      log.error("App didn't reload", ex);
      log.info("Stopping...");
      appManager.execute(AppManager.STOP);
      return this.injector;
    } finally {
      lock.writeLock().unlock();
    }
  }

  private Injector tryApp(final Class<? extends Jooby> appClass) throws IOException {
    List<String> newHash = hash(appClass.getName());
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
    scanner.stop();
  }

  @Override
  public void changed(final String resource) {
    reload(resource);
  }

  public void start() {
    scanner.start();
  }

  public static List<String> hash(final String classname) throws IOException {
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
