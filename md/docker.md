# docker

Docker is the world’s leading software containerization platform. You can easily run you Jooby app as a docker container. You need to have the docker engine [installed](https://docs.docker.com/engine/installation/).

## usage

You need [docker](https://docs.docker.com/engine/installation/) installed on the building machine.

Add the following to the pom.xml under plugins:

```xml
<plugin>
    <groupId>com.spotify</groupId>
    <artifactId>docker-maven-plugin</artifactId>
    <version>0.4.13</version>
    <configuration>
        <imageName>my-jooby-image</imageName>
        <baseImage>openjdk:jre-alpine</baseImage>
        <entryPoint>["java", "-jar", "/${project.build.finalName}.jar"]</entryPoint>
        <exposes>
          <expose>8080</expose>
        </exposes>
        <resources>
            <resource>
                <targetPath>/</targetPath>
                <directory>${project.build.directory}</directory>
                <include>${project.build.finalName}.jar</include>
            </resource>
        </resources>
    </configuration>
</plugin>
 ```

In order to create the **docker image** go to your project home, open a terminal and run:

```bash
mvn clean docker:build
```

Once it finish, the docker image will be build and tagged as ```my-jooby-image```.

## run / start

You can now run the image with:

```bash
docker run -p 80:8080 my-jobby-image
```
