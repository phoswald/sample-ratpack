package com.github.phoswald.sample.ratpack;

import static ratpack.jackson.Jackson.fromJson;
import static ratpack.jackson.Jackson.json;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.github.phoswald.sample.ConfigProvider;
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

    private static final Logger logger = LogManager.getLogger();
    private static final ConfigProvider config = new ConfigProvider();
    private static final int port = Integer.parseInt(config.getConfigProperty("app.http.port").orElse("8080"));
    private static final EntityManagerFactory emf = createEntityManagerFactory();

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
                .get("app/rest/sample/time", createHandler(() -> new SampleResource().getTime()))
                .get("app/rest/sample/config", createHandler(() -> new SampleResource().getConfig()))
                .post("app/rest/sample/echo-json", createJsonHandler(EchoRequest.class, reqBody -> new SampleResource().postEcho(reqBody)))
                .get("app/pages/sample", createHtmlHandler(() -> new SampleController().getSamplePage()))
                .path("app/rest/tasks", ctx2 -> ctx2.byMethod(chain2 -> chain2
                        .get(createJsonHandler(() -> createTaskResource().getTasks()))
                        .post(createJsonHandler(TaskEntity.class, reqBody -> createTaskResource().postTasks(reqBody)))
                ))
                .path("app/rest/tasks/:id", ctx2 -> ctx2.byMethod(chain2 -> chain2
                        .get(createJsonHandlerEx(ctx -> createTaskResource().getTask(ctx.getPathTokens().get("id"))))
                        .put(createJsonHandlerEx(TaskEntity.class, (ctx, reqBody) -> createTaskResource().putTask(ctx.getPathTokens().get("id"), reqBody)))
                        .delete(createJsonHandlerEx(ctx -> createTaskResource().deleteTask(ctx.getPathTokens().get("id"))))
                ))
                .path("app/pages/tasks", ctx2 -> ctx2.byMethod(chain2 -> chain2
                        .get(createHtmlHandler(() -> createTaskController().getTasksPage()))
                        .post(createHtmlFormHandler(form -> createTaskController().postTasksPage(form.get("title"), form.get("description"))))
                 ))
                .path("app/pages/tasks/:id", ctx2 -> ctx2.byMethod(chain2 -> chain2
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
        return new TaskRepository(emf.createEntityManager()); // TODO review cleanup
    }

    private static EntityManagerFactory createEntityManagerFactory() {
    	Map<String, String> props = new HashMap<>();
    	config.getConfigProperty("app.jdbc.driver"  ).ifPresent(v -> props.put("javax.persistence.jdbc.driver", v));
    	config.getConfigProperty("app.jdbc.url"     ).ifPresent(v -> props.put("javax.persistence.jdbc.url", v));
    	config.getConfigProperty("app.jdbc.username").ifPresent(v -> props.put("javax.persistence.jdbc.user", v));
    	config.getConfigProperty("app.jdbc.password").ifPresent(v -> props.put("javax.persistence.jdbc.password", v));
    	return Persistence.createEntityManagerFactory("taskDS", props);
    }
}
