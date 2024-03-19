<#assign packageName = packageName>
<#assign apiName = apiName>
<#assign feignClientClassName = feignClientClassName>
<#assign feignClientVariableName = feignClientClassName?uncap_first>

package ${packageName};

import ch.qos.logback.classic.Level;
import org.slf4j.LoggerFactory;
import feign.*;
import feign.codec.*;
import feign.slf4j.Slf4jLogger;
import feign.jackson.JacksonDecoder;
import feign.jackson.JacksonEncoder;
import org.springframework.cloud.openfeign.support.SpringMvcContract;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import uz.atmos.gaf.client.configuration.GafClientConfiguration;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Configuration
public class ${apiName} {

    @Bean
    public ${feignClientClassName} ${feignClientVariableName}() {
        GafClientConfiguration config = gafClientConfiguration();

        return Feign.builder()
            .contract(new SpringMvcContract())
            .requestInterceptors(getInterceptors(config))
            .options(new Request.Options(config.connectionTimeout(), config.readTimeout()))
            .encoder(getEncoder(config))
            .decoder(getDecoder(config))
            .errorDecoder(getErrorDecoder(config))
            .logger(getLogger())
            .logLevel(getLogLevel(config))
            .retryer(Retryer.NEVER_RETRY)
            .target(${feignClientClassName}.class, "${url}");
        }

    private GafClientConfiguration gafClientConfiguration() {
        return ${configVariableName};
    }

    private Slf4jLogger getLogger() {
        ch.qos.logback.classic.Logger specificLogger = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(this.getClass().getPackageName());
        specificLogger.setLevel(Level.DEBUG);
        return new Slf4jLogger(specificLogger);
    }

    private Logger.Level getLogLevel(GafClientConfiguration config) {
        return switch (config.logLevel()) {
            case NONE -> Logger.Level.NONE;
            case HEADERS -> Logger.Level.HEADERS;
            case FULL -> Logger.Level.FULL;
            default -> Logger.Level.BASIC;
        };
    }

    private RequestInterceptor headerInterceptor(GafClientConfiguration config) {
        return requestTemplate -> {
            final Map<String, String> headerMap = config.headers();
            for (String headerName : headerMap.keySet()) {
                requestTemplate.header(headerName, headerMap.get(headerName));
            }
        };
    }

    private ErrorDecoder getErrorDecoder(GafClientConfiguration config) {
        return config.errorDecoder() != null ? config.errorDecoder() : new ErrorDecoder.Default();
    }

    private Encoder getEncoder(GafClientConfiguration config) {
        return config.encoder() != null ? config.encoder() : new JacksonEncoder();
    }

    private Decoder getDecoder(GafClientConfiguration config) {
        return config.decoder() != null ? config.decoder() : new JacksonDecoder();
    }

    private List<RequestInterceptor> getInterceptors(GafClientConfiguration config) {
        List<RequestInterceptor> interceptors = new ArrayList<>(config.interceptors());
            if(!config.headers().isEmpty()) {
                interceptors.add(headerInterceptor(config));
            }
        return interceptors;
    }
}