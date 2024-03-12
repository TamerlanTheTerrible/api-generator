package uz.atmos.gaf.client;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Map;

/**
 * Created by Temurbek Ismoilov on 27/02/24.
 */

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
public @interface GafMethod {
    String url();
    String[] headers() default {};
    Class<?> requestBody() default MockClass.class;

    class MockClass {}
}
