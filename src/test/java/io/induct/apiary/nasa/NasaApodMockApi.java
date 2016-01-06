package io.induct.apiary.nasa;

import com.google.common.base.Charsets;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;

import java.nio.ByteBuffer;
import java.util.Deque;

/**
 * Behaves like NASA's APOD API
 *
 * @since 6.1.2016
 */
public class NasaApodMockApi implements HttpHandler {

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        if (exchange.isInIoThread()) {
            exchange.dispatch(this);
            return;
        }

        Deque<String> apiKeys = exchange.getQueryParameters().get("api_key");
        if (apiKeys == null || apiKeys.isEmpty()) {
            exchange.setStatusCode(400);
            exchange.getResponseSender().send("{\"error\":\"You must define api_key=DEMO_KEY as parameter\"}");
        } else {
            String apiKey = apiKeys.getFirst();
            if (!apiKey.equals("DEMO_KEY")) {
                exchange.setStatusCode(400);
                exchange.getResponseSender().send("{\"error\":\"You must define api_key=DEMO_KEY as parameter\"}");
            } else {
                exchange.getResponseSender()
                    .send(ByteBuffer.wrap("{\"url\": \"http://apod.nasa.gov/apod/image/1601/CatalinaBorrellyArcturus2016-01-01_Hemmerich600w.jpg\", \"media_type\": \"image\", \"explanation\": \"This timely, telescopic, two panel mosaic spans about 10 full moons across planet Earth's predawn skies. Recorded as the year began from Tenerife, Canary Islands, near the top of the frame are the faint coma and tail of Comet Borrelly (P/19). A comet with a seven year orbital period, Borrelly's nucleus was visited by the ion propelled spacecraft Deep Space 1 near the beginning of the 21st century. Anchoring the scene at the bottom is brilliant star Arcturus (Alpha Bootes) and Comet Catalina (C/2013 US10) a first time visitor from the Oort Cloud. Catalina's yellowish dust tail extends below and right. Buffeted by winds and storms from the Sun, the comet's complex ion tail sweeps up and toward the right, across most of the field of view. Remarkably, one of the composition's 30 second exposure subframes also caught the trail of a bright meteor, slashing toward the left between comets and bright star.\", \"concepts\": [], \"title\": \"Comets and Bright Star\"}".getBytes(Charsets.UTF_8)));
            }
        }

    }

}
