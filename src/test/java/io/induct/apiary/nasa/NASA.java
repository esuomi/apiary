package io.induct.apiary.nasa;

import com.google.common.base.CaseFormat;
import io.induct.apiary.Client;
import io.induct.apiary.Api;

import java.time.LocalDate;
import java.util.Optional;

import static io.induct.apiary.Client.Environment;

/**
 * <a href="https://api.nasa.gov">NASA API</a> client definition.
 *
 * @since 1.1.2016
 */
@Client(
    paramFormat = CaseFormat.LOWER_UNDERSCORE,
    targetPackage = "${root}.generated",
    targetClassName = "${clientName}Impl",
    environments = {
        @Environment(name = "local", root = "http://localhost:9090"),
        @Environment(name = "live", root = "https://api.nasa.gov")
    }
)
public interface NASA {

    @Api(path = "/planetary/apod")
    ApodImage apod(
        Optional<LocalDate> date,
        Optional<Boolean> conceptTags,
        Optional<Boolean> hd,
        String apiKey);
}
