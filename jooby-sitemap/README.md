# sitemap

Generate <a href="https://en.wikipedia.org/wiki/Sitemaps">sitemap.xml</a> files using <a href="https://github.com/jirkapinkas/jsitemapgenerator">jsitemapgenerator</a>.

## exports

* A ```/sitemap.xml``` route

## dependency

```xml
<dependency>
 <groupId>org.jooby</groupId>
 <artifactId>jooby-sitemap</artifactId>
 <version>1.0.2</version>
</dependency>
```

## usage

```java
{
  use(new Sitemap());

  get("/page1", ..);

  get("/page2", ..);

}
```

The module exports a ```/sitemap.xml``` route.

## baseurl

The ```sitemap.xml``` specification requires an absolute url. The way we provide this absolute url is at creation time or using the ```sitemap.url``` property:

```java
{
  use(new Sitemap("https://foo.bar"));

  get("/page1", ..);

  get("/page2", ..);

}
```

or via application.conf:

```
sitemap.url = "http://foo.bar"
```

## customize

The sitemap generator builds a ```sitemap.xml``` file with ```loc``` elements. You can customize the output in one of two ways:

### declarative

```java
{
  get("/")

    .get("/page1", ..)
    .get("/page2", ..)
    .attr("changefreq", "weekly")
    .attr("priority", "1");
}
```

We first group route under a common path: ```/``` and add some routers. Then for each router we set the ```changefrequency``` and ```priority```.

### programmatically

```java
{
  use(new Sitemap().with(r -> {

    WebPage page = new WebPage();
    page.setName(r.pattern());
    page.setChangeFreq(ChangeFreq.ALWAYS);
    page.setPriority(1);
    return Arrays.asList(page);
  }));

  get("/")
    .get("/page1", ..)
    .get("/page2", ..);
}
```

Here we built ```WebPage``` objects and set frequency and priority.

## dynamic page generation

Suppose you have a **product** route dynamically mapped as:

```java
{
  get("/products/:sku", ...);

}
```

###  How do you generate urls for all your products? 

Dynamic urls are supported via custom [WebPageProvider](/apidocs/org/jooby/sitemap/WebPageProvider.html):

```java
{
  use(new Sitemap().with(SKUPageProvider.class));

  get("/products/:sku", ...);

}
```

SKUPageProvider.java: 

```java
import org.jooby.sitemap.WebPageProvider;

public class SKUPageProvider implements WebPageProvider {
  private MyDatabase db;

  @Inject

  public SKUPageProvider(MyDatabase db) {
    this.db = db;
  }

  public List<Webpage> apply(Route.Definition route) {
    if (route.pattern().startsWith("/products")) {
      // multiple urls
      return db.findSKUS().stream().map(sku -> {
          WebPage webpage = new WebPage();
          webpage.setName(route.reverse(sku));
          return webpage;
        }).collect(Collectors.toList());
    }
    // single url
    WebPage webpage = new WebPage();
    webpage.setName(route.pattern());
    return Arrays.asList(webpage);
  }
}
```

We test for ```/products``` url, ask our ```database``` to list all the ```SKUs``` and we build a ```WebPage``` for each of them.

## filter

The ```sitemap.filter``` option allows to skip/ignore routes from final output:

```java
{
  use(new Sitemap().filter(route -> !route.pattern().startsWith("/api")));
}
```

The default filter keeps ```GET``` routes.
