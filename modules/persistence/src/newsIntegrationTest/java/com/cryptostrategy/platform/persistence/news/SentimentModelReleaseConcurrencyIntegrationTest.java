package com.cryptostrategy.platform.persistence.news;

import static org.junit.jupiter.api.Assertions.*;
import com.cryptostrategy.platform.news.api.model.SentimentModelRelease;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;

class SentimentModelReleaseConcurrencyIntegrationTest extends PostgresNewsTestSupport {
    @Test void concurrent_identical_registration_converges_and_conflicting_provenance_is_rejected() throws Exception {
        String version="release-"+com.cryptostrategy.platform.domain.api.identity.Ulids.generate();
        var release=new SentimentModelRelease(version,"model","prep","sentiment-v1");var ready=new CountDownLatch(2);var start=new CountDownLatch(1);var pool=Executors.newFixedThreadPool(2);
        try{
            Callable<Void> action=()->{ready.countDown();start.await();persistence.releases().registerOrVerify(release);return null;};
            var one=pool.submit(action);var two=pool.submit(action);ready.await();start.countDown();one.get();two.get();
        }finally{pool.shutdownNow();}
        assertEquals(1,jdbc.queryForObject("select count(*) from news.sentiment_model_release where model_version=?",Integer.class,version));
        assertThrows(IllegalStateException.class,()->persistence.releases().registerOrVerify(new SentimentModelRelease(version,"other","prep","sentiment-v1")));
        assertDoesNotThrow(()->persistence.releases().registerOrVerify(new SentimentModelRelease(version+"-next","other","prep-2","sentiment-v1")));
    }
}
