package com.banking.gateway.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Objects;


@Component
@Slf4j
public class IpKeyResolver implements KeyResolver {

    @Override
    public Mono<String> resolve(ServerWebExchange exchange) {
        log.warn("Request limit reached for user");
        return Mono.just(
                Objects.requireNonNull(exchange.getRequest()
                                .getRemoteAddress())
                                .getAddress()
                                .getHostAddress()
        );
    }
}


//@Component
//@Slf4j
//public class UserKeyResolver implements KeyResolver {
//
//    @Override
//    public Mono<String> resolve(ServerWebExchange exchange) {
//        String user = exchange.getRequest()
//                .getHeaders()
//                .getFirst("X-Auth-User"); // or extract from JWT
//
//        log.warn("Request limit reached for user: {}", user);
//        return Mono.justOrEmpty(user)
//                .switchIfEmpty(Mono.just("anonymous"));
//    }
//}

