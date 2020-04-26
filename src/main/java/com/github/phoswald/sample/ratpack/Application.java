package com.github.phoswald.sample.ratpack;

import static ratpack.jackson.Jackson.fromJson;
import static ratpack.jackson.Jackson.json;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

import org.apache.log4j.Logger;

import com.github.phoswald.sample.ratpack.sample.EchoRequest;
import com.github.phoswald.sample.ratpack.sample.SampleController;
import com.github.phoswald.sample.ratpack.sample.SampleResource;
import com.github.phoswald.sample.ratpack.task.TaskController;
import com.github.phoswald.sample.ratpack.task.TaskEntity;
import com.github.phoswald.sample.ratpack.task.TaskResource;

import ratpack.form.Form;
import ratpack.handling.Context;
import ratpack.handling.Handler;
import ratpack.server.RatpackServer;

public class Application {

    private static final Logger logger = Logger.getLogger(Application.class);

    public static void main(String[] args) throws Exception {
        int port = Integer.parseInt(System.getProperty("app.http.port", "8080"));
        logger.info("sample-ratpack is starting, port=" + port);
        RatpackServer.start(server -> server
                .serverConfig(c -> c.port(port))
                .handlers(chain -> chain
                        .get("", ctx -> printResource(ctx, "/resources/index.html", "text/html"))
                        .get("rest/sample/time", createHandler(() -> new SampleResource().getTime()))
                        .get("rest/sample/config", createHandler(() -> new SampleResource().getConfig()))
                        .post("rest/sample/echo", createJsonHandler(EchoRequest.class, req -> new SampleResource().postEcho(req)))

                        .get("rest/pages/sample", createHtmlHandler(() -> new SampleController().getSamplePage()))

                        .path("rest/tasks", ctx2 -> ctx2.byMethod(chain2 -> chain2
                                .get(createJsonHandler(() -> new TaskResource().getTasks()))
                                .post(createJsonHandler(TaskEntity.class, req -> new TaskResource().postTasks(req)))
                        ))
                        .path("rest/tasks/:id", ctx2 -> ctx2.byMethod(chain2 -> chain2
                                .get(createJsonHandlerEx(ctx -> new TaskResource().getTask(ctx.getPathTokens().get("id"))))
                                .put(createJsonHandlerEx(TaskEntity.class, (ctx, req) -> new TaskResource().putTask(ctx.getPathTokens().get("id"), req)))
                                .delete(createJsonHandlerEx(ctx -> new TaskResource().deleteTask(ctx.getPathTokens().get("id"))))
                        ))

                        .path("rest/pages/tasks", ctx2 -> ctx2.byMethod(chain2 -> chain2
                                .get(createHtmlHandler(() -> new TaskController().getTasksPage()))
                                .post(createHtmlFormHandler(form -> new TaskController().postTasksPage(form.get("title"), form.get("description"))))
                         ))
                        .path("rest/pages/tasks/:id", ctx2 -> ctx2.byMethod(chain2 -> chain2
                                .get(createHtmlHandlerEx(ctx -> new TaskController().getTaskPage(ctx.getPathTokens().get("id"), ctx.getRequest().getQueryParams().get("action"))))
                                .post(createHtmlFormHandlerEx((ctx, form) -> new TaskController().postTaskPage(ctx.getPathTokens().get("id"), form.get("action"), form.get("title"), form.get("description"), form.get("done"))))
                        ))
                )
        );
    }

    private static Handler createHandler(Supplier<Object> response) {
        return ctx -> ctx.render(response.get());
    }

    private static <R> Handler createJsonHandler(Supplier<Object> response) {
        return ctx -> ctx.render(json(response.get()));
    }

    private static <R> Handler createJsonHandlerEx(Function<Context, Object> response) {
        return ctx -> ctx.render(json(response.apply(ctx)));
    }

    private static <R> Handler createJsonHandler(Class<R> requestClass, Function<R, Object> response) {
        return ctx -> ctx.parse(fromJson(requestClass))
                .then(request -> ctx.render(json(response.apply(request))));
    }

    private static <R> Handler createJsonHandlerEx(Class<R> requestClass, BiFunction<Context, R, Object> response) {
        return ctx -> ctx.parse(fromJson(requestClass))
                .then(request -> ctx.render(json(response.apply(ctx, request))));
    }

    private static Handler createHtmlHandler(Supplier<Object> response) {
        return ctx -> ctx.header("content-type", "text/html").render(response.get());
    }

    private static Handler createHtmlHandlerEx(Function<Context, Object> response) {
        return ctx -> ctx.header("content-type", "text/html").render(response.apply(ctx));
    }

    private static Handler createHtmlFormHandler(Function<Form, Object> response) {
        return ctx -> ctx.parse(Form.class)
                .then(form -> ctx.header("content-type", "text/html").render(response.apply(form)));
    }

    private static Handler createHtmlFormHandlerEx(BiFunction<Context, Form, Object> response) {
        return ctx -> ctx.parse(Form.class)
                .then(form -> {
                    Object result = response.apply(ctx, form);
                    if(result instanceof String && ((String) result).startsWith("REDIRECT:")) { // TODO refactor redirect
                        ctx.redirect(((String) result).substring(9));
                    } else {
                        ctx.header("content-type", "text/html").render(result);
                    }
                });
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
