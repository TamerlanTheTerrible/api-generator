package uz.atmos.gaf.server;

import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.TypeElement;
import java.util.Set;

/**
 * Created by Temurbek Ismoilov on 05/03/24.
 */

public interface ApiGenerator {
    void generate(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv, ProcessingEnvironment processingEnv);
}
