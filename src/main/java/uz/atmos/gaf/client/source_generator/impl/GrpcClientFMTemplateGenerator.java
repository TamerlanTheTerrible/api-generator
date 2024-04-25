package uz.atmos.gaf.client.source_generator.impl;

import uz.atmos.gaf.client.GafClient;
import uz.atmos.gaf.client.source_generator.ClientGenerator;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;

/**
 * Created by Temurbek Ismoilov on 12/03/24.
 */

public class GrpcClientFMTemplateGenerator implements ClientGenerator {
    @Override
    public void generate(Element element, ProcessingEnvironment processingEnv, GafClient gafClientAnnotation) {

    }
}
