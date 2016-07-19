package org.jooby.cassandra;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class ConnectionStringTest {

  @Test
  public void defaults() {
    ConnectionString cstr = ConnectionString.parse("cassandra://localhost/dba");
    assertEquals("cassandra://localhost:9042/dba", cstr.toString());
    assertArrayEquals(new String[]{"localhost" }, cstr.contactPoints());
    assertEquals("dba", cstr.keyspace());
    assertEquals(9042, cstr.port());
  }

  @Test
  public void multipleHosts() {
    ConnectionString cstr = ConnectionString.parse("cassandra://host1, host2/dba");
    assertEquals("cassandra://host1,host2:9042/dba", cstr.toString());
    assertArrayEquals(new String[]{"host1", "host2" }, cstr.contactPoints());
    assertEquals("dba", cstr.keyspace());
    assertEquals(9042, cstr.port());
  }

  @Test
  public void successCustomHostPort() {
    ConnectionString cstr = ConnectionString.parse("cassandra://127.0.0.1:6780/db");
    assertEquals("cassandra://127.0.0.1:6780/db", cstr.toString());
    assertArrayEquals(new String[]{"127.0.0.1" }, cstr.contactPoints());
    assertEquals("db", cstr.keyspace());
    assertEquals(6780, cstr.port());
  }

  @Test(expected = IllegalArgumentException.class)
  public void unknownSchema() {
    ConnectionString.parse("jdbc://localhost/dba");
  }

  @Test(expected = IllegalArgumentException.class)
  public void invalidUrl() {
    ConnectionString.parse("cassandra://localhost");
  }

  @Test(expected = IllegalArgumentException.class)
  public void multiplePorts() {
    ConnectionString.parse("cassandra://host1:9042,host2:9043/db");
  }

}
