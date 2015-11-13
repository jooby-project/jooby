## hello world!

Java:

```java
import org.jooby.Jooby;

public class App extends Jooby {

  {
    get("/", () -> "Hey Jooby!");
  }

  public static void main(final String[] args) throws Exception {
    new App().start(args);
  }
}

```

[JavaScript](/jooby-js):

```js

var app = jooby();

app.get('/', function () 'Hey Jooby!');

```
