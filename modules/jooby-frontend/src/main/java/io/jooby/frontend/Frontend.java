/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.frontend;

import com.github.eirslett.maven.plugins.frontend.lib.CacheResolver;
import com.github.eirslett.maven.plugins.frontend.lib.FrontendPluginFactory;
import com.github.eirslett.maven.plugins.frontend.lib.InstallationException;
import com.github.eirslett.maven.plugins.frontend.lib.NodeInstaller;
import com.github.eirslett.maven.plugins.frontend.lib.ProxyConfig;
import com.typesafe.config.Config;
import io.jooby.Environment;
import io.jooby.Extension;
import io.jooby.Jooby;
import io.jooby.ServerOptions;
import io.jooby.internal.frontend.MavenCacheResolver;
import io.jooby.internal.frontend.NodeTaskRunner;

import javax.annotation.Nonnull;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static com.typesafe.config.ConfigFactory.parseResources;
import static io.jooby.SneakyThrows.throwingConsumer;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Objects.requireNonNull;
import static java.util.Optional.ofNullable;

/**
 * Base class for Npm and Yarn installer.
 *
 * @author edgar
 * @since 2.8.0
 */
public abstract class Frontend implements Extension {

  protected final String nodeVersion;
  private Path workDirectory;
  private Path installDirectory;
  private List<Consumer<NodeExecutor>> commands = new ArrayList<>();

  protected Frontend(@Nonnull String nodeVersion) {
    this.nodeVersion = requireNonNull(nodeVersion, "Node version required.");
  }

  /**
   * Set working directory (location of package.json).
   *
   * @param workDirectory Work directory.
   * @return This module.
   */
  public @Nonnull Frontend workDirectory(@Nonnull Path workDirectory) {
    this.workDirectory = requireNonNull(workDirectory, "Work directory required.");
    return this;
  }

  /**
   * Set install directory where to save the node executable (node/node).
   *
   * @param installDirectory Location where to save the node executable (node/node).
   * @return This module.
   */
  public @Nonnull Frontend installDirectory(@Nonnull Path installDirectory) {
    this.installDirectory = requireNonNull(installDirectory, "Install directory required.");
    return this;
  }

  /**
   * Execute a task.
   *
   * @param cmd Command.
   * @param args Arguments.
   * @return This module.
   */
  public @Nonnull Frontend execute(@Nonnull String cmd, @Nonnull String... args) {
    commands.add(executor -> executor.execute(cmd, args));
    return this;
  }

  /**
   * Execute a task once per JVM.
   *
   * @param cmd Command.
   * @param args Arguments.
   * @return This module.
   */
  public @Nonnull Frontend executeOnce(@Nonnull String cmd, @Nonnull String... args) {
    commands.add(executor -> executor.executeOnce(cmd, args));
    return this;
  }

  /**
   * Execute a task asynchronously.
   *
   * @param cmd Command.
   * @param args Arguments.
   * @return This module.
   */
  public @Nonnull Frontend executeAsync(@Nonnull String cmd, @Nonnull String... args) {
    commands.add(executor -> executor.executeAsync(cmd, args));
    return this;
  }

  /**
   * Execute a task asynchronously once per JVM.
   *
   * @param cmd Command.
   * @param args Arguments.
   * @return This module.
   */
  public @Nonnull Frontend executeAsyncOnce(@Nonnull String cmd, @Nonnull String... args) {
    commands.add(executor -> executor.executeAsyncOnce(cmd, args));
    return this;
  }

  @Override public void install(@Nonnull Jooby application) throws Exception {
    Config conf = application.getConfig()
        .withFallback(parseResources(getClass(), "frontend.conf"));

    Path userdir = Paths.get(System.getProperty("user.dir"));
    Path workdir = ofNullable(this.workDirectory).orElse(userdir);

    Path installdir = ofNullable(this.installDirectory).orElse(workdir);

    ProxyConfig proxy = new ProxyConfig(proxies(conf));

    CacheResolver cache = Files.exists(userdir.resolve("pom.xml"))
        ? new MavenCacheResolver()
        : null;

    FrontendPluginFactory factory = new FrontendPluginFactory(workdir.toFile(),
        installdir.toFile(), cache);

    installNode(conf, nodeVersion, proxy, factory);

    Map<String, String> environment = environment(application);

    NodeTaskRunner runner = newTaskRunner(factory, application.getEnvironment(), proxy);

    String name = getClass().getSimpleName().toLowerCase();
    NodeExecutor.Task task = (cmd, args) -> {
      StringBuilder cmdline = new StringBuilder(cmd).append(" ");
      Stream.of(args).forEach(arg -> cmdline.append(arg).append(" "));
      String command = cmdline.toString().trim();
      try {
        runner.execute(command, environment);
      } catch (Exception x) {
        application.getLog().error("{} resulted in exception: {}", name, command, x);
      }
    };

    application.onStarted(() -> {

      checkAndSyncPackageJson(workdir, application.getTmpdir(), task);

      if (commands.isEmpty()) {
        task.execute("run", "build");
      } else {
        NodeExecutor executor = options -> {
          NodeExecutor.Task result = task;
          for (NodeExecutor.Option option : options) {
            result = option.toTask(result);
          }
          return result;
        };
        commands.forEach(cmd -> cmd.accept(executor));
      }
    });
  }

  private static Map<String, String> environment(Jooby application) {
    Environment env = application.getEnvironment();
    Map<String, String> vars = new HashMap<>(System.getenv());
    env.getConfig().entrySet()
        .forEach(e -> vars.put(e.getKey(), e.getValue().unwrapped().toString()));
    vars.put("NODE_ENV", env.isActive("dev", "test") ? "development" : "production");
    Integer port = ofNullable(application.getServerOptions())
        .map(ServerOptions::getPort)
        .orElse(ServerOptions.DEFAULT_PORT);
    vars.put("server.port", port.toString());
    vars.put("application.path", application.getContextPath());
    return vars;
  }

  private static void checkAndSyncPackageJson(Path workdir, Path tmpdir,
      NodeExecutor.Task task) throws Exception {
    if (Files.exists(workdir.resolve("package.json"))) {
      Path tmp = tmpdir.resolve("package.json");
      Files.createDirectories(tmp);
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] bytes = digest.digest(Files.readAllBytes(workdir.resolve("package.json")));
      String sha1 = Long.toHexString(UUID.nameUUIDFromBytes(bytes).getMostSignificantBits());
      Path lastSha1 = tmp.resolve(sha1);
      if (!Files.exists(lastSha1) || !Files.exists(workdir.resolve("node_modules"))) {
        task.execute("install");
        try (Stream<Path> files = Files.walk(tmp)) {
          files.filter(f -> !f.equals(tmp)).forEach(throwingConsumer(Files::deleteIfExists));
        }
        Files.write(tmp.resolve(lastSha1), emptyList());
      }
    }
  }

  private void installNode(Config conf, String version, ProxyConfig proxy,
      FrontendPluginFactory factory) throws InstallationException {
    NodeInstaller installer = factory.getNodeInstaller(proxy)
        .setNodeVersion(version)
        .setNodeDownloadRoot(conf.getString("node.downloadRoot"));

    installNpm(installer);
    installer.install();
  }

  protected void installNpm(NodeInstaller node) {
    // NOOP
  }

  private static List<ProxyConfig.Proxy> proxies(Config conf) {
    if (conf.hasPath("proxy")) {
      return singletonList(
          new ProxyConfig.Proxy(
              conf.getString("proxy.id"),
              conf.getString("proxy.protocol"),
              conf.getString("proxy.host"),
              conf.getInt("proxy.port"),
              conf.getString("proxy.username"),
              conf.getString("proxy.password"),
              conf.getString("proxy.nonProxyHosts")
          )
      );
    }
    return emptyList();
  }

  protected abstract NodeTaskRunner newTaskRunner(FrontendPluginFactory factory, Environment env,
      ProxyConfig proxy) throws InstallationException;
}
