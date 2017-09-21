package org.jooby.filewatcher;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Injector;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.isA;
import org.easymock.IExpectationSetters;
import org.jooby.Env;
import org.jooby.test.MockUnit;
import org.jooby.test.MockUnit.Block;
import org.jooby.funzy.Throwing;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.IOException;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchEvent.Kind;
import java.nio.file.WatchEvent.Modifier;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.function.Function;

@RunWith(PowerMockRunner.class)
@PrepareForTest({FileMonitor.class, Thread.class, Files.class, Executors.class})
public class FileMonitorTest {

  private Block newThread = unit -> {
    unit.mockStatic(Executors.class);
    ExecutorService executor = unit.mock(ExecutorService.class);
    expect(Executors.newSingleThreadExecutor(unit.capture(ThreadFactory.class)))
        .andReturn(executor);
    executor.execute(unit.capture(Runnable.class));
    unit.registerMock(ExecutorService.class, executor);

    Env env = unit.get(Env.class);
    expect(env.onStop(unit.capture(Throwing.Runnable.class))).andReturn(env);

    Thread thread = unit.constructor(Thread.class)
        .args(Runnable.class, String.class)
        .build(isA(Runnable.class), eq("file-watcher"));

    thread.setDaemon(true);
  };
  private Block takeInterrupt = unit -> {
    WatchService watcher = unit.get(WatchService.class);
    expect(watcher.take()).andThrow(new InterruptedException());
  };

  private Block take = unit -> {
    WatchService watcher = unit.get(WatchService.class);
    expect(watcher.take()).andReturn(unit.get(WatchKey.class));
  };

  private Block takeIgnore = unit -> {
    WatchService watcher = unit.get(WatchService.class);
    expect(watcher.take()).andReturn(unit.mock(WatchKey.class));
  };

  @Test
  public void newFileMonitor() throws Exception {
    new MockUnit(Injector.class, Env.class, WatchService.class, FileEventOptions.class)
        .expect(newThread)
        .expect(unit -> {
          ExecutorService executor = unit.get(ExecutorService.class);
          executor.shutdown();
        })
        .run(unit -> {
          FileMonitor monitor = new FileMonitor(unit.get(Injector.class), unit.get(Env.class),
              unit.get(WatchService.class),
              ImmutableSet.of(unit.get(FileEventOptions.class)));
          unit.captured(ThreadFactory.class).get(0).newThread(monitor);
          unit.captured(Throwing.Runnable.class).get(0).run();
          ;
        });
  }

  @SuppressWarnings("unchecked")
  @Test
  public void registerTreeRecursive() throws Exception {
    new MockUnit(Injector.class, Env.class, WatchService.class, FileEventOptions.class, Path.class,
        WatchEvent.Kind.class, WatchEvent.Modifier.class, WatchKey.class)
        .expect(newThread)
        .expect(registerTree(true, false))
        .expect(takeInterrupt)
        .run(unit -> {
          FileMonitor monitor = new FileMonitor(unit.get(Injector.class), unit.get(Env.class),
              unit.get(WatchService.class),
              ImmutableSet.of(unit.get(FileEventOptions.class)));
          unit.captured(ThreadFactory.class).get(0).newThread(monitor);
        }, unit -> {
          unit.captured(Runnable.class).get(0).run();
          unit.captured(FileVisitor.class).get(0).preVisitDirectory(unit.get(Path.class), null);
        });
  }

  @Test
  public void registerTree() throws Exception {
    new MockUnit(Injector.class, Env.class, WatchService.class, FileEventOptions.class, Path.class,
        WatchEvent.Kind.class, WatchEvent.Modifier.class, WatchKey.class)
        .expect(newThread)
        .expect(registerTree(false, false))
        .expect(takeInterrupt)
        .run(unit -> {
          FileMonitor monitor = new FileMonitor(unit.get(Injector.class), unit.get(Env.class),
              unit.get(WatchService.class),
              ImmutableSet.of(unit.get(FileEventOptions.class)));
          unit.captured(ThreadFactory.class).get(0).newThread(monitor);
        }, unit -> {
          unit.captured(Runnable.class).get(0).run();
        });
  }

  @Test
  public void registerTreeErr() throws Exception {
    new MockUnit(Injector.class, Env.class, WatchService.class, FileEventOptions.class, Path.class,
        WatchEvent.Kind.class, WatchEvent.Modifier.class, WatchKey.class)
        .expect(newThread)
        .expect(registerTree(false, true))
        .expect(takeInterrupt)
        .run(unit -> {
          FileMonitor monitor = new FileMonitor(unit.get(Injector.class), unit.get(Env.class),
              unit.get(WatchService.class),
              ImmutableSet.of(unit.get(FileEventOptions.class)));
          unit.captured(ThreadFactory.class).get(0).newThread(monitor);
        }, unit -> {
          unit.captured(Runnable.class).get(0).run();
        });
  }

  @Test
  public void pollIgnoreMissing() throws Exception {
    new MockUnit(Injector.class, Env.class, WatchService.class, FileEventOptions.class, Path.class,
        WatchEvent.Kind.class, WatchEvent.Modifier.class, WatchKey.class)
        .expect(newThread)
        .expect(registerTree(false, false))
        .expect(takeIgnore)
        .expect(takeInterrupt)
        .run(unit -> {
          FileMonitor monitor = new FileMonitor(unit.get(Injector.class), unit.get(Env.class),
              unit.get(WatchService.class),
              ImmutableSet.of(unit.get(FileEventOptions.class)));
          unit.captured(ThreadFactory.class).get(0).newThread(monitor);
        }, unit -> {
          unit.captured(Runnable.class).get(0).run();
        });
  }

  @Test
  public void pollEvents() throws Exception {
    Path path = Paths.get("target/foo.txt");
    new MockUnit(Injector.class, Env.class, WatchService.class, FileEventOptions.class, Path.class,
        WatchEvent.Kind.class, WatchEvent.Modifier.class, WatchKey.class, WatchEvent.class,
        PathMatcher.class, FileEventHandler.class)
        .expect(newThread)
        .expect(registerTree(false, false))
        .expect(take)
        .expect(poll(StandardWatchEventKinds.ENTRY_MODIFY, path))
        .expect(filter(true))
        .expect(handler(StandardWatchEventKinds.ENTRY_MODIFY, false))
        .expect(recursive(false, false))
        .expect(reset(true))
        .expect(takeInterrupt)
        .run(unit -> {
          FileMonitor monitor = new FileMonitor(unit.get(Injector.class), unit.get(Env.class),
              unit.get(WatchService.class),
              ImmutableSet.of(unit.get(FileEventOptions.class)));
          unit.captured(ThreadFactory.class).get(0).newThread(monitor);
        }, unit -> {
          unit.captured(Runnable.class).get(0).run();
        });
  }

  @Test
  public void pollEventsInvalid() throws Exception {
    Path path = Paths.get("target/foo.txt");
    new MockUnit(Injector.class, Env.class, WatchService.class, FileEventOptions.class, Path.class,
        WatchEvent.Kind.class, WatchEvent.Modifier.class, WatchKey.class, WatchEvent.class,
        PathMatcher.class, FileEventHandler.class)
        .expect(newThread)
        .expect(registerTree(false, false))
        .expect(take)
        .expect(poll(StandardWatchEventKinds.ENTRY_MODIFY, path))
        .expect(filter(true))
        .expect(handler(StandardWatchEventKinds.ENTRY_MODIFY, false))
        .expect(recursive(false, false))
        .expect(reset(false))
        .run(unit -> {
          FileMonitor monitor = new FileMonitor(unit.get(Injector.class), unit.get(Env.class),
              unit.get(WatchService.class),
              ImmutableSet.of(unit.get(FileEventOptions.class)));
          unit.captured(ThreadFactory.class).get(0).newThread(monitor);
        }, unit -> {
          unit.captured(Runnable.class).get(0).run();
        });
  }

  @Test
  public void pollEventsRecursive() throws Exception {
    Path path = Paths.get("target/foo.txt");
    new MockUnit(Injector.class, Env.class, WatchService.class, FileEventOptions.class, Path.class,
        WatchEvent.Kind.class, WatchEvent.Modifier.class, WatchKey.class, WatchEvent.class,
        PathMatcher.class, FileEventHandler.class)
        .expect(newThread)
        .expect(registerTree(false, false))
        .expect(take)
        .expect(poll(StandardWatchEventKinds.ENTRY_CREATE, path))
        .expect(filter(true))
        .expect(handler(StandardWatchEventKinds.ENTRY_CREATE, false))
        .expect(recursive(true, false))
        .expect(reset(true))
        .expect(takeInterrupt)
        .run(unit -> {
          FileMonitor monitor = new FileMonitor(unit.get(Injector.class), unit.get(Env.class),
              unit.get(WatchService.class),
              ImmutableSet.of(unit.first(FileEventOptions.class)));
          unit.captured(ThreadFactory.class).get(0).newThread(monitor);
        }, unit -> {
          unit.captured(Runnable.class).get(0).run();
        });
  }

  @Test
  public void pollEventsRecursiveErr() throws Exception {
    Path path = Paths.get("target/foo.txt");
    new MockUnit(Injector.class, Env.class, WatchService.class, FileEventOptions.class, Path.class,
        WatchEvent.Kind.class, WatchEvent.Modifier.class, WatchKey.class, WatchEvent.class,
        PathMatcher.class, FileEventHandler.class)
        .expect(newThread)
        .expect(registerTree(false, false))
        .expect(take)
        .expect(poll(StandardWatchEventKinds.ENTRY_CREATE, path))
        .expect(filter(true))
        .expect(handler(StandardWatchEventKinds.ENTRY_CREATE, false))
        .expect(recursive(true, true))
        .expect(reset(true))
        .expect(takeInterrupt)
        .run(unit -> {
          FileMonitor monitor = new FileMonitor(unit.get(Injector.class), unit.get(Env.class),
              unit.get(WatchService.class),
              ImmutableSet.of(unit.first(FileEventOptions.class)));
          unit.captured(ThreadFactory.class).get(0).newThread(monitor);
        }, unit -> {
          unit.captured(Runnable.class).get(0).run();
        });
  }

  @Test
  public void pollEventWithHandleErr() throws Exception {
    Path path = Paths.get("target/foo.txt");
    new MockUnit(Injector.class, Env.class, WatchService.class, FileEventOptions.class, Path.class,
        WatchEvent.Kind.class, WatchEvent.Modifier.class, WatchKey.class, WatchEvent.class,
        PathMatcher.class, FileEventHandler.class)
        .expect(newThread)
        .expect(registerTree(false, false))
        .expect(take)
        .expect(poll(StandardWatchEventKinds.ENTRY_MODIFY, path))
        .expect(filter(true))
        .expect(handler(StandardWatchEventKinds.ENTRY_MODIFY, true))
        .expect(recursive(false, false))
        .expect(reset(true))
        .expect(takeInterrupt)
        .run(unit -> {
          FileMonitor monitor = new FileMonitor(unit.get(Injector.class), unit.get(Env.class),
              unit.get(WatchService.class),
              ImmutableSet.of(unit.get(FileEventOptions.class)));
          unit.captured(ThreadFactory.class).get(0).newThread(monitor);
        }, unit -> {
          unit.captured(Runnable.class).get(0).run();
        });
  }

  @Test
  public void pollEventsNoMatches() throws Exception {
    Path path = Paths.get("target/foo.txt");
    new MockUnit(Injector.class, Env.class, WatchService.class, FileEventOptions.class, Path.class,
        WatchEvent.Kind.class, WatchEvent.Modifier.class, WatchKey.class, WatchEvent.class,
        PathMatcher.class, FileEventHandler.class)
        .expect(newThread)
        .expect(registerTree(false, false))
        .expect(take)
        .expect(poll(StandardWatchEventKinds.ENTRY_MODIFY, path))
        .expect(filter(false))
        .expect(recursive(false, false))
        .expect(reset(true))
        .expect(takeInterrupt)
        .run(unit -> {
          FileMonitor monitor = new FileMonitor(unit.get(Injector.class), unit.get(Env.class),
              unit.get(WatchService.class),
              ImmutableSet.of(unit.get(FileEventOptions.class)));
          unit.captured(ThreadFactory.class).get(0).newThread(monitor);
        }, unit -> {
          unit.captured(Runnable.class).get(0).run();
        });
  }

  private Block reset(final boolean valid) {
    return unit -> {
      WatchKey key = unit.get(WatchKey.class);
      expect(key.reset()).andReturn(valid);
    };
  }

  @SuppressWarnings("rawtypes")
  private Block poll(final Kind kind, final Path path) {
    return unit -> {
      WatchEvent event = unit.get(WatchEvent.class);
      expect(event.kind()).andReturn(kind);
      expect(event.context()).andReturn(path);
      Path source = unit.get(Path.class);
      Path resolved = unit.mock(Path.class);
      unit.registerMock(Path.class, resolved);
      expect(source.resolve(path)).andReturn(resolved);

      WatchEvent overflow = unit.mock(WatchEvent.class);
      expect(overflow.kind()).andReturn(StandardWatchEventKinds.OVERFLOW);
      expect(overflow.context()).andReturn(path);

      WatchKey key = unit.get(WatchKey.class);
      expect(key.pollEvents()).andReturn(ImmutableList.of(event, overflow));
    };
  }

  @SuppressWarnings("unchecked")
  private Block handler(final Kind<Path> kind, final boolean err) {
    return unit -> {
      FileEventHandler handler = unit.get(FileEventHandler.class);
      FileEventOptions options = unit.get(FileEventOptions.class);
      expect(options.handler(isA(Function.class))).andReturn(handler);
      handler.handle(kind, unit.get(Path.class));
      if (err) {
        expectLastCall().andThrow(new IOException("intentional err"));
      }
    };
  }

  private Block filter(final boolean matches) {
    return unit -> {
      Path resolved = unit.get(Path.class);
      PathMatcher filter = unit.get(PathMatcher.class);
      expect(filter.matches(resolved)).andReturn(matches);

      FileEventOptions options = unit.get(FileEventOptions.class);
      expect(options.filter()).andReturn(filter);
    };
  }

  private Block registerTree(final boolean recursive, final boolean err) {
    return unit -> {
      register(unit, recursive, err);
    };
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  private void register(final MockUnit unit, final boolean recursive, final boolean err)
      throws IOException {
    FileEventOptions options = unit.get(FileEventOptions.class);
    expect(options.recursive()).andReturn(recursive);

    Path path = unit.get(Path.class);
    expect(options.path()).andReturn(path).times(1, 2);

    Kind kind = unit.get(WatchEvent.Kind.class);
    Kind[] kinds = {kind};
    expect(options.kinds()).andReturn(kinds);

    Modifier mod = unit.get(WatchEvent.Modifier.class);
    expect(options.modifier()).andReturn(mod);

    if (recursive) {
      unit.mockStatic(Files.class);
      expect(Files.walkFileTree(eq(path), unit.capture(FileVisitor.class))).andReturn(path);
    }

    WatchKey key = unit.get(WatchKey.class);

    IExpectationSetters<WatchKey> register = expect(
        path.register(unit.get(WatchService.class), kinds, mod));
    if (err) {
      register.andThrow(new IOException("intentional error"));
    } else {
      register.andReturn(key);
    }
  }

  @SuppressWarnings("rawtypes")
  private Block recursive(final boolean recursive, final boolean err) {
    return unit -> {
      FileEventOptions options = unit.get(FileEventOptions.class);
      expect(options.recursive()).andReturn(recursive);

      if (recursive) {
        unit.mockStatic(Files.class);
        expect(Files.isDirectory(unit.get(Path.class))).andReturn(true);
        expect(options.recursive()).andReturn(false);

        ///
        Path path = unit.get(Path.class);

        Kind kind = unit.get(WatchEvent.Kind.class);
        Kind[] kinds = {kind};
        expect(options.kinds()).andReturn(kinds);

        Modifier mod = unit.get(WatchEvent.Modifier.class);
        expect(options.modifier()).andReturn(mod);

        WatchKey key = unit.get(WatchKey.class);

        IExpectationSetters<WatchKey> register = expect(
            path.register(unit.get(WatchService.class), kinds, mod));
        if (err) {
          register.andThrow(new IOException("intentional error"));
        } else {
          register.andReturn(key);
        }
      }
    };
  }

}
