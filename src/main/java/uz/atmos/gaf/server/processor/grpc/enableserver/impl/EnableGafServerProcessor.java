package uz.atmos.gaf.server.processor.grpc.enableserver.impl;

import com.google.auto.service.AutoService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RestController;
import uz.atmos.gaf.ApiType;
import uz.atmos.gaf.server.GafServer;
import uz.atmos.gaf.server.processor.ApiProcessorContainer;
import uz.atmos.gaf.server.processor.grpc.enableserver.EnableGrpcServer;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import java.util.List;
import java.util.Set;

/**
 * Created by Temurbek Ismoilov on 27/02/24.
 */

@AutoService(Processor.class)
@SupportedSourceVersion(SourceVersion.RELEASE_17)
@SupportedAnnotationTypes("uz.atmos.gaf.server.processor.grpc.enableserver")
public class EnableGafServerProcessor extends AbstractProcessor {
    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        try {
            for (TypeElement annotation : annotations) {
                for (Element element : roundEnv.getElementsAnnotatedWith(annotation)) {
                    EnableGrpcServer gafServerAnnotation = element.getAnnotation(EnableGrpcServer.class);
                    var generator = new GrpcServerConfigurationGenerator();
                    generator.generate(element, processingEnv, gafServerAnnotation);
                }
            }
            return true;
        } catch (Exception e) {
            System.err.println(e.getMessage());
            return false;
        }
    }
}
