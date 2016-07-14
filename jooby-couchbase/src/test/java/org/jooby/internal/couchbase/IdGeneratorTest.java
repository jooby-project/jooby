package org.jooby.internal.couchbase;

import static org.junit.Assert.assertEquals;

import org.jooby.couchbase.GeneratedValue;
import org.junit.Test;

import com.couchbase.client.java.repository.annotation.Id;

public class IdGeneratorTest {

  public static class Base {
    String id;
  }

  public static class Sub extends Base {
  }

  @Test
  public void getId() {
    new IdGenerator();

    assertEquals("id1", IdGenerator.getId(new Object() {
      private String id = "id1";

      @Override
      public String toString() {
        return id;
      }
    }));

    assertEquals(1L, IdGenerator.getId(new Object() {
      @Id
      private Long beerId = 1L;
    }));
  }

  @Test
  public void getIdName() {
    assertEquals("id", IdGenerator.getIdName(new Object() {
      private String id = "id1";

      @Override
      public String toString() {
        return id;
      }
    }));

    assertEquals("beerId", IdGenerator.getIdName(new Object() {
      @Id
      private Long beerId = 1L;
    }));
  }

  @Test
  public void idFromSuper() {
    assertEquals("id", IdGenerator.getIdName(new Sub()));
  }

  @Test
  public void getOrGen() {
    assertEquals(null, IdGenerator.getOrGenId(new Object() {
      private String id;

      @Override
      public String toString() {
        return id;
      }
    }, () -> 7L));

    assertEquals("id1", IdGenerator.getOrGenId(new Object() {
      private String id = "id1";

      @Override
      public String toString() {
        return id;
      }
    }, () -> 7L));

    assertEquals(3L, IdGenerator.getOrGenId(new Object() {
      private Long id = 3L;

      @Override
      public String toString() {
        return id.toString();
      }
    }, () -> 7L));

    assertEquals(7L, IdGenerator.getOrGenId(new Object() {
      @GeneratedValue
      private Long id;

      @Override
      public String toString() {
        return id.toString();
      }
    }, () -> 7L));

  }

  @Test
  public void generatedValue() {
    Object entity = new Object() {
      @GeneratedValue
      private Long id;

      @Override
      public String toString() {
        return id.toString();
      }
    };
    assertEquals(null, IdGenerator.getId(entity));
    assertEquals(7L, IdGenerator.getOrGenId(entity, () -> 7L));
    assertEquals(7L, IdGenerator.getId(entity));
  }

  @Test(expected = IllegalArgumentException.class)
  public void generatedValueMustBeLong() {
    IdGenerator.getOrGenId(new Object() {
      @GeneratedValue
      private Integer id;

      @Override
      public String toString() {
        return id.toString();
      }
    }, () -> 7L);
  }

  @Test(expected = IllegalArgumentException.class)
  public void noId() {
    IdGenerator.getOrGenId(new Object(), () -> 7L);
  }

  @Test(expected = IllegalArgumentException.class)
  public void noId2() {
    IdGenerator.getOrGenId(new Object() {
      @SuppressWarnings("unused")
      String foo;
    }, () -> 7L);
  }

  @Test(expected = IllegalStateException.class)
  public void errorWhileGeneratingValue() {
    IdGenerator.getOrGenId(new Object() {
      @GeneratedValue
      private Long id;

      @Override
      public String toString() {
        return id.toString();
      }
    }, () -> {
      throw new IllegalStateException("intentional errr");
    });
  }

}
