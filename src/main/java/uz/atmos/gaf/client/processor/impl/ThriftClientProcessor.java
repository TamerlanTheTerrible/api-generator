package uz.atmos.gaf.client.processor.impl;

import uz.atmos.gaf.client.GafClient;
import uz.atmos.gaf.client.processor.ClientProcessor;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;

/**
 * Created by Temurbek Ismoilov on 12/03/24.
 */

public class ThriftClientProcessor implements ClientProcessor {
    @Override
    public void processor(Element element, ProcessingEnvironment processingEnv, GafClient gafClientAnnotation) {

    }
}
