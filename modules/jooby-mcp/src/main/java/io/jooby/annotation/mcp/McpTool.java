/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.annotation.mcp;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Exposes a method as an MCP Tool. */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface McpTool {
  /**
   * The name of the tool. If empty, the method name is used.
   *
   * @return Tool name.
   */
  String name() default "";

  /**
   * Intended for UI and end-user contexts — optimized to be human-readable and easily understood,
   * even by those unfamiliar with domain-specific terminology. If not provided, the name should be
   * used for display (except for Tool, where annotations.title should be given precedence over
   * using name, if present).
   */
  String title() default "";

  /**
   * A description of what the tool does. Highly recommended for LLM usage.
   *
   * @return Tool description.
   */
  String description() default "";

  /** Additional hints for clients. */
  McpAnnotations annotations() default @McpAnnotations;

  /**
   * Additional properties describing a Tool to clients.
   *
   * <p>All properties in ToolAnnotations are hints. They are not guaranteed to provide a faithful
   * description of tool behavior (including descriptive properties like title).
   *
   * <p>Clients should never make tool use decisions based on ToolAnnotations received from
   * untrusted servers.
   */
  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.ANNOTATION_TYPE)
  @interface McpAnnotations {
    /** If true, the tool does not modify its environment. */
    boolean readOnlyHint() default false;

    /**
     * If true, the tool may perform destructive updates to its environment. If false, the tool
     * performs only additive updates.
     *
     * <p>(This property is meaningful only when readOnlyHint == false)
     */
    boolean destructiveHint() default true;

    /**
     * If true, calling the tool repeatedly with the same arguments will have no additional effect
     * on the its environment.
     *
     * <p>(This property is meaningful only when readOnlyHint == false)
     */
    boolean idempotentHint() default false;

    /**
     * If true, this tool may interact with an “open world” of external entities. If false, the
     * tool’s domain of interaction is closed. For example, the world of a web search tool is open,
     * whereas that of a memory tool is not.
     */
    boolean openWorldHint() default true;
  }
}
