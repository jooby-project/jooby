# commons-email

Email supports via {{commons-email}}.

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
  <version>{{version}}</version>
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
    req.require(SimpleEmail.class)
       .setMsg("you got an email!")
       .setTo("foo@bar.com")
       .send();
  });
}
```

That's all it does! Every time you require an email, it creates one and setup properties from ```mail.*```.

{{appendix}}
