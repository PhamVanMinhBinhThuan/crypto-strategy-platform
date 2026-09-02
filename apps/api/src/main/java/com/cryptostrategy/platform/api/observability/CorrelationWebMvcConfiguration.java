package com.cryptostrategy.platform.api.observability;

import java.util.concurrent.Callable;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.async.CallableProcessingInterceptor;
import org.springframework.web.servlet.config.annotation.AsyncSupportConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration(proxyBeanMethods = false)
class CorrelationWebMvcConfiguration implements WebMvcConfigurer {
    @Override
    public void configureAsyncSupport(AsyncSupportConfigurer configurer) {
        configurer.registerCallableInterceptors(new CorrelationCallableInterceptor());
    }

    private static final class CorrelationCallableInterceptor implements CallableProcessingInterceptor {
        private final ThreadLocal<CorrelationContext.Scope> scopes = new ThreadLocal<>();

        @Override
        public <T> void preProcess(NativeWebRequest request, Callable<T> task) {
            Object requestCorrelationId = request.getAttribute(
                    CorrelationId.MDC_KEY,
                    RequestAttributes.SCOPE_REQUEST);
            String correlationId = requestCorrelationId instanceof String value ? value : null;
            scopes.set(CorrelationContext.open(correlationId));
        }

        @Override
        public <T> void postProcess(NativeWebRequest request, Callable<T> task, Object concurrentResult) {
            CorrelationContext.Scope scope = scopes.get();
            try {
                if (scope != null) {
                    scope.close();
                }
            } finally {
                scopes.remove();
            }
        }
    }
}
