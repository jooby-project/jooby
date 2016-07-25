package org.jooby.internal;

import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import org.jooby.test.MockUnit;
import org.junit.Test;

import com.google.common.collect.ImmutableList;

public class ParamReferenceImplTest {

  @Test
  public void defaults() throws Exception {
    new MockUnit()
        .run(unit -> {
          new StrParamReferenceImpl("parameter", "name", Collections.emptyList());
        });
  }

  @Test
  public void first() throws Exception {
    new MockUnit()
        .run(unit -> {
          assertEquals("first",
              new StrParamReferenceImpl("parameter", "name", ImmutableList.of("first")).first());
        });
  }

  @Test
  public void last() throws Exception {
    new MockUnit()
        .run(unit -> {
          assertEquals("last",
              new StrParamReferenceImpl("parameter", "name", ImmutableList.of("last")).last());
        });
  }

  @Test
  public void get() throws Exception {
    new MockUnit()
        .run(unit -> {
          assertEquals("0",
              new StrParamReferenceImpl("parameter", "name", ImmutableList.of("0")).get(0));
          assertEquals("1",
              new StrParamReferenceImpl("parameter", "name", ImmutableList.of("0", "1")).get(1));
        });
  }

  @Test(expected = NoSuchElementException.class)
  public void missing() throws Exception {
    new MockUnit()
        .run(unit -> {
          new StrParamReferenceImpl("parameter", "name", ImmutableList.of("0")).get(1);
        });
  }

  @Test(expected = NoSuchElementException.class)
  public void missingLowIndex() throws Exception {
    new MockUnit()
        .run(unit -> {
          new StrParamReferenceImpl("parameter", "name", ImmutableList.of("0")).get(-1);
        });
  }

  @Test
  public void size() throws Exception {
    new MockUnit()
        .run(unit -> {
          assertEquals(1,
              new StrParamReferenceImpl("parameter", "name", ImmutableList.of("0")).size());
          assertEquals(2,
              new StrParamReferenceImpl("parameter", "name", ImmutableList.of("0", "1")).size());
        });
  }

  @SuppressWarnings({"rawtypes", "unchecked" })
  @Test
  public void iterator() throws Exception {
    new MockUnit(List.class, Iterator.class)
        .expect(unit -> {
          List list = unit.get(List.class);
          expect(list.iterator()).andReturn(unit.get(Iterator.class));
        })
        .run(unit -> {
          assertEquals(unit.get(Iterator.class),
              new StrParamReferenceImpl("parameter", "name", unit.get(List.class)).iterator());
        });
  }

}
