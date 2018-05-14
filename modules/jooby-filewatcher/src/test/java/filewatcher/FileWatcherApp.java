package filewatcher;

import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;

import org.jooby.Jooby;
import org.jooby.filewatcher.FileWatcher;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.typesafe.config.ConfigFactory;

public class FileWatcherApp extends Jooby {

  {
    use(ConfigFactory.parseMap(ImmutableMap.of("filewatcher.register",
        ImmutableList.of(
            ImmutableMap.of("path", "workdir/hashmap", "handler",
                MyFileEventHandler.class.getName()),
            ImmutableMap.of("path", "workdir/f2", "handler", MyFileEventHandler.class.getName())),
        "fprop", "workdir/2nf")));

    use(new FileWatcher()
        .register(Paths.get("workdir", "watchme"), MyFileEventHandler.class)
        .register("fprop", MyFileEventHandler.class, options -> {
          options.recursive(false);
        }).register(Paths.get("workdir", "kt"), MyFileEventHandler.class, options -> {
          options.includes("**/*.kt");
          options.includes("**/*.cp");
          options.kind(StandardWatchEventKinds.ENTRY_MODIFY);
        }));
  }

  public static void main(final String[] args) {
    run(FileWatcherApp::new, args);
  }

}
