package Api.Gateway.config;

import org.slf4j.MDC;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Hooks;
import reactor.core.publisher.Operators;

@Component
public class ReactorContextToMdcConfig {
    @EventListener(ApplicationReadyEvent.class)
    public void registerHook() {
        Hooks.onEachOperator("mdc-trace-id", Operators.lift((scannable, subscriber) ->
                new reactor.core.CoreSubscriber<Object>() {
                    @Override
                    public reactor.util.context.Context currentContext() {
                        return subscriber.currentContext();
                    }

                    @Override
                    public void onSubscribe(org.reactivestreams.Subscription s) {
                        subscriber.onSubscribe(s);
                    }

                    @Override
                    public void onNext(Object o) {
                        currentContext().getOrEmpty("X-Trace-Id")
                                .ifPresent(traceId -> MDC.put("traceId", (String) traceId));
                        subscriber.onNext(o);
                        MDC.remove("traceId");
                    }

                    @Override
                    public void onError(Throwable t) {
                        subscriber.onError(t);
                    }

                    @Override
                    public void onComplete() {
                        subscriber.onComplete();
                    }
                }
        ));
    }
}
