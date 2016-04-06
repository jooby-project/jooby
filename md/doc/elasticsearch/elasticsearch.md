# elasticsearch

Enterprise full text search via {{elasticsearch}}.

Provides a client/local API but also a RESTFul API.

## exports

* ```/search``` route
* ```Client```

## dependency

```xml
<dependency>
  <groupId>org.jooby</groupId>
  <artifactId>jooby-elasticsearch</artifactId>
  <version>{{version}}</version>
</dependency>
```

## usage

```java
{
  use(new ElasticSearch("/search"));
}
```

Elastic search will listen under the ```/search``` path. Here are some examples:

Creating a index:

```bash
 curl -XPUT 'localhost:8080/search/customer?pretty'
```

Indexing a doc:

```bash
 curl -XPUT 'localhost:8080/search/customer/external/1?pretty' -d '
{
 "name": "John Doe"
}'
```

Getting a doc:

```bash
 curl 'localhost:8080/search/customer/external/1?pretty'
```

## client API

The module exports a ```Client``` and ```Node``` instances, so it is possible to manage the index inside programmatically.

```java

post("/:id", req -> {
  // index a document
  Client client = req.require(Client.class);
  Map<String, Object>; json = new HashMap<>();
  json.put("user", "kimchy");
  json.put("message", "trying out Elasticsearch");

  return client.prepareIndex("twitter", "tweet", req.param("id").value())
    .setSource(json)
    .execute()
    .actionGet();
});

get("/:id", req -> {
  // get a doc
  Client client = req.require(Client.class);
  return client.prepareGet("twitter", "tweet", req.param("id").value())
    .execute()
    .actionGet()
    .getSource();
});

delete("/:id", req -> {
  // delete a doc
  Client client = req.require(Client.class);
  return client.prepareDelete("twitter", "tweet", req.param("id").value())
    .execute()
    .actionGet();
});
```

## configuration
If it possible to setup or configure {{elasticsearch}} via ```application.conf```, just make sure to prefix the property with ```elasticsearch```:

```properties
 elasticsearch.http.jsonp.enable = true
```

or programmatically:

```java
{
  use(new ElasticSearch().doWith(settings -> {
    settings.put(..., ...);
  });
}
```

### http.enabled
HTTP is disabled and isn't possible to change this value. What does it mean? {{jooby}} setup a custom
handler which makes it possible to use a {{jooby}} server to serve Elastic Search requests and avoid
the need of starting up another server running in a different port.

Most of the ```http.*``` properties has no sense in Jooby.

### path.data

Path data is set to a temporary directory: ```${application.tmpdir}/es/data```. It is
useful for development, but if you need want to make sure the index is persistent between server
restarts, please make sure to setup this path to something else.

### node.name
Node name is set to: ```application.name```.

### cluster.name
Cluster name is set to: ```${application.name}-cluster```.

That's all folks! Enjoy it!!!

{{appendix}}
