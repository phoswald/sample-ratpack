package com.github.phoswald.sample;

import static ratpack.jackson.Jackson.fromJson;
import static ratpack.jackson.Jackson.json;

import java.io.StringReader;
import java.io.StringWriter;
import java.nio.file.Path;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.phoswald.sample.sample.EchoRequest;
import com.github.phoswald.sample.sample.SampleController;
import com.github.phoswald.sample.sample.SampleResource;
import com.github.phoswald.sample.task.TaskController;
import com.github.phoswald.sample.task.TaskEntity;
import com.github.phoswald.sample.task.TaskResource;
import com.github.phoswald.sample.utils.ConfigProvider;

import jakarta.xml.bind.JAXB;
import ratpack.form.Form;
import ratpack.func.Action;
import ratpack.handling.Chain;
import ratpack.handling.Context;
import ratpack.handling.Handler;
import ratpack.server.BaseDir;
import ratpack.server.RatpackServer;
import ratpack.server.ServerConfigBuilder;

public class Application {

    private static final Logger logger = LoggerFactory.getLogger(Application.class);

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
        var module = new ApplicationModule();
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
                .get("app/rest/sample/time", createHandler(ctx -> sampleResource.getTime()))
                .get("app/rest/sample/config", createHandler(ctx -> sampleResource.getConfig()))
                .post("app/rest/sample/echo-xml", createXmlHandler(EchoRequest.class, (ctx, reqBody) -> sampleResource.postEcho(reqBody)))
                .post("app/rest/sample/echo-json", createJsonHandler(EchoRequest.class, (ctx, reqBody) -> sampleResource.postEcho(reqBody)))
                .get("app/pages/sample", createHtmlHandler(ctx -> sampleController.getSamplePage()))
                .path("app/rest/tasks", ctx2 -> ctx2.byMethod(chain2 -> chain2
                        .get(createJsonHandler(ctx -> taskResource.getTasks()))
                        .post(createJsonHandler(TaskEntity.class, (ctx, reqBody) -> taskResource.postTasks(reqBody)))
                ))
                .path("app/rest/tasks/:id", ctx2 -> ctx2.byMethod(chain2 -> chain2
                        .get(createJsonHandler(ctx -> taskResource.getTask(ctx.getPathTokens().get("id"))))
                        .put(createJsonHandler(TaskEntity.class, (ctx, reqBody) -> taskResource.putTask(ctx.getPathTokens().get("id"), reqBody)))
                        .delete(createJsonHandler(ctx -> taskResource.deleteTask(ctx.getPathTokens().get("id"))))
                ))
                .path("app/pages/tasks", ctx2 -> ctx2.byMethod(chain2 -> chain2
                        .get(createHtmlHandler(ctx -> taskController.getTasksPage()))
                        .post(createHtmlHandler((ctx, form) -> taskController.postTasksPage(form.get("title"), form.get("description"))))
                 ))
                .path("app/pages/tasks/:id", ctx2 -> ctx2.byMethod(chain2 -> chain2
                        .get(createHtmlHandler(ctx -> taskController.getTaskPage(ctx.getPathTokens().get("id"), ctx.getRequest().getQueryParams().get("action"))))
                        .post(createHtmlHandler((ctx, form) -> taskController.postTaskPage(ctx.getPathTokens().get("id"), form.get("action"), form.get("title"), form.get("description"), form.get("done"))))
                ));
    }

    private Handler createHandler(Function<Context, Object> callback) {
        return ctx -> {
            Object result = callback.apply(ctx);
            ctx.render(result);
        };
    }

    private <R> Handler createXmlHandler(Class<R> reqClass, BiFunction<Context, R, Object> callback) {
        return ctx -> ctx.getRequest().getBody().map(reqBody -> deserializeXml(reqClass, reqBody.getText())).then(
                reqBody -> handleXml(ctx, () -> callback.apply(ctx, reqBody)));
    }

    private void handleXml(Context ctx, Supplier<Object> callback) {
        Object result = callback.get();
        ctx.header("content-type", "text/xml").render(serializeXml(result));
    }

    private Handler createJsonHandler(Function<Context, Object> callback) {
        return ctx -> handleJson(ctx, () -> callback.apply(ctx));
    }

    private <R> Handler createJsonHandler(Class<R> reqClass, BiFunction<Context, R, Object> callback) {
        return ctx -> ctx.parse(fromJson(reqClass)).then(
                reqBody -> handleJson(ctx, () -> callback.apply(ctx, reqBody)));
    }

    private void handleJson(Context ctx, Supplier<Object> callback) {
        Object result = callback.get();
        if(result == null) {
            ctx.notFound();
        } else if(result instanceof String resultString) {
            ctx.render(resultString);
        } else {
            ctx.render(json(result));
        }
    }

    private Handler createHtmlHandler(Function<Context, Object> callback) {
        return ctx -> handleHtml(ctx, () -> callback.apply(ctx));
    }

    private Handler createHtmlHandler(BiFunction<Context, Form, Object> callback) {
        return ctx -> ctx.parse(Form.class).then(form -> handleHtml(ctx, () -> callback.apply(ctx, form)));
    }

    private void handleHtml(Context ctx, Supplier<Object> callback) {
        Object result = callback.get();
        if(result == null) {
            ctx.notFound();
        } else if(result instanceof Path resultPath) {
            ctx.redirect(resultPath.toString());
        } else {
            ctx.header("content-type", "text/html").render(result);
        }
    }

    private static String serializeXml(Object object) {
        var buffer = new StringWriter();
        JAXB.marshal(object, buffer);
        return buffer.toString();
    }

    private static <T> T deserializeXml(Class<T> clazz, String text) {
        return JAXB.unmarshal(new StringReader(text), clazz);
    }
}
