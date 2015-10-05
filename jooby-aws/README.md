# aws module

Small utility module that exposes ```AmazonWebServiceClient``` and give you access to aws credentials (access and secret keys).

## exposes
* One ore more ```AmazonWebServiceClient```

## dependency

```xml
<dependency>
  <groupId>org.jooby</groupId>
  <artifactId>jooby-aws</artifactId>
  <version>0.11.1</version>
</dependency>
```

## usage

application.conf:

```properties
aws.accessKey = AKIAIOSFODNN7EXAMPLE
aws.secretKey =  wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY
```

```java
{
  use(new Aws()
    .with(creds -> new AmazonS3Client(creds))
    .with(creds -> new AmazonSimpleEmailServiceClient(creds))
  );

  get("/", req -> {
    AmazonS3 s3 = req.require(AmazonS3.class);
    // work with s3
  });
}
```

Keep in mind, you will need the ```s3 (ses, sqs,sns, etc..)``` jar in your classpath.

This module is small and simple. All it does is bind ```AmazonWebServiceClient``` instances in
[Guice](https://github.com/google/guice). It also helps to bind utility classes like ```TransferManager```

```java
{
  use(new Aws()
    .with(creds -> new AmazonS3Client(creds))
    .doWith((AmazonS3Client s3) -> new TransferManager(s3))
  );

  post("/", req -> {
    TransferMananger tm = require(TransferManager.class);
  });
}
```

## handling access and secret keys

Keys are defined in ```.conf``` file. It is possible to use global or per service keys.

```properties
 aws.accessKey = AKIAIOSFODNN7EXAMPLE
 aws.secretKey =  wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY

 aws.s3.accessKey = S3IOSFODNN7S3EXAMPLE
 aws.s3.secretKey = s3alrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY
```

```
{
  use(new Aws()
    .with(creds -> new AmazonS3Client(creds)) // use aws.s3 keys
    .with(creds -> new AmazonSimpleEmailServiceClient(creds)) // use global keys
  );
```

It uses the ```AmazonWebServiceClient#getServiceName()``` method in order to find per service
keys.

That's all folks!

Happy coding!!!
