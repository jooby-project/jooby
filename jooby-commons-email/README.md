# commons-email

Email supports via [Apache Commons Email](https://commons.apache.org/proper/commons-email).

Small but helpful module that provides access to ```Email``` instances.

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

## commons-email.conf

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
