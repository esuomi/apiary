package io.induct.apiary.nasa;

import com.google.common.base.CaseFormat;
import io.induct.apiary.Client;
import io.induct.apiary.Api;

import java.time.LocalDate;
import java.util.Optional;

/**
 * <a href="https://api.nasa.gov">NASA API</a> client definition.
 *
 * @since 1.1.2016
 */
@Client(
    paramFormat = CaseFormat.LOWER_UNDERSCORE,
    targetPackage = "${root}.generated",
    targetClassName = "${clientName}Impl"
)
public interface NASA {

    @Api(url = "https://api.nasa.gov/planetary/apod")
    ApodImage apod(Optional<LocalDate> date, Optional<Boolean> conceptTags, Optional<Boolean> hd, String apiKey);
}
