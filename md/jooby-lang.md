## hello world!

Java:

```java
import org.jooby.Jooby;

public class App extends Jooby {

  {
    get("/", () -> "Hey Jooby!");
  }

  public static void main(final String[] args) {
    run(App::new, args);
  }
}

```

[Kotlin](/jooby-lang-kotlin):

```kotlin

import org.jooby.*

fun main(args: Array<String>) {
  run(*args) {
    get {
      "Hello Kotlin"
    }
  }
}

```

[JavaScript](/jooby-lang-js):

```js

var app = jooby();

app.get('/', function () 'Hey Jooby!');

```
