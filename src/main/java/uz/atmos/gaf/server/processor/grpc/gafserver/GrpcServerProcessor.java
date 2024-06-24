package uz.atmos.gaf.server.processor.grpc.gafserver;

import uz.atmos.gaf.server.GafServer;
import uz.atmos.gaf.server.processor.ApiProcessor;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;

/**
 * Created by Temurbek Ismoilov on 10/06/24.
 */

public class GrpcServerProcessor implements ApiProcessor {
    @Override
    public void process(Element element, ProcessingEnvironment processingEnv, GafServer gafServerAnnotation) {
        //generate gRPC scheme
        GrpcSchemeFreeMakerGenerator schemeFreeMakerGenerator = new GrpcSchemeFreeMakerGenerator();
        schemeFreeMakerGenerator.generate(element, processingEnv, gafServerAnnotation);
        //generate gRPC server configuration
        EnableGrpcServerGenerator configurationGenerator = new EnableGrpcServerGenerator();
//        configurationGenerator.generate(element, processingEnv, gafServerAnnotation);
    }
}
