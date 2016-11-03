# elasticsearch

Full text search & analytics  via [Elasticsearch](https://github.com/elastic/elasticsearch).

Provides a RESTFul client.

## exports

* ```Client```

## dependency

```xml
<dependency>
  <groupId>org.jooby</groupId>
  <artifactId>jooby-elasticsearch</artifactId>
  <version>1.0.0.CR8</version>
</dependency>
```

## usage

```java
{
  use(new Elasticsearch());
}
```

You can specify a list of hosts to connect to, which are then connected via round-robin

```java
{
  use(new Elasticsearch("localhost:9200", "localhost:9201"));
}
```

## client API

The module exports a ```RestClient``` instance

```java

put("/:id", req -> {
  // index a document
  RestClient client = req.require(RestClient.class);
  StringEntity data = new StringEntity("{\"foo\":\"bar\"}");
  return client.performRequest("PUT", "/twitter/tweet/" + req.param("id").value(), Collections.emptyMap(), data)
    .getEntity().getContent();
});

```

