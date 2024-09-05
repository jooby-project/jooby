/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.dbscheduler;

public class DbTable {
  public static String MariaDB =
      "-- Best-effort schema definition based on MySQL schema. Please suggest improvements.\n"
          + "create table scheduled_tasks (\n"
          + "  task_name varchar(100) not null,\n"
          + "  task_instance varchar(100) not null,\n"
          + "  task_data blob,\n"
          + "  execution_time timestamp(6) not null,\n"
          + "  picked BOOLEAN not null,\n"
          + "  picked_by varchar(50),\n"
          + "  last_success timestamp(6) null,\n"
          + "  last_failure timestamp(6) null,\n"
          + "  consecutive_failures INT,\n"
          + "  last_heartbeat timestamp(6) null,\n"
          + "  version BIGINT not null,\n"
          + "  PRIMARY KEY (task_name, task_instance),\n"
          + "  INDEX execution_time_idx (execution_time),\n"
          + "  INDEX last_heartbeat_idx (last_heartbeat)\n"
          + ")";
  public static String MSSQL =
      "create table scheduled_tasks\n"
          + "(\n"
          + "  task_name            varchar(250)   not null,\n"
          + "  task_instance        varchar(250)   not null,\n"
          + "  task_data            nvarchar(max),\n"
          + "  execution_time       datetimeoffset not null,\n"
          + "  picked               bit,\n"
          + "  picked_by            varchar(50),\n"
          + "  last_success         datetimeoffset,\n"
          + "  last_failure         datetimeoffset,\n"
          + "  consecutive_failures int,\n"
          + "  last_heartbeat       datetimeoffset,\n"
          + "  [version]            bigint         not null,\n"
          + "  primary key (task_name, task_instance),\n"
          + "  index execution_time_idx (execution_time),\n"
          + "  index last_heartbeat_idx (last_heartbeat)\n"
          + ")";

  public static String MY_SQL =
      "create table scheduled_tasks (\n"
          + "  task_name varchar(100) not null,\n"
          + "  task_instance varchar(100) not null,\n"
          + "  task_data blob,\n"
          + "  execution_time timestamp(6) not null,\n"
          + "  picked BOOLEAN not null,\n"
          + "  picked_by varchar(50),\n"
          + "  last_success timestamp(6) null,\n"
          + "  last_failure timestamp(6) null,\n"
          + "  consecutive_failures INT,\n"
          + "  last_heartbeat timestamp(6) null,\n"
          + "  version BIGINT not null,\n"
          + "  PRIMARY KEY (task_name, task_instance),\n"
          + "  INDEX execution_time_idx (execution_time),\n"
          + "  INDEX last_heartbeat_idx (last_heartbeat)\n"
          + ")";

  public static String ORACLE =
      "create table scheduled_tasks\n"
          + "(\n"
          + "    task_name            varchar(100),\n"
          + "    task_instance        varchar(100),\n"
          + "    task_data            blob,\n"
          + "    execution_time       TIMESTAMP(6) WITH TIME ZONE,\n"
          + "    picked               NUMBER(1, 0),\n"
          + "    picked_by            varchar(50),\n"
          + "    last_success         TIMESTAMP(6) WITH TIME ZONE,\n"
          + "    last_failure         TIMESTAMP(6) WITH TIME ZONE,\n"
          + "    consecutive_failures NUMBER(19, 0),\n"
          + "    last_heartbeat       TIMESTAMP(6) WITH TIME ZONE,\n"
          + "    version              NUMBER(19, 0),\n"
          + "    PRIMARY KEY (task_name, task_instance)\n"
          + ");\n"
          + "\n"
          + "CREATE INDEX scheduled_tasks__execution_time__idx on"
          + " scheduled_tasks(execution_time);\n"
          + "CREATE INDEX scheduled_tasks__last_heartbeat__idx on scheduled_tasks(last_heartbeat);";

  public static String POSTGRESQL =
      "create table scheduled_tasks (\n"
          + "  task_name text not null,\n"
          + "  task_instance text not null,\n"
          + "  task_data bytea,\n"
          + "  execution_time timestamp with time zone not null,\n"
          + "  picked BOOLEAN not null,\n"
          + "  picked_by text,\n"
          + "  last_success timestamp with time zone,\n"
          + "  last_failure timestamp with time zone,\n"
          + "  consecutive_failures INT,\n"
          + "  last_heartbeat timestamp with time zone,\n"
          + "  version BIGINT not null,\n"
          + "  PRIMARY KEY (task_name, task_instance)\n"
          + ");\n"
          + "\n"
          + "CREATE INDEX execution_time_idx ON scheduled_tasks (execution_time);\n"
          + "CREATE INDEX last_heartbeat_idx ON scheduled_tasks (last_heartbeat);";
}
