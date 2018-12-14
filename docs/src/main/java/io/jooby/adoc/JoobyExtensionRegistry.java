package io.jooby.adoc;

import org.asciidoctor.Asciidoctor;
import org.asciidoctor.extension.spi.ExtensionRegistry;

public class JoobyExtensionRegistry implements ExtensionRegistry {
  @Override public void register(Asciidoctor asciidoctor) {
    asciidoctor.javaExtensionRegistry().block("dependency", DependencyProcessor.class);
    asciidoctor.javaExtensionRegistry().inlineMacro("javadoc", JavadocProcessor.class);
  }
}
