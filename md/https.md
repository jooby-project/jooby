# https

{{jooby}} supports ```HTTPS```. To enable this, you need to set:

```properties
application.securePort = 8443
```

## certificates

{{jooby}} comes with a self-signed certificate, useful for development and test. But of course, you
should NEVER use it in the real world.

In order to setup HTTPS with a secure certificate, you need to set these properties:

* ```ssl.keystore.cert```: An X.509 certificate chain file in PEM format. It can be an absolute path or a classpath resource.
* ```ssl.keystore.key```: A PKCS#8 private key file in PEM format. It can be an absolute path or a classpath resource.

Optionally, you can set these too:

* ```ssl.keystore.password```: Password of the keystore.key (if any). Default is: null/empty.
* ```ssl.trust.cert```: Trusted certificates for verifying the remote endpoint's certificate. The file should contain an X.509 certificate chain in PEM format. Default uses the system default.
* ```ssl.session.cacheSize```: Set the size of the cache used for storing SSL session objects. 0 to use the default value.
* ```ssl.session.timeout```: Timeout for the cached SSL session objects, in seconds. 0 to use the default value.

As you can see setup is very simple. All you need is your ```.crt``` and ```.key``` files.
