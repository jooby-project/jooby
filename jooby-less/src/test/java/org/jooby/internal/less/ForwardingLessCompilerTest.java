package org.jooby.internal.less;

import static org.easymock.EasyMock.expect;

import java.io.File;
import java.net.URL;

import org.jooby.test.MockUnit;
import org.junit.Test;

import com.github.sommeri.less4j.LessCompiler;
import com.github.sommeri.less4j.LessCompiler.CompilationResult;
import com.github.sommeri.less4j.LessCompiler.Configuration;
import com.github.sommeri.less4j.LessSource;

public class ForwardingLessCompilerTest {

  @Test
  public void compileFile() throws Exception {
    File input = new File("target/missing.less");
    new MockUnit(LessCompiler.class, Configuration.class, CompilationResult.class)
        .expect(unit -> {
          LessCompiler compiler = unit.get(LessCompiler.class);

          expect(compiler.compile(input, unit.get(Configuration.class)))
              .andReturn(unit.get(CompilationResult.class));
        })
        .run(unit -> {
          new ForwardingLessCompiler(unit.get(LessCompiler.class), unit.get(Configuration.class))
              .compile(input);
        });
  }

  @Test
  public void compileFileWithOptions() throws Exception {
    File input = new File("target/missing.less");
    Configuration options = new Configuration();
    new MockUnit(LessCompiler.class, Configuration.class, CompilationResult.class)
        .expect(unit -> {
          LessCompiler compiler = unit.get(LessCompiler.class);

          expect(compiler.compile(input, options))
              .andReturn(unit.get(CompilationResult.class));
        })
        .run(unit -> {
          new ForwardingLessCompiler(unit.get(LessCompiler.class), unit.get(Configuration.class))
              .compile(input, options);
        });
  }

  @Test
  public void compileSource() throws Exception {
    LessSource.FileSource input = new LessSource.FileSource(new File("target/missing.less"));
    new MockUnit(LessCompiler.class, Configuration.class, CompilationResult.class)
        .expect(unit -> {
          LessCompiler compiler = unit.get(LessCompiler.class);

          expect(compiler.compile(input, unit.get(Configuration.class)))
              .andReturn(unit.get(CompilationResult.class));
        })
        .run(unit -> {
          new ForwardingLessCompiler(unit.get(LessCompiler.class), unit.get(Configuration.class))
              .compile(input);
        });
  }

  @Test
  public void compileSourceWithOptions() throws Exception {
    LessSource.FileSource input = new LessSource.FileSource(new File("target/missing.less"));
    Configuration options = new Configuration();
    new MockUnit(LessCompiler.class, Configuration.class, CompilationResult.class)
        .expect(unit -> {
          LessCompiler compiler = unit.get(LessCompiler.class);

          expect(compiler.compile(input, options))
              .andReturn(unit.get(CompilationResult.class));
        })
        .run(unit -> {
          new ForwardingLessCompiler(unit.get(LessCompiler.class), unit.get(Configuration.class))
              .compile(input, options);
        });
  }

  @Test
  public void compileURL() throws Exception {
    URL input = new File("target/missing.less").toURI().toURL();
    new MockUnit(LessCompiler.class, Configuration.class, CompilationResult.class)
        .expect(unit -> {
          LessCompiler compiler = unit.get(LessCompiler.class);

          expect(compiler.compile(input, unit.get(Configuration.class)))
              .andReturn(unit.get(CompilationResult.class));
        })
        .run(unit -> {
          new ForwardingLessCompiler(unit.get(LessCompiler.class), unit.get(Configuration.class))
              .compile(input);
        });
  }

  @Test
  public void compileURLWithOptions() throws Exception {
    URL input = new File("target/missing.less").toURI().toURL();
    Configuration options = new Configuration();
    new MockUnit(LessCompiler.class, Configuration.class, CompilationResult.class)
        .expect(unit -> {
          LessCompiler compiler = unit.get(LessCompiler.class);

          expect(compiler.compile(input, options))
              .andReturn(unit.get(CompilationResult.class));
        })
        .run(unit -> {
          new ForwardingLessCompiler(unit.get(LessCompiler.class), unit.get(Configuration.class))
              .compile(input, options);
        });
  }

  @Test
  public void compileString() throws Exception {
    String input = "body{}";
    new MockUnit(LessCompiler.class, Configuration.class, CompilationResult.class)
        .expect(unit -> {
          LessCompiler compiler = unit.get(LessCompiler.class);

          expect(compiler.compile(input, unit.get(Configuration.class)))
              .andReturn(unit.get(CompilationResult.class));
        })
        .run(unit -> {
          new ForwardingLessCompiler(unit.get(LessCompiler.class), unit.get(Configuration.class))
              .compile(input);
        });
  }

  @Test
  public void compileStringWithOptions() throws Exception {
    String input = "body{}";
    Configuration options = new Configuration();
    new MockUnit(LessCompiler.class, Configuration.class, CompilationResult.class)
        .expect(unit -> {
          LessCompiler compiler = unit.get(LessCompiler.class);

          expect(compiler.compile(input, options))
              .andReturn(unit.get(CompilationResult.class));
        })
        .run(unit -> {
          new ForwardingLessCompiler(unit.get(LessCompiler.class), unit.get(Configuration.class))
              .compile(input, options);
        });
  }

}
