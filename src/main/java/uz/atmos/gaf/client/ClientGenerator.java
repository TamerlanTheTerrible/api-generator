package uz.atmos.gaf.client;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;

/**
 * Created by Temurbek Ismoilov on 05/03/24.
 */

public interface ClientGenerator {
    void generate(Element element, ProcessingEnvironment processingEnv, GafClient gafClientAnnotation);
}
