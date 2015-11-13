package org.jooby.internal;

import static org.junit.Assert.assertEquals;

import java.util.Calendar;
import java.util.Date;

import org.junit.Test;

public class HeadersTest {

  @Test
  public void sillyJacoco() {
    new Headers();
  }

  @Test
  public void encodeString() {
    assertEquals("x1", Headers.encode("x1"));
  }

  @Test
  public void encodeNumber() {
    assertEquals("12", Headers.encode(12));
  }

  @Test
  public void date() {
    assertEquals("Fri, 10 Apr 2015 23:31:25 GMT", Headers.encode(new Date(1428708685066L)));
  }

  @Test
  public void calendar() {
    Calendar calendar = Calendar.getInstance();
    calendar.setTimeInMillis(1428708685066L);
    assertEquals("Fri, 10 Apr 2015 23:31:25 GMT", Headers.encode(calendar));
  }

}
