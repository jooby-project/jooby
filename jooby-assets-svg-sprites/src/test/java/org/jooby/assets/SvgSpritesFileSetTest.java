package org.jooby.assets;

import static org.junit.Assert.assertEquals;

import java.nio.file.Paths;

import org.junit.Test;

public class SvgSpritesFileSetTest {

  @Test(expected = IllegalArgumentException.class)
  public void requireSpriteElementPath() throws Exception {
    new SvgSprites()
        .set("spriteElementPath", "src/test/resources/svg-source")
        .fileset();
  }

  @Test
  public void cssPath() throws Exception {
    assertEquals(Paths.get("target", "css", "sprite.css").toString(),
        new SvgSprites()
            .set("spritePath", Paths.get("target", "css").toString())
            .cssPath());

    assertEquals(Paths.get("target", "css", "s.css").toString(),
        new SvgSprites()
            .set("spritePath", Paths.get("target", "css/s.svg").toString())
            .cssPath());
  }

  @Test
  public void spritePath() throws Exception {
    assertEquals(Paths.get("target", "css", "sprite.svg").toString(),
        new SvgSprites()
            .set("spritePath", Paths.get("target", "css").toString())
            .spritePath());

    assertEquals(Paths.get("target", "css", "s.svg").toString(),
        new SvgSprites()
            .set("spritePath", Paths.get("target", "css/s.svg").toString())
            .spritePath());
  }

  @Test
  public void spritePrefix() throws Exception {
    assertEquals(Paths.get("target", "css", "p-sprite.svg").toString(),
        new SvgSprites()
            .set("prefix", "p")
            .set("spritePath", Paths.get("target", "css").toString())
            .spritePath());

    assertEquals(Paths.get("target", "css", "sprite.svg").toString(),
        new SvgSprites()
            .set("prefix", "p")
            .set("spritePath", Paths.get("target", "css", "sprite.svg").toString())
            .spritePath());
  }

  @Test
  public void spriteName() throws Exception {
    assertEquals(Paths.get("target", "css", "p-n-sprite.svg").toString(),
        new SvgSprites()
            .set("prefix", "p")
            .set("name", "n")
            .set("spritePath", Paths.get("target", "css").toString())
            .spritePath());

    assertEquals(Paths.get("target", "css", "n-sprite.svg").toString(),
        new SvgSprites()
            .set("name", "n")
            .set("spritePath", Paths.get("target", "css").toString())
            .spritePath());
  }

  @Test
  public void cssPrefix() throws Exception {
    assertEquals(Paths.get("target", "css", "p-sprite.css").toString(),
        new SvgSprites()
            .set("prefix", "p")
            .set("spritePath", Paths.get("target", "css").toString())
            .cssPath());

    assertEquals(Paths.get("target", "css", "sprite.css").toString(),
        new SvgSprites()
            .set("prefix", "p")
            .set("spritePath", Paths.get("target", "css", "sprite.svg").toString())
            .cssPath());
  }

}
