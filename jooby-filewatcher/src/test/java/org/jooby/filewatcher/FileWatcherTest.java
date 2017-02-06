package org.jooby.filewatcher;

import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;

import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchEvent.Kind;
import java.nio.file.WatchService;
import java.util.List;
import java.util.Map;

import org.jooby.Env;
import org.jooby.test.MockUnit;
import org.jooby.test.MockUnit.Block;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Binder;
import com.google.inject.binder.AnnotatedBindingBuilder;
import com.google.inject.binder.LinkedBindingBuilder;
import com.google.inject.multibindings.Multibinder;
import com.typesafe.config.Config;

@RunWith(PowerMockRunner.class)
@PrepareForTest({FileWatcher.class, Multibinder.class, FileSystems.class, FileEventOptions.class,
    Paths.class })
public class FileWatcherTest {

  @SuppressWarnings({"rawtypes", "unchecked" })
  private Block watcher = unit -> {
    WatchService watcher = unit.get(WatchService.class);
    FileSystem fs = unit.get(FileSystem.class);
    expect(fs.newWatchService()).andReturn(watcher);

    unit.mockStatic(FileSystems.class);
    expect(FileSystems.getDefault()).andReturn(fs);

    AnnotatedBindingBuilder abbws = unit.mock(AnnotatedBindingBuilder.class);
    abbws.toInstance(watcher);

    Binder binder = unit.get(Binder.class);
    expect(binder.bind(WatchService.class)).andReturn(abbws);
  };

  @SuppressWarnings({"rawtypes", "unchecked" })
  private Block filemonitor = unit -> {
    AnnotatedBindingBuilder aab = unit.mock(AnnotatedBindingBuilder.class);
    aab.asEagerSingleton();

    Binder binder = unit.get(Binder.class);
    expect(binder.bind(FileMonitor.class)).andReturn(aab);
  };

  @Test
  public void newModule() throws Exception {
    new MockUnit(Env.class, Config.class, Binder.class, WatchService.class, FileSystem.class)
        .expect(watcher)
        .expect(filewatcherProp(false))
        .expect(filemonitor)
        .run(unit -> {
          new FileWatcher()
              .configure(unit.get(Env.class), unit.get(Config.class), unit.get(Binder.class));
        });
  }

  @Test
  public void registerPath() throws Exception {
    new MockUnit(Env.class, Config.class, Binder.class, WatchService.class, FileSystem.class,
        Path.class)
            .expect(watcher)
            .expect(filewatcherProp(false))
            .expect(filemonitor)
            .expect(fileeventoptions(FileEventHandler.class))
            .run(unit -> {
              FileWatcher watcher = new FileWatcher();
              watcher.register(unit.get(Path.class), FileEventHandler.class)
                  .configure(unit.get(Env.class), unit.get(Config.class),
                      unit.get(Binder.class));
            });
  }

  @Test
  public void registerPathInstance() throws Exception {
    FileEventHandler handler = (k, p) -> {
    };
    new MockUnit(Env.class, Config.class, Binder.class, WatchService.class, FileSystem.class,
        Path.class)
            .expect(watcher)
            .expect(filewatcherProp(false))
            .expect(filemonitor)
            .expect(fileeventoptions(handler))
            .run(unit -> {
              FileWatcher watcher = new FileWatcher();
              watcher.register(unit.get(Path.class), handler)
                  .configure(unit.get(Env.class), unit.get(Config.class),
                      unit.get(Binder.class));
            });
  }

  @Test
  public void registerPathInstanceCallback() throws Exception {
    FileEventHandler handler = (k, p) -> {
    };
    new MockUnit(Env.class, Config.class, Binder.class, WatchService.class, FileSystem.class,
        Path.class)
            .expect(watcher)
            .expect(filewatcherProp(false))
            .expect(filemonitor)
            .expect(fileeventoptions(handler))
            .expect(unit -> {
              FileEventOptions options = unit.get(FileEventOptions.class);
              expect(options.recursive(true)).andReturn(options);
            })
            .run(unit -> {
              FileWatcher watcher = new FileWatcher();
              watcher.register(unit.get(Path.class), handler, options -> {
                options.recursive(true);
              })
                  .configure(unit.get(Env.class), unit.get(Config.class),
                      unit.get(Binder.class));
            });
  }

  @SuppressWarnings({"unchecked", "rawtypes" })
  @Test
  public void registerFromConf() throws Exception {
    new MockUnit(Env.class, Config.class, Binder.class, WatchService.class, FileSystem.class,
        Path.class)
            .expect(watcher)
            .expect(filewatcherProp(true))
            .expect(unit -> {
              Config conf = unit.get(Config.class);
              Map<String, Object> options = ImmutableMap.<String, Object> builder()
                  .put("path", "target")
                  .put("handler", FileEventHandler.class.getName())
                  .put("kind", ImmutableList.of("ENTRY_CREATE", "ENTRY_MODIFY"))
                  .put("recursive", false)
                  .put("includes", "*.cp")
                  .put("modifier", "LOW")
                  .build();
              expect(conf.getAnyRef("filewatcher.register")).andReturn(options);

              unit.mockStatic(Paths.class);
              expect(Paths.get("target")).andReturn(unit.get(Path.class));
            })
            .expect(filemonitor)
            .expect(fileeventoptions(FileEventHandler.class))
            .expect(unit -> {
              FileEventOptions options = unit.get(FileEventOptions.class);
              expect(options.kind(unit.capture(WatchEvent.Kind.class))).andReturn(options);
              expect(options.kind(unit.capture(WatchEvent.Kind.class))).andReturn(options);
              expect(options.recursive(false)).andReturn(options);
              expect(options.includes("*.cp")).andReturn(options);
              expect(options.modifier(unit.capture(WatchEvent.Modifier.class))).andReturn(options);
            })
            .run(unit -> {
              FileWatcher watcher = new FileWatcher();
              watcher.configure(unit.get(Env.class), unit.get(Config.class),
                  unit.get(Binder.class));
            }, unit -> {
              List<Kind> kinds = unit.captured(WatchEvent.Kind.class);
              assertEquals("ENTRY_CREATE", kinds.get(0).name());
              assertEquals("ENTRY_MODIFY", kinds.get(1).name());
              assertEquals("LOW", unit.captured(WatchEvent.Modifier.class).get(0).name());
            });
  }

  @Test
  public void registerPropPath() throws Exception {
    new MockUnit(Env.class, Config.class, Binder.class, WatchService.class, FileSystem.class,
        Path.class)
            .expect(watcher)
            .expect(filewatcherProp(false))
            .expect(pathprop("property"))
            .expect(filemonitor)
            .expect(fileeventoptions(FileEventHandler.class))
            .run(unit -> {
              FileWatcher watcher = new FileWatcher();
              watcher.register("property", FileEventHandler.class)
                  .configure(unit.get(Env.class), unit.get(Config.class),
                      unit.get(Binder.class));
            });
  }

  @Test
  public void registerPropPathInstance() throws Exception {
    FileEventHandler handler = (k, p) -> {
    };
    new MockUnit(Env.class, Config.class, Binder.class, WatchService.class, FileSystem.class,
        Path.class)
            .expect(watcher)
            .expect(filewatcherProp(false))
            .expect(pathprop("property"))
            .expect(filemonitor)
            .expect(fileeventoptions(handler))
            .run(unit -> {
              FileWatcher watcher = new FileWatcher();
              watcher.register("property", handler)
                  .configure(unit.get(Env.class), unit.get(Config.class),
                      unit.get(Binder.class));
            });
  }

  @Test
  public void registerPropPathInstanceOptions() throws Exception {
    FileEventHandler handler = (k, p) -> {
    };
    new MockUnit(Env.class, Config.class, Binder.class, WatchService.class, FileSystem.class,
        Path.class)
            .expect(watcher)
            .expect(filewatcherProp(false))
            .expect(pathprop("property"))
            .expect(filemonitor)
            .expect(fileeventoptions(handler))
            .expect(unit -> {
              FileEventOptions options = unit.get(FileEventOptions.class);
              expect(options.kind(StandardWatchEventKinds.ENTRY_CREATE)).andReturn(options);
            })
            .run(unit -> {
              FileWatcher watcher = new FileWatcher();
              watcher.register("property", handler, options -> {
                options.kind(StandardWatchEventKinds.ENTRY_CREATE);
              }).configure(unit.get(Env.class), unit.get(Config.class),
                  unit.get(Binder.class));
            });
  }

  @Test
  public void registerPropPathWithOptions() throws Exception {
    new MockUnit(Env.class, Config.class, Binder.class, WatchService.class, FileSystem.class,
        Path.class)
            .expect(watcher)
            .expect(filewatcherProp(false))
            .expect(pathprop("prop"))
            .expect(filemonitor)
            .expect(fileeventoptions(FileEventHandler.class))
            .expect(unit -> {
              FileEventOptions options = unit.get(FileEventOptions.class);
              expect(options.kind(StandardWatchEventKinds.ENTRY_CREATE)).andReturn(options);
            })
            .run(unit -> {
              FileWatcher watcher = new FileWatcher();
              watcher.register("prop", FileEventHandler.class, options -> {
                options.kind(StandardWatchEventKinds.ENTRY_CREATE);
              }).configure(unit.get(Env.class), unit.get(Config.class),
                  unit.get(Binder.class));
            });
  }

  @Test
  public void registerPathWithOptions() throws Exception {
    new MockUnit(Env.class, Config.class, Binder.class, WatchService.class, FileSystem.class,
        Path.class)
            .expect(watcher)
            .expect(filewatcherProp(false))
            .expect(filemonitor)
            .expect(fileeventoptions(FileEventHandler.class))
            .expect(unit -> {
              FileEventOptions options = unit.get(FileEventOptions.class);
              expect(options.kind(StandardWatchEventKinds.ENTRY_CREATE)).andReturn(options);
            })
            .run(unit -> {
              FileWatcher watcher = new FileWatcher();
              watcher.register(unit.get(Path.class), FileEventHandler.class, options -> {
                options.kind(StandardWatchEventKinds.ENTRY_CREATE);
              }).configure(unit.get(Env.class), unit.get(Config.class),
                  unit.get(Binder.class));
            });
  }

  @SuppressWarnings({"rawtypes", "unchecked" })
  private Block fileeventoptions(final Object handler) {
    return unit -> {
      Binder binder = unit.get(Binder.class);

      FileEventOptions options;
      if (handler instanceof FileEventHandler) {
        options = unit.constructor(FileEventOptions.class)
            .args(Path.class, FileEventHandler.class)
            .build(unit.get(Path.class), handler);
      } else {
        options = unit.constructor(FileEventOptions.class)
            .args(Path.class, Class.class)
            .build(unit.get(Path.class), handler);
      }
      unit.registerMock(FileEventOptions.class, options);

      LinkedBindingBuilder lbb = unit.mock(LinkedBindingBuilder.class);
      lbb.toInstance(options);

      Multibinder mbinder = unit.mock(Multibinder.class);
      unit.mockStatic(Multibinder.class);
      expect(Multibinder.newSetBinder(binder, FileEventOptions.class)).andReturn(mbinder);
      expect(mbinder.addBinding()).andReturn(lbb);
    };
  }

  private Block filewatcherProp(final boolean b) {
    return unit -> {
      Config config = unit.get(Config.class);
      expect(config.hasPath("filewatcher.register")).andReturn(b);
    };
  }

  private Block pathprop(final String name) {
    return unit -> {
      Config conf = unit.get(Config.class);
      expect(conf.getString(name)).andReturn("target");
      unit.mockStatic(Paths.class);
      expect(Paths.get("target")).andReturn(unit.get(Path.class));
    };
  }
}
