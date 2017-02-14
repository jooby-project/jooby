# elasticsearch

Open Source, Distributed, RESTful Search Engine via [Elastic Search](https://github.com/elastic/elasticsearch).

## exports

* ```RestClient```

## dependency

```xml
<dependency>
  <groupId>org.jooby</groupId>
  <artifactId>jooby-elasticsearch</artifactId>
  <version>1.0.3</version>
</dependency>
```

## usage

```java
{
  use(new Elasticsearch("localhost:9200"));
}
```

The constructor field is an array, so you can specify several hosts for round robin of requests.

## client API

The module exports a ```RestClient``` instance.

```java

put("/:id", req -> {
  // index a document
  RestClient client = require(RestClient.class);
  StringEntity data = new StringEntity("{\"foo\":\"bar\"}");
  return client.performRequest("PUT", "/twitter/tweet/" + req.param("id").value(), Collections.emptyMap(), data)
    .getEntity().getContent();
});
```
