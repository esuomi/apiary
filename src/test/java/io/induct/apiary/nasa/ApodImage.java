package io.induct.apiary.nasa;

import lombok.Builder;
import lombok.Value;

import java.util.List;
import java.util.Optional;

/**
 * @since 1.1.2016
 */
@Value
@Builder
public class ApodImage {
    String url;
    Optional<String> hdurl;
    String mediaType;
    String explanation;
    List<String> concepts;
    String title;
}
