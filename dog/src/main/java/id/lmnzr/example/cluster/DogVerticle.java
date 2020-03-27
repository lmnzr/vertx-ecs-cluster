package id.lmnzr.example.cluster;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;

import java.util.logging.Logger;

public class DogVerticle extends AbstractVerticle {
    private static Logger log = Logger.getLogger(DogVerticle.class.getPackageName());

    @Override
    public void start(Promise<Void> promise){
        DogService dogService = new DogService();

        vertx.eventBus().consumer("barking",m->{
            log.info("Message For barking");
            m.reply(dogService.barking());
        });

        promise.complete();
    }
}

