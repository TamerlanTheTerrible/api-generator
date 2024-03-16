package uz.atmos.gaf.client.configuration;

import org.springframework.context.annotation.Configuration;

import java.util.*;

/**
 * Created by Temurbek Ismoilov on 15/03/24.
 */

@Configuration
public interface GafClientConfiguration {

    int DEFAULT_CONNECTION_TIMEOUT = 5000; // 5 seconds
    int DEFAULT_READ_TIMEOUT = 10000; // 10 seconds

    default int connectionTimeout() {
        return DEFAULT_CONNECTION_TIMEOUT;
    }

    default int readTimeout() {
        return DEFAULT_READ_TIMEOUT;
    }

    default <E> E encoder() {
        return null;
    }

    default <D> D decoder() {
        return null;
    }

    default <R> R errorDecoder() {
        return null;
    }

    default <I> List<I> interceptors() {
        return Collections.emptyList();
    }

    default Map<String, String> headers() {
        return new HashMap<>() {{put("Content-Type", "application/json");}};
    }

    default LogLevel logLevel() {
        return LogLevel.FULL;
    }

    enum LogLevel {
        NONE,
        BASIC,
        HEADERS,
        FULL
    }
}
