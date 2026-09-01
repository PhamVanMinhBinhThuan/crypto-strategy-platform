package com.cryptostrategy.platform.worker.news.sentiment;

import com.cryptostrategy.platform.contracts.sentiment.v1.SentimentHealthResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.*;
import java.util.concurrent.*;

public final class SentimentReadinessProbe {
    private final HttpClient http; private final URI endpoint; private final ObjectMapper json; private final String expectedModel;
    public SentimentReadinessProbe(HttpClient http,URI base,ObjectMapper json,String expectedModel){this.http=http;this.endpoint=base.resolve("/health/ready");this.json=json;this.expectedModel=expectedModel;}
    public CompletionStage<Boolean> ready(){
        var request=HttpRequest.newBuilder(endpoint).GET().build();
        return http.sendAsync(request,HttpResponse.BodyHandlers.ofByteArray()).handle((response,error)->{
            if(error!=null||response.statusCode()!=200||response.body().length>8192)return false;
            try{var health=json.readValue(response.body(),SentimentHealthResponse.class);return "READY".equals(health.status())&&health.modelVersion().filter(expectedModel::equals).isPresent()&&health.contractVersion().filter("sentiment-v1"::equals).isPresent();}
            catch(Exception invalid){return false;}
        });
    }
}
