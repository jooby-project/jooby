package io.jooby.junit;

import io.jooby.ExecutionMode;
import io.jooby.jetty.Jetty;
import io.jooby.netty.Netty;
import io.jooby.utow.Utow;

import org.junit.jupiter.api.extension.Extension;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestTemplateInvocationContext;
import org.junit.jupiter.api.extension.TestTemplateInvocationContextProvider;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.junit.platform.commons.util.AnnotationUtils.isAnnotated;

public class ServerExtensionImpl implements TestTemplateInvocationContextProvider {

  private static class ServerInfo implements Comparable<ServerInfo> {
    private final int index;
    private ServerProvider server;

    private ExecutionMode mode;

    private String description;

    public ServerInfo(ServerProvider server, ExecutionMode mode, int index, String description) {
      this.server = server;
      this.mode = mode;
      this.description = description;
      this.index = index;
    }

    @Override public int compareTo(ServerInfo o) {
      int diff = description.compareTo(o.description);
      if (diff == 0) {
        return index - o.index;
      }
      return diff;
    }
  }

  private static final Class[] SERVERS = {Jetty.class, Netty.class, Utow.class};

  @Override public boolean supportsTestTemplate(ExtensionContext context) {
    return isAnnotated(context.getTestMethod(), ServerTest.class);
  }

  @Override public Stream<TestTemplateInvocationContext> provideTestTemplateInvocationContexts(
      ExtensionContext context) {
    ServerTest serverTest = context.getRequiredTestMethod().getAnnotation(ServerTest.class);
    Class[] servers = serverTest.server();
    if (servers.length == 0) {
      servers = SERVERS;
    }
    Set<ExecutionMode> executionModes = EnumSet.copyOf(Arrays.asList(serverTest.executionMode()));
    if (executionModes.contains(ExecutionMode.DEFAULT) && executionModes.size() == 1) {
      executionModes.remove(ExecutionMode.DEFAULT);
    }
    int repetitions = serverTest.iterations();
    return Stream.of(servers)
        .flatMap(it -> {
          List<ServerInfo> serverInfos = new ArrayList<>();
          IntStream.range(0, repetitions)
              .forEach(i -> {
                if (executionModes.isEmpty()) {
                  serverInfos.add(
                      new ServerInfo(
                          new ServerProvider(it),
                          null,
                          i,
                          displayName(it, null, i, repetitions)
                      ));
                } else {
                  executionModes.stream()
                      .map(mode ->
                          new ServerInfo(
                              new ServerProvider(it),
                              mode,
                              i,
                              displayName(it, mode, i, repetitions)
                          )
                      ).forEach(serverInfos::add);
                }
              });

          return serverInfos.stream();
        })
        .sorted()
        .map(info -> invocationContext(info));
  }

  private TestTemplateInvocationContext invocationContext(ServerInfo serverInfo) {
    return new TestTemplateInvocationContext() {
      @Override
      public String getDisplayName(int invocationIndex) {
        return serverInfo.description;
      }

      @Override
      public List<Extension> getAdditionalExtensions() {
        return Arrays.asList(new ServerParameterResolver(serverInfo.server, serverInfo.mode));
      }
    };
  }

  private static String displayName(Class server, ExecutionMode mode, int i, int total) {
    StringBuilder displayName = new StringBuilder(server.getSimpleName().toLowerCase());
    if (mode != null) {
      displayName.append(".").append(mode.name().toLowerCase());
    }
    if (total > 1) {
      displayName.append("[").append(i + 1).append("]");
    }
    return displayName.toString();
  }
}
