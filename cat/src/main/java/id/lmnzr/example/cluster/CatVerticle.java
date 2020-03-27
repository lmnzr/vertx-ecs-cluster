package id.lmnzr.example.cluster;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;

import java.util.logging.Logger;

public class CatVerticle extends AbstractVerticle {
    private static Logger log = Logger.getLogger(CatVerticle.class.getPackageName());
    @Override
    public void start(Promise<Void> promise){
        CatService catService = new CatService();

        vertx.eventBus().consumer("meowing",m->{
            log.info("Message For meowing");
            m.reply(catService.meowing());
        });

        promise.complete();
    }
}
