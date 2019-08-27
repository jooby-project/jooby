[![Maven](https://img.shields.io/maven-metadata/v/http/central.maven.org/maven2/org/jooby/jooby-commons-email/maven-metadata.xml.svg)](http://mvnrepository.com/artifact/org.jooby/jooby-commons-email/1.6.4)
[![javadoc](https://javadoc.io/badge/org.jooby/jooby-commons-email.svg)](https://javadoc.io/doc/org.jooby/jooby-commons-email/1.6.4)
[![jooby-commons-email website](https://img.shields.io/badge/jooby-commons-email-brightgreen.svg)](http://jooby.org/doc/commons-email)
# commons-email

Email supports via [Apache Commons Email](https://commons.apache.org/proper/commons-email).

Small but helpful module that provides access to ```Email``` instances.

## exports

* ```SimpleEmail```
* ```MultiPartEmail```
* ```HtmlEmail```

## dependency

```xml
<dependency>
  <groupId>org.jooby</groupId>
  <artifactId>jooby-commons-email</artifactId>
  <version>1.6.4</version>
</dependency>
```

## usage

application.conf:

```properties
 mail.hostName = smtp.googlemail.com
 mail.ssl.onConnect = true
 mail.username = username
 mail.password = password
 mail.from = user@gmail.com
 mail.subject = TestMail
```

```java
{
  use(new CommonsEmail());

  get("/send", req -> {
    require(SimpleEmail.class)
       .setMsg("you got an email!")
       .setTo("foo@bar.com")
       .send();
  });
}
```

That's all it does! Every time you require an email, it creates one and setup properties from ```mail.*```.

## commons-email.conf
These are the default properties for commons-email:

```properties
mail {

  charset: ${application.charset}

  debug: false

  sendPartial: false

  smtpPort: 25

  starttls: false

  # SSL

  ssl.smtpPort: 465

  ssl.onConnect: false

  # advanced

  socketConnectionTimeout: 60s

  socketTimeout: 60s

}
```
