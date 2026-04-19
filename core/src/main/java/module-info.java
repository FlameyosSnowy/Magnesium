module core {
    uses net.magnesiumbackend.core.http.MagnesiumTransport;
    uses net.magnesiumbackend.core.http.websocket.WebSocketSessionManager;

    uses net.magnesiumbackend.core.config.GeneratedConfigClass;

    uses net.magnesiumbackend.core.annotations.service.GeneratedSubscriberClass;
    uses net.magnesiumbackend.core.annotations.service.GeneratedEmitProxyClass;
    uses net.magnesiumbackend.core.annotations.service.GeneratedExceptionHandlerClass;
    uses net.magnesiumbackend.core.annotations.service.GeneratedRouteRegistrationClass;
    uses net.magnesiumbackend.core.annotations.service.GeneratedWebSocketRegistrationClass;
    uses net.magnesiumbackend.core.annotations.service.GeneratedServiceClass;
    uses net.magnesiumbackend.core.json.JsonProvider;
    uses net.magnesiumbackend.core.extensions.MagnesiumExtension;

    requires io.github.flameyossnowy.velocis;
    requires org.jetbrains.annotations;
    requires org.slf4j;
    requires java.logging;
    requires com.nimbusds.jose.jwt;
    requires org.snakeyaml.engine.v2;
    requires java.desktop;
    requires org.bouncycastle.pkix;
    requires org.bouncycastle.provider;
    requires java.net.http;
    requires org.junit.jupiter.api;
    requires org.tomlj;

    exports net.magnesiumbackend.core;
    exports net.magnesiumbackend.core.annotations;
    exports net.magnesiumbackend.core.annotations.service;
    exports net.magnesiumbackend.core.annotations.enums;
    exports net.magnesiumbackend.core.base;
    exports net.magnesiumbackend.core.event;
    exports net.magnesiumbackend.core.exceptions;
    exports net.magnesiumbackend.core.http;
    exports net.magnesiumbackend.core.http.exceptions;
    exports net.magnesiumbackend.core.http.websocket;
    exports net.magnesiumbackend.core.json;
    exports net.magnesiumbackend.core.config;
    exports net.magnesiumbackend.core.meta;
    exports net.magnesiumbackend.core.auth;
    exports net.magnesiumbackend.core.services;
    exports net.magnesiumbackend.core.route;
    exports net.magnesiumbackend.core.http.messages;
    exports net.magnesiumbackend.core.security;
    exports net.magnesiumbackend.core.headers;
    exports net.magnesiumbackend.core.utils;
    exports net.magnesiumbackend.core.http.response;
    exports net.magnesiumbackend.core.cancellation;
    exports net.magnesiumbackend.core.backpressure;
    exports net.magnesiumbackend.core.reload;
    exports net.magnesiumbackend.core.runtime.kernel;
    exports net.magnesiumbackend.core.runtime.input;
    exports net.magnesiumbackend.core.runtime.lifecycle;
    exports net.magnesiumbackend.core.runtime.config;
    exports net.magnesiumbackend.core.runtime.engine;
}