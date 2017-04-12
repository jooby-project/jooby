# docker

Docker is the world’s leading software containerization platform. You can easily run you Jooby app as a docker container. You need to have the docker engine [installed](https://docs.docker.com/engine/installation/).

## usage

You need [docker](https://docs.docker.com/engine/installation/) installed on the building machine.

Maven users have two options: one for a building a [fat jar](/doc/deployment/#fat-jar) and one for building a [stork](/doc/deployment/#stork) distribution.

Gradle users might want to choose one of the available [plugins](https://plugins.gradle.org/search?term=docker).

### fat jar

* Write a `src/etc/docker.activator` file. File contents doesn't matter, the file presence activates a Maven profile.

* Open a terminal and run:

```bash
mvn clean package docker:build
```

* Once finished, the docker image has been built and tagged as `${project.artifactId}`.

* You can now run the image with:

```bash
docker run -p 80:8080 ${project.artifactId}
```

The Maven profile triggers the [spotify/docker-maven-plugin](https://github.com/spotify/docker-maven-plugin) which generates a `docker` file. Please see the [doc](https://github.com/spotify/docker-maven-plugin) for more details.

### stork

* Write a `src/etc/docker.stork.activator` file. File contents doesn't matter, the file presence activates a Maven profile.

* Open a terminal and run:

```bash
mvn clean package docker:build
```

* Once finished, the docker image has been built and tagged as `${project.artifactId}`.

* You can now run the image with:

```bash
docker run -it -p 80:8080 ${project.artifactId}
```

The Maven profile triggers the [spotify/docker-maven-plugin](https://github.com/spotify/docker-maven-plugin) which generates a `docker` file. Please see the [doc](https://github.com/spotify/docker-maven-plugin) for more details.
