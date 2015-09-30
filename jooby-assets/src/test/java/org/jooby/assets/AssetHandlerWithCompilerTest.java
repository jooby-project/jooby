package org.jooby.assets;

import static org.easymock.EasyMock.expect;

import org.jooby.Asset;
import org.jooby.Request;
import org.jooby.Response;
import org.jooby.test.MockUnit;
import org.junit.Test;

public class AssetHandlerWithCompilerTest {

  @Test
  public void defaults() throws Exception {
    new MockUnit(Request.class, Response.class, Asset.class, AssetCompiler.class)
    .expect(unit -> {
      Asset asset = unit.get(Asset.class);
      Asset newAsset = unit.mock(Asset.class);

      AssetCompiler compiler = unit.get(AssetCompiler.class);
      expect(compiler.build(asset)).andReturn(newAsset);

      unit.get(Response.class).send(newAsset);
    })
      .run(unit -> {
        new AssetHandlerWithCompiler("/", unit.get(AssetCompiler.class))
        .send(unit.get(Request.class), unit.get(Response.class), unit.get(Asset.class));
      });
  }
}
