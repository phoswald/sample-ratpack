package com.github.phoswald.sample.ratpack;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.ZonedDateTime;

import org.apache.log4j.Logger;

import ratpack.handling.Context;
import ratpack.server.RatpackServer;

public class Application {

    private static final Logger logger = Logger.getLogger(Application.class);

    public static void main(String[] args) throws Exception {
        logger.info("sample-ratpack is starting");
        RatpackServer.start(server -> server
                .serverConfig(c -> c.port(8080))
                .handlers(chain -> chain
                        .get(ctx -> printResource(ctx, "/index.html", "text/html"))
                        .get("now", ctx -> ctx.render(ZonedDateTime.now() + "\n"))));
    }


    private static void printResource(Context ctx, String name, String contentType) {
       try(InputStream stm = Application.class.getResourceAsStream(name)) {
          ByteArrayOutputStream bytes = new ByteArrayOutputStream();
          int current;
          while((current = stm.read()) != -1) {
             bytes.write(current);
          }
          ctx.getResponse().send(contentType, bytes.toByteArray());
       } catch (IOException e) {
          logger.error("Unexpected trouble", e);
          ctx.getResponse().status(500);
          ctx.render("");
       }
    }
}

