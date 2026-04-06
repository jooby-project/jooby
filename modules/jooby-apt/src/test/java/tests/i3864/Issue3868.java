/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package tests.i3864;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import io.jooby.apt.ProcessorRunner;

public class Issue3868 {
  @Test
  public void topLevelAnnotationMakeAllPublicJSONRPC() throws Exception {
    new ProcessorRunner(new DefaultMapping())
        .withRpcCode(
            source -> {
              assertThat(source)
                  .contains(
                      "public class DefaultMappingRpc_ implements"
                          + " io.jooby.jsonrpc.JsonRpcService, io.jooby.Extension {")
                  .contains("public java.util.List<String> getMethods() {")
                  .contains(
                      "return java.util.List.of(\"default.rpcMethod1\", \"default.rpcMethod2\")")
                  .contains(
                      "public Object execute(io.jooby.Context ctx,"
                          + " io.jooby.jsonrpc.JsonRpcRequest req) throws Exception {");
            });
  }

  @Test
  public void emptyNamespaceMustGenerateMethodNameOnly() throws Exception {
    new ProcessorRunner(new EmptyNamespace())
        .withRpcCode(
            source -> {
              assertThat(source)
                  .contains("return java.util.List.of(\"rpcMethod1\", \"rpcMethod2\")");
            });
  }

  @Test
  public void explicitMappingTurnOffDefaultMapping() throws Exception {
    new ProcessorRunner(new ExplicitMapping())
        .withRpcCode(
            source -> {
              assertThat(source).contains("return java.util.List.of(\"explicit.onlyThis\")");
            });
  }

  @Test
  public void customNaming() throws Exception {
    new ProcessorRunner(new CustomNaming())
        .withRpcCode(
            source -> {
              assertThat(source)
                  .contains("return java.util.List.of(\"movies.getById\", \"movies.create\")");
            });
  }

  @Test
  public void shouldInjectContext() throws Exception {
    new ProcessorRunner(new WithContext())
        .withRpcCode(
            source -> {
              assertThat(source).contains("return c.rpcMethod1(ctx, value);");
            });
  }

  @Test
  public void shouldFollowNullability() throws Exception {
    new ProcessorRunner(new NullSupport())
        .withRpcCode(
            source -> {
              assertThat(source)
                  .contains(
                      "return java.util.List.of(\"nullableInt\", \"requiredInt\","
                          + " \"requiredString\");")
                  // (Integer nullInt)
                  .contains(
                      "var nullInt = reader.nextIsNull(\"nullInt\") ? null :"
                          + " reader.nextInt(\"nullInt\");")
                  // (int nonnullInt)
                  .contains("var nonnullInt = reader.nextInt(\"nonnullInt\");")
                  // (@Nonnull String nonnullStr)
                  .contains("var nonnullStr = reader.nextString(\"nonnullStr\");");
            });
  }

  @Test
  public void shouldGenerateDefaultConstructorForDI() throws Exception {
    new ProcessorRunner(new DIService(null))
        .withRpcCode(
            source -> {
              assertThat(source)
                  .containsIgnoringWhitespaces(
                      "public DIServiceRpc_() {\n" + "    this(DIService.class);\n" + "  }")
                  .containsIgnoringWhitespaces(
                      "public DIServiceRpc_(Class<DIService> type) {\n"
                          + "    setup(ctx -> ctx.require(type));\n"
                          + "  }");
            });
  }
}
