jooby(function (ConfigFactory, ConfigValueFactory, Jackson) {

  this.use(ConfigFactory.empty()
     .withValue('server.join', ConfigValueFactory.fromAnyRef(false))
     .withValue('application.port', ConfigValueFactory.fromAnyRef(3000)));

  this.use(new Jackson);

  this.use(org.jooby.js.Pets);

  this.get('/', function () 'Hey jooby!');

  this.use('/ns')
    .get('/:id', function (req) req.path())
    .get(function (req) req.path());

  this.post('/pets', '/pets/:id', function (req) req.path());

  this.get('/jsonobject', function () {
    return {
      'name': 'object'
    };
  });

  this.get('/jsonarray', function () {
    return [{
      'name': 'object'
    }];
  });

  this.get('/req', function (req, rsp) {
    rsp.send({
      'name': 'object'
    });
  });

  this.get('/reqstr', function (req, rsp) {
    rsp.send('stra');
  });

})(com.typesafe.config.ConfigFactory, com.typesafe.config.ConfigValueFactory, org.jooby.json.Jackson);
