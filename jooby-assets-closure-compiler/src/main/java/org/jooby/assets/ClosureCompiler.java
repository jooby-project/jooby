/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.jooby.assets;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.stream.Collectors;

import org.jooby.MediaType;
import org.slf4j.bridge.SLF4JBridgeHandler;

import com.google.common.collect.ImmutableList;
import com.google.javascript.jscomp.CheckLevel;
import com.google.javascript.jscomp.ClosureCodingConvention;
import com.google.javascript.jscomp.CommandLineRunner;
import com.google.javascript.jscomp.CompilationLevel;
import com.google.javascript.jscomp.Compiler;
import com.google.javascript.jscomp.CompilerOptions;
import com.google.javascript.jscomp.DiagnosticGroups;
import com.google.javascript.jscomp.Result;
import com.google.javascript.jscomp.SourceFile;
import com.typesafe.config.Config;

/**
 * <h1>closure-compiler</h1>
 * <p>
 * <a href="https://developers.google.com/closure/compiler">Closure Compiler</a> is a tool
 * for making JavaScript download and run faster.
 * </p>
 *
 * <h2>usage</h2>
 *
 * <pre>
 * assets {
 *   fileset {
 *     home: ...
 *   }
 *
 *   pipeline {
 *     ...
 *     dist: [closure-compiler]
 *   }
 * }
 * </pre>
 *
 * <h2>options</h2>
 * <pre>
 * assets {
 *   ...
 *   closure-compiler {
 *     level: advanced
 *   }
 * }
 * </pre>
 *
 * @author edgar
 * @since 0.11.0
 */
public class ClosureCompiler extends AssetProcessor {

  static {
    if (!SLF4JBridgeHandler.isInstalled()) {
      SLF4JBridgeHandler.removeHandlersForRootLogger();
      SLF4JBridgeHandler.install();
    }
  }

  {
    set("level", "simple");
  }

  @Override
  public boolean matches(final MediaType type) {
    return MediaType.css.matches(type);
  }

  @Override
  public String process(final String filename, final String source, final Config conf)
      throws Exception {
    final CompilerOptions copts = new CompilerOptions();
    copts.setCodingConvention(new ClosureCodingConvention());
    copts.setOutputCharset("UTF-8");
    copts.setWarningLevel(DiagnosticGroups.CHECK_VARIABLES, CheckLevel.WARNING);
    CompilationLevel level = level(get("level"));
    level.setOptionsForCompilationLevel(copts);

    Compiler.setLoggingLevel(Level.SEVERE);
    Compiler compiler = new Compiler();
    compiler.disableThreads();
    compiler.initOptions(copts);

    List<SourceFile> externs = externs(copts);
    Result result = compiler.compile(externs,
        ImmutableList.of(SourceFile.fromCode(filename, source)), copts);
    if (result.success) {
      return compiler.toSource();
    }
    List<AssetProblem> errors = Arrays.stream(result.errors)
        .map(error -> new AssetProblem(error.sourceName, error.lineNumber, error.getCharno(),
            error.description))
        .collect(Collectors.toList());
    throw new AssetException(errors);
  }

  private CompilationLevel level(final String level) {
    switch (level.toLowerCase()) {
      case "simple":
        return CompilationLevel.SIMPLE_OPTIMIZATIONS;
      case "advanced":
        return CompilationLevel.ADVANCED_OPTIMIZATIONS;
      case "ws":
        return CompilationLevel.WHITESPACE_ONLY;
    }
    throw new IllegalArgumentException("Unknown compilation level: " + level);
  }

  private List<SourceFile> externs(final CompilerOptions coptions) throws IOException {
    List<SourceFile> externs = CommandLineRunner.getBuiltinExterns(coptions);
    List<String> local = get("externs");
    if (local != null) {
      Class<?> loader = getClass();
      for (String js : local) {
        String path = js.startsWith("/") ? js : "/" + js;
        try (InputStream stream = loader.getResourceAsStream(path)) {
          if (stream == null) {
            throw new FileNotFoundException(path);
          }
          externs.add(SourceFile.fromInputStream(path, stream, StandardCharsets.UTF_8));
        }
      }
    }
    return externs;
  }

}
