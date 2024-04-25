package uz.atmos.gaf;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created by Temurbek Ismoilov on 27/02/24.
 */

@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.SOURCE)
public @interface PathVariable {
    String value() default "";
}
