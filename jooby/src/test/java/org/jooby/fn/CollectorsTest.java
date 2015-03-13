package org.jooby.fn;

import static org.junit.Assert.assertEquals;

import org.jooby.util.Collectors;
import org.junit.Test;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

public class CollectorsTest {

  @Test
  public void listAddOne() {
    assertEquals(Lists.newArrayList("1", Lists.newArrayList("1")),
        Lists.newArrayList("1", Lists.newArrayList("1")).stream().collect(Collectors.toList()));
  }

  @Test
  public void listAddTwo() {
    assertEquals(Lists.newArrayList("1", "2"),
        Lists.newArrayList("1", "2").stream().collect(Collectors.toList()));
  }

  @Test
  public void listAddMore() {
    assertEquals(Lists.newArrayList("1", "2", "3"),
        Lists.newArrayList("1", "2", "3").stream().collect(Collectors.toList()));

    assertEquals(Lists.newArrayList("1", "2", "3", "4"),
        Lists.newArrayList("1", "2", "3", "4").stream().collect(Collectors.toList()));
  }

  @Test
  public void setAddOne() {
    assertEquals(Sets.newHashSet("1"),
        Lists.newArrayList("1").stream().collect(Collectors.toSet()));
  }

  @Test
  public void setAddTwo() {
    assertEquals(Sets.newHashSet("1", "2"),
        Lists.newArrayList("1", "2").stream().collect(Collectors.toSet()));
  }

  @Test
  public void setAddMore() {
    assertEquals(Sets.newHashSet("1", "2", "3"),
        Lists.newArrayList("1", "2", "3").stream().collect(Collectors.toSet()));

    assertEquals(Sets.newHashSet("1", "2", "3", "4"),
        Lists.newArrayList("1", "2", "3", "4").stream().collect(Collectors.toSet()));
  }

}
