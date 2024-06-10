package uz.atmos.gaf.server.processor.thrift;

import uz.atmos.gaf.server.GafServer;
import uz.atmos.gaf.server.processor.ApiProcessor;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;

/**
 * Created by Temurbek Ismoilov on 05/03/24.
 */

public class ThriftApiProcessor implements ApiProcessor {

    @Override
    public void process(Element element, ProcessingEnvironment processingEnv, GafServer gafServerAnnotation) {

    }
}
