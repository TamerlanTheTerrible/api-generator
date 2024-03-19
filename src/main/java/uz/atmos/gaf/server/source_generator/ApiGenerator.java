package uz.atmos.gaf.server.source_generator;

import uz.atmos.gaf.server.GafServer;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;

/**
 * Created by Temurbek Ismoilov on 05/03/24.
 */

public interface ApiGenerator {
    void generate(Element element, ProcessingEnvironment processingEnv, GafServer gafServerAnnotation);
}
