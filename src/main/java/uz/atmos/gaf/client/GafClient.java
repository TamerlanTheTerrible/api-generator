package uz.atmos.gaf.client;

import uz.atmos.gaf.ApiType;
import uz.atmos.gaf.client.configuration.GafClientConfiguration;

import java.lang.annotation.*;

/**
 * Created by Temurbek Ismoilov on 27/02/24.
 */

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
@Documented
public @interface GafClient {
    ApiType[] types() default {ApiType.THRIFT, ApiType.REST, ApiType.GRPC};
    String url();
    Class<? extends GafClientConfiguration> configuration() default GafClientConfiguration.class;
}
