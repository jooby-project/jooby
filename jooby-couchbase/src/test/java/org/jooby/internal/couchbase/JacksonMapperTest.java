package org.jooby.internal.couchbase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import org.junit.Test;

import com.couchbase.client.java.document.EntityDocument;
import com.couchbase.client.java.document.JsonDocument;
import com.couchbase.client.java.document.json.JsonObject;
import com.couchbase.client.java.repository.annotation.Id;

public class JacksonMapperTest {

  public static class Bean {
    @Id
    public String foo;

    public Date date;

    public Bean(final String foo) {
      this.foo = foo;
    }

    public Bean() {
    }

    @Override
    public String toString() {
      return foo;
    }
  }

  @Test
  public void fromBytes() throws IOException {
    Double dateAsDouble = new Long(System.currentTimeMillis()).doubleValue();
    byte[] bytes = ("{\"_class\":\"" + Bean.class.getName() + "\", \"foo\":\"bar\", \"date\": "
        + dateAsDouble + "}")
            .getBytes(StandardCharsets.UTF_8);
    Bean bean = new JacksonMapper().fromBytes(bytes);
    assertNotNull(bean);
    assertEquals("bar", bean.toString());
    assertEquals(dateAsDouble.longValue(), bean.date.getTime());
  }

  @SuppressWarnings({"rawtypes", "unchecked" })
  @Test
  public void fromEntity() throws IOException {
    ("{\"_class\":\"" + Bean.class.getName() + "\"}")
        .getBytes(StandardCharsets.UTF_8);
    EntityDocument doc = EntityDocument.create("bar", new Bean("bar"));
    JsonDocument json = new JacksonMapper().fromEntity(doc);
    assertNotNull(json);
    assertEquals(Bean.class.getName(), json.content().getString("_class"));
    assertEquals("bar", json.content().getString("foo"));
  }

  @Test
  public void toEntity() throws IOException {
    JsonDocument doc = JsonDocument.create("bar",
        JsonObject.create().put("foo", "bar").put("_class", Bean.class.getName()));
    EntityDocument<Object> entity = new JacksonMapper().toEntity(doc, Object.class);
    Bean bean = (Bean) entity.content();
    assertNotNull(bean);
    assertEquals("bar", bean.toString());
  }
}
