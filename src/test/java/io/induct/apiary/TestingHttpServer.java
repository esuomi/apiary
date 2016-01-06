package io.induct.apiary;

import io.induct.apiary.nasa.NasaApodMockApi;
import io.undertow.Undertow;
import io.undertow.server.handlers.PathHandler;
import org.junit.rules.ExternalResource;

/**
 * @since 28.2.2015
 */
public class TestingHttpServer extends ExternalResource {

    private Undertow undertow;

    @Override
    protected void before() throws Throwable {
        this.undertow = Undertow.builder()
                .addHttpListener(9090, "localhost")
                .setHandler(new PathHandler()
                        .addPrefixPath("/planetary/apod", new NasaApodMockApi()))
                .build();
        undertow.start();
    }

    @Override
    protected void after() {
        undertow.stop();
    }
}
