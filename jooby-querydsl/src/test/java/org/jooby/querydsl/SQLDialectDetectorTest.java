package org.jooby.querydsl;

import com.querydsl.sql.*;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.Test;

import javax.sql.DataSource;

import static org.junit.Assert.*;


public class SQLDialectDetectorTest {

  @Test
  public void detectMySql() {
    SQLTemplates templates = SQLDialectDetector.detectByUrl("jdbc:mysql://localhost:3306");
    assertTrue(templates instanceof MySQLTemplates);
  }

  @Test
  public void detectPostgreSql() {
    SQLTemplates templates = SQLDialectDetector.detectByUrl("jdbc:postgres://localhost:5432");
    assertTrue(templates instanceof PostgreSQLTemplates);
  }

  @Test
  public void detectPgSql() {
    SQLTemplates templates = SQLDialectDetector.detectByUrl("jdbc:pgsql://localhost:5432");
    assertTrue(templates instanceof PostgreSQLTemplates);
  }

  @Test
  public void detectH2() {
    SQLTemplates templates = SQLDialectDetector.detectByUrl("jdbc:h2:mem:test");
    assertTrue(templates instanceof H2Templates);
  }

  @Test
  public void detectHSql() {
    SQLTemplates templates = SQLDialectDetector.detectByUrl("jdbc:hsqldb:hsql://localhost/test");
    assertTrue(templates instanceof HSQLDBTemplates);
  }

  @Test
  public void detectSqlite() {
    SQLTemplates templates = SQLDialectDetector.detectByUrl("jdbc:sqlite:some.db");
    assertTrue(templates instanceof SQLiteTemplates);
  }

  @Test
  public void detectDerby() {
    SQLTemplates templates = SQLDialectDetector.detectByUrl("jdbc:derby:some.db");
    assertTrue(templates instanceof DerbyTemplates);
  }

  @Test(expected = IllegalArgumentException.class)
  public void unknownDbShouldFail() {
    SQLDialectDetector.detectByUrl("jdbc:fail:so.hard");
  }

  @Test(expected = IllegalArgumentException.class)
  public void nonjdbcUrlShouldFail() {
    SQLDialectDetector.detectByUrl("fail:db:now");
  }

  @Test(expected = IllegalArgumentException.class)
  public void detectFromDataSourceShouldFailOnNull() {
    SQLDialectDetector.detectByUrl(null);
  }
}
