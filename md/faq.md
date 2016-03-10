# status

* <a target="_blank" href="https://travis-ci.org/jooby-project/jooby"><img src="https://travis-ci.org/jooby-project/jooby.svg?branch=master" alt="Build Status"></a>

* <a target="_blank" href="https://coveralls.io/r/jooby-project/jooby?branch=master"><img src="https://img.shields.io/coveralls/jooby-project/jooby.svg" alt="Coverage Status"></a>

* Code coverage is composed by unit and integration tests. Integration tests can be found <a target="_blank" href="https://github.com/jooby-project/jooby/tree/master/coverage-report">here</a>

* Integration tests run again each of the available servers: {{netty}}, {{jetty}} and {{undertow}}

# general

## what jvm do I need?

* Java 8 or higher
 
## where are the default properties?

* Default properties can be found [here](/doc/#appendix:-jooby.conf)

## how do I deploy my application?

* Deployment options can be found [here](/doc/deployment)

# mime types

## what mime types are supported?

* Full list of mime types can be found [here](/doc/#appendix:-mime.properties)

## how do I add a new/override mime type?

* Create a ```mime.properties``` file inside the ```conf``` directory. Then, add the new mime type there.
