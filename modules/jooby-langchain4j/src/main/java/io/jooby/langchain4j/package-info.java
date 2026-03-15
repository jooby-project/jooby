/**
 * Jooby Extension for LangChain4j.
 *
 * <p>This module provides seamless integration between Jooby and the LangChain4j 1.x ecosystem,
 * automatically instantiating and registering {@link dev.langchain4j.model.chat.ChatModel} and
 * {@link dev.langchain4j.model.chat.StreamingChatModel} components based on your application
 * configuration.
 *
 * <h3>Installation</h3>
 *
 * <p>Install the module inside your Jooby application:
 *
 * <pre>{@code
 * {
 *   install(new LangChain4jModule());
 *   get("/chat", ctx -> {
 *     ChatModel ai = require(ChatModel.class);
 *     return ai.chat("Hello world!");
 *   });
 *   }
 * }</pre>
 *
 * <h3>Configuration</h3>
 *
 * <p>Models are defined in your {@code application.conf} under the {@code langchain4j.models} key.
 * The module automatically creates both blocking and streaming interfaces if the provider supports
 * them.
 *
 * <pre>{@code
 * langchain4j {
 *   models {
 *   # The name of the registered service will be "gpt-assistant"
 *     gpt-assistant {
 *       provider = "openai"
 *       api-key = ${OPENAI_API_KEY}
 *       model-name = "gpt-4o-mini"
 *       timeout = 30s
 *       temperature = 0.7
 *    }
 *   }
 * }
 * }</pre>
 *
 * <h3>Resilience & Fallback Routing</h3>
 *
 * <p>You can define a chain of fallbacks to ensure high availability. If the primary model fails
 * (e.g., due to rate limits or network timeouts), the module automatically and silently routes the
 * request to the next configured fallback.
 *
 * <pre>{@code
 * langchain4j.models {
 *   primary-agent {
 *     provider = "openai"
 *     api-key = "..."
 *     fallback = ["local-failover"]
 *   }
 *   local-failover {
 *     provider = "jlama"
 *     model-name = "tjake/Llama-3.2-1B-Instruct-JQ4"
 *   }
 * }
 * }</pre>
 *
 * <p>To track when these failovers occur, attach a listener during module installation:
 *
 * <pre>{@code
 * install(new LangChain4jModule()
 *   .failoverListener((modelName, error) -> {
 *     log.warn("Model {} failed, switching to fallback. Reason: {}", modelName, error.getMessage());
 *    })
 * );
 * }</pre>
 *
 * <h3>Custom Providers</h3>
 *
 * <p>The extension includes built-in support for popular providers like OpenAI, Anthropic, Ollama,
 * and Jlama. To add support for an unlisted provider (e.g., Google Vertex AI), register a custom
 * {@link io.jooby.langchain4j.ChatModelFactory}:
 *
 * <pre>{@code
 * install(new LangChain4jModule()
 *   .register("vertex", new ChatModelFactory() {
 *     @Override
 *     public ChatModel createChatModel(Config config) {
 *       return VertexAiGeminiChatModel.builder()
 *          .project(config.getString("project"))
 *          .location(config.getString("location"))
 *          .build();
 *     }
 *   }));
 * }</pre>
 *
 * <h3>Dependency Management</h3>
 *
 * <p>To keep your application lightweight, the heavy provider SDKs (like {@code
 * langchain4j-open-ai} or {@code langchain4j-jlama}) are marked as <strong>optional</strong>. You
 * must explicitly add the dependencies for the specific providers you intend to use to your
 * project's {@code pom.xml} or {@code build.gradle}.
 *
 * @author edgar
 * @since 4.1.0
 */
@edu.umd.cs.findbugs.annotations.ReturnValuesAreNonnullByDefault
package io.jooby.langchain4j;
