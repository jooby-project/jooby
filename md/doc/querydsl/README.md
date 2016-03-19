# querydsl jpa

[Querydsl](http://www.querydsl.com/) unified Queries for Java. Querydsl is compact, safe and easy to learn.

This module provides [JPA](http://www.querydsl.com/static/querydsl/4.0.9/reference/html_single/#jpa_integration) integration.

## usage

* Write a ```querydsl-jpa.activator``` file inside the ```src/etc/``` directory.

* Open a terminal and type: ```mvn clean compile```

* Generated classes will be placed inside the ```target/generated-sources``` directory.

Of course, you need to define some entities and have **JPA** in your classpath. The [hibernate](/doc/hbm) module gives you JPA support.

## profile activation

Just write a ```src/etc/querydsl-jpa.activator``` file. File contents doesn't matter just file presence.

The file ```src/etc/querydsl-jpa.activator``` trigger a maven profile that does [all this](http://www.querydsl.com/static/querydsl/4.0.9/reference/html_single/#jpa_integration) for you.
