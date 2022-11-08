package com.github.phoswald.sample;

import static ratpack.jackson.Jackson.fromJson;
import static ratpack.jackson.Jackson.json;

import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.github.phoswald.sample.sample.EchoRequest;
import com.github.phoswald.sample.sample.SampleController;
import com.github.phoswald.sample.sample.SampleResource;
import com.github.phoswald.sample.task.TaskController;
import com.github.phoswald.sample.task.TaskEntity;
import com.github.phoswald.sample.task.TaskResource;
import com.github.phoswald.sample.utils.ConfigProvider;

import ratpack.form.Form;
import ratpack.func.Action;
import ratpack.handling.Chain;
import ratpack.handling.Context;
import ratpack.handling.Handler;
import ratpack.server.BaseDir;
import ratpack.server.RatpackServer;
import ratpack.server.ServerConfigBuilder;

public class Application {

    private static final Logger logger = LogManager.getLogger();

    private final int port;
    private final SampleResource sampleResource;
    private final SampleController sampleController;
    private final TaskResource taskResource;
    private final TaskController taskController;

    private RatpackServer server;

    public Application( //
            ConfigProvider config, //
            SampleResource sampleResource, //
            SampleController sampleController, //
            TaskResource taskResource, //
            TaskController taskController) {
        this.port = Integer.parseInt(config.getConfigProperty("app.http.port").orElse("8080"));
        this.sampleResource = sampleResource;
        this.sampleController = sampleController;
        this.taskResource = taskResource;
        this.taskController = taskController;
    }

    public static void main(String[] args) throws Exception {
        var module = new ApplicationModule() { };
        module.getApplication().start();
    }

    void start() throws Exception {
        logger.info("sample-ratpack is starting, port=" + port);
        server = RatpackServer.start(server -> server
                .serverConfig(createConfig())
                .handlers(createRoutes()));
    }

    void stop() throws Exception {
        server.stop();
    }

    private Action<? super ServerConfigBuilder> createConfig() {
        return config -> config
                .port(port)
                .baseDir(BaseDir.find("resources/.ratpack").toAbsolutePath());
    }

    private Action<? super Chain> createRoutes() {
        return chain -> chain
                .files(config -> config.indexFiles("index.html"))
                .get("app/rest/sample/time", createHandler(() -> sampleResource.getTime()))
                .get("app/rest/sample/config", createHandler(() -> sampleResource.getConfig()))
                .post("app/rest/sample/echo-json", createJsonHandler(EchoRequest.class, reqBody -> sampleResource.postEcho(reqBody)))
                .get("app/pages/sample", createHtmlHandler(() -> sampleController.getSamplePage()))
                .path("app/rest/tasks", ctx2 -> ctx2.byMethod(chain2 -> chain2
                        .get(createJsonHandler(() -> taskResource.getTasks()))
                        .post(createJsonHandler(TaskEntity.class, reqBody -> taskResource.postTasks(reqBody)))
                ))
                .path("app/rest/tasks/:id", ctx2 -> ctx2.byMethod(chain2 -> chain2
                        .get(createJsonHandlerEx(ctx -> taskResource.getTask(ctx.getPathTokens().get("id"))))
                        .put(createJsonHandlerEx(TaskEntity.class, (ctx, reqBody) -> taskResource.putTask(ctx.getPathTokens().get("id"), reqBody)))
                        .delete(createJsonHandlerEx(ctx -> taskResource.deleteTask(ctx.getPathTokens().get("id"))))
                ))
                .path("app/pages/tasks", ctx2 -> ctx2.byMethod(chain2 -> chain2
                        .get(createHtmlHandler(() -> taskController.getTasksPage()))
                        .post(createHtmlFormHandler(form -> taskController.postTasksPage(form.get("title"), form.get("description"))))
                 ))
                .path("app/pages/tasks/:id", ctx2 -> ctx2.byMethod(chain2 -> chain2
                        .get(createHtmlHandlerEx(ctx -> taskController.getTaskPage(ctx.getPathTokens().get("id"), ctx.getRequest().getQueryParams().get("action"))))
                        .post(createHtmlFormHandlerEx((ctx, form) -> taskController.postTaskPage(ctx.getPathTokens().get("id"), form.get("action"), form.get("title"), form.get("description"), form.get("done"))))
                ));
    }

    // TODO support XML, not only JSON

    private Handler createHandler(Supplier<Object> callback) {
        return ctx -> ctx.render(callback.get());
    }

    private <R> Handler createJsonHandler(Supplier<Object> callback) {
        return ctx -> ctx.render(json(callback.get()));
    }

    private <R> Handler createJsonHandlerEx(Function<Context, Object> callback) {
        return ctx -> ctx.render(json(callback.apply(ctx)));
    }

    private <R> Handler createJsonHandler(Class<R> reqClass, Function<R, Object> callback) {
        return ctx -> ctx.parse(fromJson(reqClass)).then(reqBody -> ctx.render(json(callback.apply(reqBody))));
    }

    private <R> Handler createJsonHandlerEx(Class<R> reqClass, BiFunction<Context, R, Object> callback) {
        return ctx -> ctx.parse(fromJson(reqClass)).then(reqBody -> ctx.render(json(callback.apply(ctx, reqBody))));
    }

    private Handler createHtmlHandler(Supplier<Object> callback) {
        return ctx -> ctx.header("content-type", "text/html").render(callback.get());
    }

    private Handler createHtmlHandlerEx(Function<Context, Object> callback) {
        return ctx -> ctx.header("content-type", "text/html").render(callback.apply(ctx));
    }

    private Handler createHtmlFormHandler(Function<Form, Object> callback) {
        return ctx -> ctx.parse(Form.class).then(form -> ctx.header("content-type", "text/html").render(callback.apply(form)));
    }

    private Handler createHtmlFormHandlerEx(BiFunction<Context, Form, Object> callback) {
        return ctx -> ctx.parse(Form.class).then(form -> sendHtmlOrRedirect(ctx, callback.apply(ctx, form)));
    }

    private void sendHtmlOrRedirect(Context ctx, Object result) {
        if(result instanceof String && ((String) result).startsWith("REDIRECT:")) { // TODO refactor redirect
            ctx.redirect(((String) result).substring(9));
        } else {
            ctx.header("content-type", "text/html").render(result);
        }
    }
}
