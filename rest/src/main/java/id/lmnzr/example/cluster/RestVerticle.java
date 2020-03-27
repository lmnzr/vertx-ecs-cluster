package id.lmnzr.example.cluster;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;

import java.util.logging.Logger;

public class RestVerticle extends AbstractVerticle {
    private static Logger log = Logger.getLogger(RestVerticle.class.getPackageName());

    @Override
    public void start(Promise<Void> startPromise) {
        Router router = createRouter();
        vertx.createHttpServer()
                .requestHandler(router)
                .listen(8080, http -> {
                    if (http.succeeded()) {
                        startPromise.complete();
                        System.out.println
                                ("HTTP server started on port 8080");
                    } else {
                        startPromise.fail(http.cause());
                    }
                });
    }

    private Router createRouter(){
        Router router = Router.router(vertx);
        router.route().handler(BodyHandler.create());
        router.get("/").handler(this::helloHandler);
        router.get("/barking").handler(this::barkHandler);
        router.get("/meowing").handler(this::meowHandler);
        return router;
    }

    private void helloHandler(RoutingContext context){
        log.info("ACCESS 200 [/]");
        context.response()
                .putHeader("content-type", "text/plain")
                .end("Hello from Vert.x!");
    }

    private void barkHandler(RoutingContext context){
        vertx.eventBus().request("barking","test",handler->{
            if(handler.succeeded()){
                log.info("ACCESS 200 [/barking]");
                context.response()
                        .end(handler.result().body().toString());
            } else{
                log.info("ACCESS 500 [/barking]");
                context.response().setStatusCode(500)
                        .end(handler.cause().getMessage());
            }
        });
    }

    private void meowHandler(RoutingContext context){
        vertx.eventBus().request("meowing","",handler->{
            if(handler.succeeded()){
                log.info("ACCESS 200 [/meowing]");
                context.response()
                        .end(handler.result().body().toString());
            } else{
                log.info("ACCESS 500 [/meowing]");
                context.response().setStatusCode(500)
                        .end(handler.cause().getMessage());
            }
        });
    }
}
