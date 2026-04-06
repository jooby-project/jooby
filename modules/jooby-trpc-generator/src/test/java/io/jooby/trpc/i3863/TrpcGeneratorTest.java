/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.trpc.i3863;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.jupiter.api.Test;

import io.jooby.trpc.TrpcGenerator;

class TrpcGeneratorTest {

  @Test
  void shouldGenerateTrpcRouterAndModels() throws Exception {
    var generator = new TrpcGenerator();
    var outputDir = Paths.get("target");

    // Dynamically locate the test-classes directory where the sample is compiled
    var testClassesDir =
        Paths.get(C3863.class.getProtectionDomain().getCodeSource().getLocation().toURI());

    generator.setOutputDir(outputDir);
    generator.setOutputFile("api.d.ts");

    generator.generate();

    var outputFile = outputDir.resolve("api.d.ts");
    assertTrue(Files.exists(outputFile), "TypeScript file should be generated");

    var actualContent = Files.readString(outputFile);

    var expectedContent =
        """
        /* tslint:disable */
        /* eslint-disable */

        export interface U3863 {
            id: string;
            name: string;
        }

        // --- tRPC Router Mapping ---

        export type AppRouter = {
          users: {
            // queries
            getUser: { input: string; output: U3863 };

            // mutations
            createFuture: { input: U3863; output: U3863 };
            createMono: { input: U3863; output: U3863 };
            createUser: { input: U3863; output: U3863 };
          };
        };
        """;

    // Strip out the dynamic timestamp comment line
    var cleanActual =
        actualContent.replaceAll("// Generated using typescript-generator.*\\r?\\n", "");

    // Assert with normalized newlines to avoid \r\n vs \n test flakes
    assertThat(cleanActual).isEqualToNormalizingNewlines(expectedContent);
  }
}
