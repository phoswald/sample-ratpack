# sample-ratpack

Experiments with Ratpack, featuring:

- Non-blocking HTTP server
- Static web content
- REST endpoints

## Run Standalone

~~~
$ mvn clean verify
$ java -cp 'target/*:target/lib/*' \
  -Dapp.http.port=8080 \
  com.github.phoswald.sample.ratpack.Application
~~~

# URLs

- http://localhost:8080/

~~~
$ curl 'http://localhost:8080/now'
~~~
