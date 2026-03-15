module io.jooby.langchain4j {
  exports io.jooby.langchain4j;

  requires io.jooby;
  requires static com.github.spotbugs.annotations;
  requires org.slf4j;
  requires typesafe.config;
  requires langchain4j.core;

  // Optional provider modules
  requires static langchain4j.open.ai;
  requires static langchain4j.anthropic;
  requires static langchain4j.ollama;
  requires static langchain4j.jlama;
}
