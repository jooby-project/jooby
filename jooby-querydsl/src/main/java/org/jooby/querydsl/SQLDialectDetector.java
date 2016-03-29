/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.jooby.querydsl;

import com.querydsl.sql.*;

public interface SQLDialectDetector {

    static SQLTemplates detectByUrl(String url) {
        if (url == null) throw new IllegalArgumentException("URL is null");
        String[] components = url.split(":");
        if (components.length < 2 || !components[0].toLowerCase().equals("jdbc")) {
            throw new IllegalArgumentException(String.format("Invalid jdbc URL: %s", url));
        }
        return detectByDbTypeString(components[1].toLowerCase());
    }

    static SQLTemplates detectByDbTypeString(String dbType) {
        switch (dbType) {
            case "mysql":
                return new MySQLTemplates();
            case "h2":
                return new H2Templates();
            case "derby":
                return new DerbyTemplates();
            case "hsqldb":
                return new HSQLDBTemplates();
            case "postgres":
            case "pgsql":
                return new PostgreSQLTemplates();
            case "sqlite":
                return new SQLiteTemplates();
            default:
                throw new IllegalArgumentException("Could not determine database type!");
        }
    }
}
