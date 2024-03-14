package uz.atmos.gaf.client;

import uz.atmos.gaf.ApiType;

import java.lang.annotation.*;

/**
 * Created by Temurbek Ismoilov on 27/02/24.
 */

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
@Documented
public @interface GafClient {
    ApiType[] types() default {};
    String url();
}
