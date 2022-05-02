package at.jku.isse.ecco.rest;

import io.micronaut.http.HttpRequest;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.runtime.EmbeddedApplication;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@MicronautTest
class RestTest {

    @Inject
    EmbeddedApplication<?> application;

    @Inject
    @Client("/api")
    HttpClient client;

    @Test
    void testItWorks() {
        Assertions.assertTrue(application.isRunning());
    }

    @Test
    void testCommitController() {
            HttpRequest request = HttpRequest.GET("/repository/all");
            String res = client.toBlocking().retrieve(request);
            Assertions.assertEquals("test", res);
            System.out.println("Executed: " + res);
    }

}
