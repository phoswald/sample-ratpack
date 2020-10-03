package com.github.phoswald.sample.ratpack;

import static ratpack.jackson.Jackson.fromJson;
import static ratpack.jackson.Jackson.json;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

import org.apache.log4j.Logger;

import com.github.phoswald.sample.sample.EchoRequest;
import com.github.phoswald.sample.sample.SampleController;
import com.github.phoswald.sample.sample.SampleResource;
import com.github.phoswald.sample.task.TaskController;
import com.github.phoswald.sample.task.TaskEntity;
import com.github.phoswald.sample.task.TaskRepository;
import com.github.phoswald.sample.task.TaskResource;

import ratpack.form.Form;
import ratpack.func.Action;
import ratpack.handling.Chain;
import ratpack.handling.Context;
import ratpack.handling.Handler;
import ratpack.server.BaseDir;
import ratpack.server.RatpackServer;
import ratpack.server.ServerConfigBuilder;

public class Application {

    private static final Logger logger = Logger.getLogger(Application.class);
    private static final int port = Integer.parseInt(Optional.ofNullable(System.getenv("APP_HTTP_PORT")).orElse("8080"));

    public static void main(String[] args) throws Exception {
        logger.info("sample-ratpack is starting, port=" + port);
        RatpackServer.start(server -> server
                .serverConfig(createConfig())
                .handlers(createRoutes()));
    }

    private static Action<? super ServerConfigBuilder> createConfig() {
        return config -> config
                .port(port)
                .baseDir(BaseDir.find("resources/.ratpack").toAbsolutePath());
    }

    private static Action<? super Chain> createRoutes() {
        return chain -> chain
                .files(config -> config.indexFiles("index.html"))
                .get("rest/sample/time", createHandler(() -> new SampleResource().getTime()))
                .get("rest/sample/config", createHandler(() -> new SampleResource().getConfig()))
                .post("rest/sample/echo-json", createJsonHandler(EchoRequest.class, reqBody -> new SampleResource().postEcho(reqBody)))
                .get("rest/pages/sample", createHtmlHandler(() -> new SampleController().getSamplePage()))
                .path("rest/tasks", ctx2 -> ctx2.byMethod(chain2 -> chain2
                        .get(createJsonHandler(() -> createTaskResource().getTasks()))
                        .post(createJsonHandler(TaskEntity.class, reqBody -> createTaskResource().postTasks(reqBody)))
                ))
                .path("rest/tasks/:id", ctx2 -> ctx2.byMethod(chain2 -> chain2
                        .get(createJsonHandlerEx(ctx -> createTaskResource().getTask(ctx.getPathTokens().get("id"))))
                        .put(createJsonHandlerEx(TaskEntity.class, (ctx, reqBody) -> createTaskResource().putTask(ctx.getPathTokens().get("id"), reqBody)))
                        .delete(createJsonHandlerEx(ctx -> createTaskResource().deleteTask(ctx.getPathTokens().get("id"))))
                ))
                .path("rest/pages/tasks", ctx2 -> ctx2.byMethod(chain2 -> chain2
                        .get(createHtmlHandler(() -> createTaskController().getTasksPage()))
                        .post(createHtmlFormHandler(form -> createTaskController().postTasksPage(form.get("title"), form.get("description"))))
                 ))
                .path("rest/pages/tasks/:id", ctx2 -> ctx2.byMethod(chain2 -> chain2
                        .get(createHtmlHandlerEx(ctx -> createTaskController().getTaskPage(ctx.getPathTokens().get("id"), ctx.getRequest().getQueryParams().get("action"))))
                        .post(createHtmlFormHandlerEx((ctx, form) -> createTaskController().postTaskPage(ctx.getPathTokens().get("id"), form.get("action"), form.get("title"), form.get("description"), form.get("done"))))
                ));
    }

    // TODO support XML, not only JSON

    private static Handler createHandler(Supplier<Object> callback) {
        return ctx -> ctx.render(callback.get());
    }

    private static <R> Handler createJsonHandler(Supplier<Object> callback) {
        return ctx -> ctx.render(json(callback.get()));
    }

    private static <R> Handler createJsonHandlerEx(Function<Context, Object> callback) {
        return ctx -> ctx.render(json(callback.apply(ctx)));
    }

    private static <R> Handler createJsonHandler(Class<R> reqClass, Function<R, Object> callback) {
        return ctx -> ctx.parse(fromJson(reqClass)).then(reqBody -> ctx.render(json(callback.apply(reqBody))));
    }

    private static <R> Handler createJsonHandlerEx(Class<R> reqClass, BiFunction<Context, R, Object> callback) {
        return ctx -> ctx.parse(fromJson(reqClass)).then(reqBody -> ctx.render(json(callback.apply(ctx, reqBody))));
    }

    private static Handler createHtmlHandler(Supplier<Object> callback) {
        return ctx -> ctx.header("content-type", "text/html").render(callback.get());
    }

    private static Handler createHtmlHandlerEx(Function<Context, Object> callback) {
        return ctx -> ctx.header("content-type", "text/html").render(callback.apply(ctx));
    }

    private static Handler createHtmlFormHandler(Function<Form, Object> callback) {
        return ctx -> ctx.parse(Form.class).then(form -> ctx.header("content-type", "text/html").render(callback.apply(form)));
    }

    private static Handler createHtmlFormHandlerEx(BiFunction<Context, Form, Object> callback) {
        return ctx -> ctx.parse(Form.class).then(form -> sendHtmlOrRedirect(ctx, callback.apply(ctx, form)));
    }

    private static void sendHtmlOrRedirect(Context ctx, Object result) {
        if(result instanceof String && ((String) result).startsWith("REDIRECT:")) { // TODO refactor redirect
            ctx.redirect(((String) result).substring(9));
        } else {
            ctx.header("content-type", "text/html").render(result);
        }
    }

    private static TaskResource createTaskResource()  {
        return new TaskResource(createTaskRepository());
    }

    private static TaskController createTaskController() {
        return new TaskController(createTaskRepository());
    }

    private static TaskRepository createTaskRepository() {
        try {
            Connection conn = DriverManager.getConnection("jdbc:h2:./databases/task-db", "sa", "sa");
            return new TaskRepository(conn);
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }
}
