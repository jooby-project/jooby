package org.jooby.internal;

import static org.junit.Assert.assertEquals;

import org.jooby.Err;
import org.junit.Test;

import javaslang.Tuple2;

public class ByteRangeTest {

  @Test
  public void newInstance() {
    new ByteRange();
  }

  @Test(expected = Err.class)
  public void noByteRange() {
    ByteRange.parse("foo");
  }

  @Test(expected = Err.class)
  public void emptyRange() {
    ByteRange.parse("byte=");
  }

  @Test(expected = Err.class)
  public void invalidRange() {
    ByteRange.parse("bytes=-");
  }

  @Test(expected = Err.class)
  public void invalidRange2() {
    ByteRange.parse("bytes=z-");
  }

  @Test(expected = Err.class)
  public void invalidRange3() {
    ByteRange.parse("bytes=-z");
  }

  @Test(expected = Err.class)
  public void invalidRange4() {
    ByteRange.parse("bytes=6");
  }

  @Test
  public void validRange() {
    Tuple2<Long, Long> range = ByteRange.parse("bytes=1-10");
    assertEquals(1L, range._1.longValue());
    assertEquals(10L, range._2.longValue());
  }

  @Test
  public void prefixRange() {
    Tuple2<Long, Long> range = ByteRange.parse("bytes=99-");
    assertEquals(99L, range._1.longValue());
    assertEquals(-1L, range._2.longValue());
  }

  @Test
  public void suffixRange() {
    Tuple2<Long, Long> range = ByteRange.parse("bytes=-99");
    assertEquals(-1L, range._1.longValue());
    assertEquals(99L, range._2.longValue());
  }

}
