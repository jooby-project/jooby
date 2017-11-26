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

[Kotlin](http://jooby.org/doc/lang-kotlin):

```java

import org.jooby.*

class App: Kooby({
  get {
    "Hello Jooby!"
  }
})

fun main(args: Array<String>) {
  run(::App, *args)
}

```
