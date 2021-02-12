package io.quarkus.qe.jbang;//usr/bin/env jbang "$0" "$@" ; exit $?

//JAVAC_OPTIONS -parameters

//DEPS io.quarkus:quarkus-bom:1.11.0.Final@pom
//DEPS io.quarkus:quarkus-vertx-web
//DEPS io.smallrye.reactive:smallrye-mutiny-vertx-web-client
//DEPS io.quarkus:quarkus-resteasy-mutiny
//DEPS io.quarkus:quarkus-smallrye-openapi
//DEPS io.quarkus:quarkus-swagger-ui

//FILES application.properties=application.properties
//FILES META-INF/resources/index.html

//SOURCES domain/CryptoCurrencyIndex.java
//SOURCES domain/Currency.java
//SOURCES domain/CryptoCurrencyValues.java
//SOURCES domain/Hello.java

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import domain.CryptoCurrencyIndex;
import domain.CryptoCurrencyValues;
import domain.Hello;
import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.configuration.ProfileManager;
import io.quarkus.vertx.web.Param;
import io.quarkus.vertx.web.Route;
import io.quarkus.vertx.web.RouteBase;
import io.smallrye.mutiny.Uni;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.Json;
import io.vertx.core.json.jackson.DatabindCodec;
import io.vertx.ext.web.RoutingContext;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.ext.web.client.WebClient;
import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import org.eclipse.microprofile.openapi.annotations.OpenAPIDefinition;
import org.eclipse.microprofile.openapi.annotations.info.Contact;
import org.eclipse.microprofile.openapi.annotations.info.Info;
import org.eclipse.microprofile.openapi.annotations.info.License;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.logging.Logger;

@OpenAPIDefinition(
        info = @Info(
                title = "Crypto currency API",
                version = "1.0.0",
                contact = @Contact(name = "Crypto currency API Support", email = "techsupport@example.com"),
                license = @License(name = "Apache 2.0",
                        url = "http://www.apache.org/licenses/LICENSE-2.0.html")))
@Tag(name = "stock", description = "crypto currency stock values")
@RouteBase(path = "/stock", produces = "application/json")
@ApplicationScoped
public class stockCryptoCurrency {
    public static final Logger LOG = Logger.getLogger(stockCryptoCurrency.class);

    @Inject
    private Vertx vertx;

    private WebClient client;

    public static void main(String... args) {
        System.setProperty("quarkus.test.profile", "test");
        Quarkus.run(args);
    }

    @PostConstruct
    void initialize() {
        LOG.info("The application is starting with profile " + ProfileManager.getActiveProfile());
        this.client = WebClient.create(vertx);

        ObjectMapper mapper = DatabindCodec.mapper();
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    @Route(methods = HttpMethod.GET, path = "/currency/names")
    Uni<CryptoCurrencyIndex> currencyNames() {
        return new CryptoCurrencyIndex().retrieveCryptoCurrenciesNames(client);
    }

    @Route(methods = HttpMethod.GET, path = "/currency/:currency")
    Uni<CryptoCurrencyValues> currencyValue(@Param("currency") String currency) {
        return new CryptoCurrencyValues().getCurrencyValue(client, currency);
    }

    @Route(methods = HttpMethod.GET, path = "/hello/:name")
    void helloWorld(RoutingContext ctx, @Param("name") String name) {
        ctx.response().end(Json.encode(new Hello(name)));
    }

}