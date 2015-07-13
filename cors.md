# cors

Cross-origin resource sharing (CORS) is a mechanism that allows restricted resources
(e.g. fonts, JavaScript, etc.) on a web page to be requested from another domain outside the domain from which the resource originated.

## usage

```java
{

  cors();

}
```

Previous example, will handle CORS request (simple or preflight).

Default options are defined via ```.conf``` file:

```properties
cors {
  # Configures the Access-Control-Allow-Origin CORS header. Possibly values: *, domain, regex or a list of previous values.
  # Example:
  # "*"
  # ["http://foo.com"]
  # ["http://*.com"]
  # ["http://foo.com", "http://bar.com"]
  origin: "*"

  # If true, set the Access-Control-Allow-Credentials header
  credentials: true

  # Allowed methods: Set the Access-Control-Allow-Methods header
  allowedMethods: [GET, POST]

  # Allowed headers: set the Access-Control-Allow-Headers header. Possibly values: *, header name or a list of previous values.
  # Examples
  # "*"
  # Custom-Header
  # [Header-1, Header-2]
  allowedHeaders: [X-Requested-With, Content-Type, Accept, Origin]

  # Preflight max age: number of seconds that preflight requests can be cached by the client
  maxAge: 30m

  # Set the Access-Control-Expose-Headers header
  # exposedHeaders: []
}
```

```CORS``` options are represented at runtime by [Cors](/apidocs/org/jooby
/Cors.html).
