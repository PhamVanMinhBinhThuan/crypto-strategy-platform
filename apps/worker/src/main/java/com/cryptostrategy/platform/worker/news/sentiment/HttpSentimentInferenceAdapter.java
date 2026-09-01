package com.cryptostrategy.platform.worker.news.sentiment;

import com.cryptostrategy.platform.contracts.sentiment.v1.*;
import com.cryptostrategy.platform.news.api.model.*;
import com.cryptostrategy.platform.news.api.port.out.SentimentInferencePort;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.*;
import java.util.concurrent.*;

public final class HttpSentimentInferenceAdapter implements SentimentInferencePort {
    private static final int MAX_BODY=262_144;
    private final HttpClient http; private final URI endpoint; private final String token; private final ObjectMapper json; private final SentimentContractMapper mapper;
    public HttpSentimentInferenceAdapter(HttpClient http,URI baseUri,String token,ObjectMapper json,SentimentContractMapper mapper){this.http=http;this.endpoint=baseUri.resolve("/api/v1/sentiment/analyze");this.token=token;this.json=json;this.mapper=mapper;}
    @Override public CompletionStage<SentimentAnalysisOutcome> analyze(SentimentAnalysisRequest request) {
        try {
            var body=json.writeValueAsString(mapper.toWire(request));
            var outbound=HttpRequest.newBuilder(endpoint).header("Authorization","Bearer "+token).header("Content-Type","application/json").header("X-Correlation-Id",request.requestId()).POST(HttpRequest.BodyPublishers.ofString(body)).build();
            return http.sendAsync(outbound,HttpResponse.BodyHandlers.ofByteArray()).thenApply(response->{
                if(response.body().length>MAX_BODY) throw new SentimentClientException("Sentiment response too large",false,true);
                if(response.statusCode()==200) try{return mapper.fromWire(request,json.readValue(response.body(),SentimentAnalyzeSuccess.class));}catch(java.io.IOException e){throw new SentimentClientException("Malformed sentiment response",false,true,e);}
                boolean retryable=response.statusCode()==429||response.statusCode()==503||response.statusCode()>=500;
                throw new SentimentClientException("Sentiment service returned HTTP "+response.statusCode(),retryable,retryable);
            });
        } catch(java.io.IOException error){return CompletableFuture.failedFuture(new SentimentClientException("Cannot serialize sentiment request",false,false,error));}
    }
}
