# jooby-hbv

Bean validation via [Hibernate Validator](hibernate.org/validator).

## exposes

* a ```Validator```,
* a ```HibernateValidatorConfiguration``` and
* a ```org.jooby.Parser```

## dependency

```xml
<dependency>
  <groupId>org.jooby</groupId>
  <artifactId>jooby-hbv</artifactId>
  <version>0.11.0</version>
</dependency>
```

## usage

```java
{
  use(new Hbv());

  get("/", req -> {
   Validator validator = req.require(Validator.class);
   Car car = req.params().to(Car.class);
   Set<ConstraintViolation> violations = validator.validate(car);
   if (violations.size() > 0) {
     // handle errors
     ...
   }
  });
}
```

## automatic validations of HTTP params and body

Previous example demonstrate how to manually validate a bean created via: ```req.params()``` or ```req.body()```. The boilerplate code
can be avoided if you explicitly tell the validation module which classes require validation.

The previous example can be rewritten as:

```java
{
  use(new Hbv(Car.class));

  get("/", () -> {
   Car car = req.params().to(Car.class);
   // a valid car is here
   ...
  });
}
```

Here a [Parser](/apidocs/org/jooby/Parser.html) will do the boilerplate part and throws a ```ConstraintViolationException```.

### rendering a ```ConstraintViolationException```

The default err handler will render the ```ConstraintViolationException``` without problem, but suppose we have a JavaScript client and want to display the errors in a friendly way.

```java
{
  use(new Jackson()); // JSON renderer

  use(new Hbv(Car.class)); // Validate Car objects

  err((req, rsp, err) -> {
    Throwable cause = err.getCause();
    if (cause instanceof ConstraintViolationException) {
      Set<ConstraintViolation<?>> constraints = ((ConstraintViolationException) cause)
            .getConstraintViolations();

      Map<Path, String> errors = constraints.stream()
          .collect(
              Collectors.toMap(
                  ConstraintViolation::getPropertyPath,
                  ConstraintViolation::getMessage
                  )
          );
      rsp.send(errors);
    }
  });

  get("/", () -> {
   Car car = req.params().to(Car.class);
   // a valid car is here
   ...
  });
}
```

The call to ```rsp.send(errors);``` will be rendered by ```Jackson``` (or any other that applies) and will produces a more friendly response, here it will be a JavaScript object with the errors.

## constraint validator factory

```ConstraintValidatorFactory``` is the extension point for customizing how constraint validators are instantiated and released.

In [Jooby](http://jooby.org), a ```ConstraintValidatorFactory``` is powered by [Guice](https://github.com/google/guice) and ```java.io.Closeable``` constraint will be release it. See [ConstraintValidatorFactory](http://docs.jboss.org/hibernate/validator/5.1/reference/en-US/html/chapter-bootstrapping.html#d0e4456)

## configuration

Any property defined at ```hibernate.validator``` will be add it automatically:

application.conf

```
hibernate.validator.fail_fast = true
```

Or programmatically:

```java
{
  use(new Hbv().doWith(config -> {
    config.failFast(true);
  }));
}
```
