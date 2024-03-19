package uz.atmos.gaf.server;

import uz.atmos.gaf.ApiType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created by Temurbek Ismoilov on 27/02/24.
 */

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
public @interface GafServer {
    ApiType[] types() default {};
    String url() default "";
}
