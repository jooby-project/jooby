---
layout: index
title: faq
version: 0.7.0
---

# faq and facts

## build status [![Build Status](https://travis-ci.org/jooby-project/jooby.svg?branch=master)](https://travis-ci.org/jooby-project/jooby)

## code coverage [![Coverage Status](https://img.shields.io/coveralls/jooby-project/jooby.svg)](https://coveralls.io/r/jooby-project/jooby?branch=master)

Code coverage is composed by unit and integration tests. Integration tests can be found [here](https://github.com/jooby-project/jooby/tree/master/coverage-report)

Integration tests run again each of the available servers: [Netty](http://netty.io), [Jetty](http://www.eclipse.org/jetty/) and [Undertow](http://undertow.io)

## where are the default properties?

Default properties can be found [here](/doc/#appendix:-jooby.conf)

## what mime types are supported?

Full list of mime types can be found [here](/doc/#appendix:-mime.properties)

## how do I add a new/override mime type?

Create a ```mime.properties``` file inside the ```conf``` directory. Then, add the new mime type there.

## can I deploy my application inside a Servlet Container?

Yes, it is possible with [some limitations](/doc/jooby-servlet/).
