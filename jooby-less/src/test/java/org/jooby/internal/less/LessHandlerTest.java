package org.jooby.internal.less;

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.isA;
import static org.junit.Assert.assertEquals;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import org.jooby.Asset;
import org.jooby.MediaType;
import org.jooby.Request;
import org.jooby.Response;
import org.jooby.Result;
import org.jooby.test.MockUnit;
import org.jooby.test.MockUnit.Block;
import org.junit.Test;

import com.github.sommeri.less4j.LessCompiler;
import com.github.sommeri.less4j.LessCompiler.CompilationResult;
import com.github.sommeri.less4j.LessCompiler.Problem;
import com.github.sommeri.less4j.LessSource;

public class LessHandlerTest {

  private URL resource;

  public LessHandlerTest() throws MalformedURLException {
    this.resource = new File("target/missing.less").toURI().toURL();
  }

  private Block asset = unit -> {
    Asset asset = unit.get(Asset.class);
    expect(asset.resource()).andReturn(resource);
  };

  private Block utf8 = unit -> {
    Request req = unit.get(Request.class);
    expect(req.charset()).andReturn(StandardCharsets.UTF_8);
  };

  private Block lessSource = unit -> {
    LessCompiler compiler = unit.get(LessCompiler.class);
    expect(compiler.compile(isA(LessSource.URLSource.class)))
        .andReturn(unit.get(CompilationResult.class));
  };

  private Block warnings = unit -> {
    CompilationResult result = unit.get(CompilationResult.class);
    Problem warning = unit.mock(Problem.class);
    List<Problem> warnings = Arrays.asList(warning);
    expect(result.getWarnings()).andReturn(warnings);
  };

  @Test
  public void sendLess() throws Exception {
    new MockUnit(LessCompiler.class, Request.class, Response.class, Asset.class,
        CompilationResult.class)
        .expect(asset)
        .expect(utf8)
        .expect(unit -> {
          Request req = unit.get(Request.class);
          expect(req.path()).andReturn("/css/less.css");
        })
        .expect(unit -> {
          Response rsp = unit.get(Response.class);
          expect(rsp.type(MediaType.css)).andReturn(rsp);
        })
        .expect(unit -> {
          Response rsp = unit.get(Response.class);
          rsp.send(unit.capture(Result.class));
        })
        .expect(lessSource)
        .expect(warnings)
        .expect(unit -> {
          CompilationResult result = unit.get(CompilationResult.class);
          expect(result.getCss()).andReturn("body {}");
        })
        .run(unit -> {
          new LessHandler("/css/**", unit.get(LessCompiler.class))
              .send(unit.get(Request.class), unit.get(Response.class), unit.get(Asset.class));
        }, unit -> {
          String css = unit.captured(Result.class).iterator().next().get().get().toString();
          assertEquals("body {}", css);
        });
  }

  @Test
  public void sendSourceMap() throws Exception {
    new MockUnit(LessCompiler.class, Request.class, Response.class, Asset.class,
        CompilationResult.class)
        .expect(asset)
        .expect(utf8)
        .expect(unit -> {
          Request req = unit.get(Request.class);
          expect(req.path()).andReturn("/css/less.css.map");
        })
        .expect(unit -> {
          Response rsp = unit.get(Response.class);
          expect(rsp.type(MediaType.json)).andReturn(rsp);
        })
        .expect(unit -> {
          Response rsp = unit.get(Response.class);
          rsp.send(unit.capture(Result.class));
        })
        .expect(lessSource)
        .expect(warnings)
        .expect(unit -> {
          CompilationResult result = unit.get(CompilationResult.class);
          expect(result.getSourceMap()).andReturn("{}");
        })
        .run(unit -> {
          new LessHandler("/css/**", unit.get(LessCompiler.class))
              .send(unit.get(Request.class), unit.get(Response.class), unit.get(Asset.class));
        }, unit -> {
          String sourceMap = unit.captured(Result.class).iterator().next().get().get().toString();
          assertEquals("{}", sourceMap);
        });
  }

}
