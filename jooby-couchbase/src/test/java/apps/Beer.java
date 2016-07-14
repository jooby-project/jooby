package apps;

import java.util.Date;

import org.jooby.couchbase.GeneratedValue;

import com.couchbase.client.java.repository.annotation.Id;

public class Beer {

  @Id @GeneratedValue
  public Long beerId;

  public String name;

  public Date created = new Date();

  public transient String foo = "bar";

  public String getFoo() {
    return foo;
  }

  public void setFoo(final String foo) {
    this.foo = foo;
  }

}
