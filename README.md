# sample-ratpack

Experiments with Ratpack and Docker, featuring:

- Static web content
- Dynamic web content using Thymeleaf
- REST endpoints
- H2 database using JPA and Hibernate

## Run Standalone

~~~
$ mvn clean verify
$ export APP_HTTP_PORT=8080
$ export APP_SAMPLE_CONFIG=ValueFromShell
$ java -cp 'target/*:target/lib/*' com.github.phoswald.sample.ratpack.Application
~~~

## Run with Docker

~~~
$ mvn clean verify -P docker
$ docker run -it --name sample-ratpack --rm \
  -p 8080:8080 \
  -e APP_SAMPLE_CONFIG=ValueFromDockerRun \
  -v "$(pwd)/../databases":/usr/local/application/databases \
  sample-ratpack:0.1.0-SNAPSHOT
~~~

# URLs

- http://localhost:8080/

~~~
$ curl 'http://localhost:8080/rest/sample/time'
$ curl 'http://localhost:8080/rest/sample/config'
$ curl 'http://localhost:8080/rest/sample/echo' -i -X POST \
  -H 'content-type: text/xml' \
  -d '<EchoRequest><input>This is CURL</input></EchoRequest>'
$ curl 'http://localhost:8080/rest/sample/echo' -i -X POST \
  -H 'content-type: application/json' \
  -d '{"input":"This is CURL"}'
$ curl 'http://localhost:8080/rest/tasks' -i
$ curl 'http://localhost:8080/rest/tasks' -i -X POST \
  -H 'content-type: application/json' \
  -d '{"title":"Some task","description":"This is CURL","done":true}'
$ curl 'http://localhost:8080/rest/tasks/5b89f266-c566-4d1f-8545-451bc443cf26' -i
$ curl 'http://localhost:8080/rest/tasks/5b89f266-c566-4d1f-8545-451bc443cf26' -i -X PUT \
  -H 'content-type: application/json' \
  -d '{"title":"Some updated task","description":"This is still CURL","done":false}'
$ curl 'http://localhost:8080/rest/tasks/5b89f266-c566-4d1f-8545-451bc443cf26' -i -X DELETE
~~~

# Database setup

1.  Start H2 with `$ java -jar target/lib/h2-1.4.200.jar` and open UI in browser.
2.  Open database `jdbc:h2:./databases/task-db` with username `sa` password `sa`.
3.  Execute script `src/main/resources/schema.sql`. 

# TODOs

- XML instead of JSON
- Refactor HTTP stuff, redirect
- GraalVM
