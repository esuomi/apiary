package io.induct.apiary;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.base.Optional;
import io.induct.daniel.Daniel;
import io.induct.http.HttpClient;
import io.induct.http.Response;
import io.induct.http.builders.Request;
import io.induct.http.builders.RequestBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.InputStream;

/**
 * All generated HTTP API clients use this class as their base class.
 *
 * @since 1.1.2016
 */
@SuppressWarnings({"WeakerAccess", "unused"})
public abstract class ApiClient {
    private final Logger log = LoggerFactory.getLogger(ApiClient.class);

    @Inject Daniel daniel;
    @Inject HttpClient httpClient;

    protected RequestBuilder createRequestBuilder() {
        return new RequestBuilder(httpClient);
    }

    protected <T> String asString(T unknownType) {
        if (unknownType instanceof java.util.Optional) {
            java.util.Optional optionalUnknown = ((java.util.Optional) unknownType);
            if (optionalUnknown.isPresent()) {
                return convertToString(optionalUnknown.get());
            }
        }

        if (unknownType instanceof Optional) {
            Optional optionalUnknown = ((Optional) unknownType);
            if (optionalUnknown.isPresent()) {
                return convertToString(optionalUnknown.get());
            }
        }

        return convertToString(unknownType);
    }

    private String convertToString(Object o) {
        if (o instanceof Number) {
            return o.toString();
        }
        return o.toString();
    }

    protected <T> T handleApiCall(Request request, TypeReference<T> targetType) {
        try (Response response = request.get()) {

            switch (response.getStatusCode()) {
                // informational:
                // success:
                case 200: {
                    Optional<InputStream> body = response.getResponseBody();
                    if (body.isPresent()) {
                        log.debug("Body is present");
                        return daniel.deserialize(targetType, response.getResponseBody().get());
                    } else {
                        log.debug("Body is absent, returning empty ApiResponse");
                    }
                }
                // redirect:
                // client error:
                // server error:
                default: return null;
            }
        }
    }
}
