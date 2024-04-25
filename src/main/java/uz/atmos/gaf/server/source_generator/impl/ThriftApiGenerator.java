package uz.atmos.gaf.server.source_generator.impl;

import uz.atmos.gaf.server.GafServer;
import uz.atmos.gaf.server.source_generator.ApiGenerator;

import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import java.util.Set;

/**
 * Created by Temurbek Ismoilov on 05/03/24.
 */

public class ThriftApiGenerator implements ApiGenerator {

    @Override
    public void generate(Element element, ProcessingEnvironment processingEnv, GafServer gafServerAnnotation) {

    }
}
