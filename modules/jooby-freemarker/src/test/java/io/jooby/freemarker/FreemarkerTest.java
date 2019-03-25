package io.jooby.freemarker;

import io.jooby.AttributeKey;
import io.jooby.Env;
import io.jooby.MockContext;
import io.jooby.ModelAndView;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class FreemarkerTest {

  public static class User {
    private String firstname;

    private String lastname;

    public User(String firstname, String lastname) {
      this.firstname = firstname;
      this.lastname = lastname;
    }

    public String getFirstname() {
      return firstname;
    }

    public String getLastname() {
      return lastname;
    }
  }

  @Test
  public void render() throws Exception {
    Freemarker freemarker = Freemarker.builder().build(Env.empty("test"));
    AttributeKey<String> local = new AttributeKey<>(String.class, "local");
    MockContext ctx = new MockContext();
    ctx.getAttributes().put(local, "var");
    String output = freemarker
        .apply(ctx, new ModelAndView("index.ftl")
            .put("user", new User("foo", "bar"))
            .put("sign", "!"));
    assertEquals("Hello foo bar var!\n", output);
  }
}
