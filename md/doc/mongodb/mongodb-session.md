# mongodb session store

## usage

```java
{
  use(new Mongodb());

  session(MongoSessionStore.class);

  get("/", req -> {
   req.session().set("name", "jooby");
  });
}
```

The ```name``` attribute and value will be stored in a {{mongodb}}.

## options

### timeout

By default, a mongodb session will expire after ```30 minutes```. Changing the default timeout is as simple as:

```properties
# 8 hours
session.timeout = 8h

# 15 seconds
session.timeout = 15

# 120 minutes
session.timeout = 120m

# no timeout
session.timeout = -1
```

It uses [MongoDB's TTL](docs.mongodb.org/manual/core/index-ttl) collection feature (2.2+) to have ```mongod``` automatically remove expired sessions.

### session collection

Default {{mongodb}} collection is ```sessions```.

It's possible to change the default key setting the ```mongodb.sesssion.collection``` properties.
