package org.jooby.querydsl.internal;

import com.querydsl.sql.Configuration;
import com.querydsl.sql.H2Templates;
import com.querydsl.sql.SQLTemplates;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ConfigurationProviderTest {

  @Test
  public void get() throws Exception {
    SQLTemplates templates = new H2Templates();
    Configuration configuration = new ConfigurationProvider(templates).get();
    assertEquals(configuration.getTemplates(), templates);
  }
}