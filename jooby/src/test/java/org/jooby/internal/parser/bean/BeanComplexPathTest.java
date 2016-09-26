package org.jooby.internal.parser.bean;

import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;

import java.lang.reflect.Type;
import java.util.Arrays;

import org.jooby.test.MockUnit;
import org.junit.Test;

public class BeanComplexPathTest {

  @Test
  public void complexPath() throws Exception {
    new MockUnit(BeanPath.class, Type.class)
        .expect(unit -> {
          expect(unit.get(BeanPath.class).type()).andReturn(unit.get(Type.class));
        })
        .run(unit -> {
          BeanComplexPath path = new BeanComplexPath(Arrays.asList(), unit.get(BeanPath.class),
              "path");
          assertEquals(unit.get(Type.class), path.type());
        });
  }
}
