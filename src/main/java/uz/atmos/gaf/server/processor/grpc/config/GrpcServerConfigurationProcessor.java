package uz.atmos.gaf.server.processor.grpc.config;

import com.google.auto.service.AutoService;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import java.util.Set;

/**
 * Created by Temurbek Ismoilov on 27/02/24.
 */

@AutoService(Processor.class)
@SupportedSourceVersion(SourceVersion.RELEASE_17)
@SupportedAnnotationTypes("uz.atmos.gaf.server.GafServer")
public class GrpcServerConfigurationProcessor extends AbstractProcessor {
    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        try {
            for (TypeElement annotation : annotations) {
                for (Element element : roundEnv.getElementsAnnotatedWith(annotation)) {
                    var generator = new GrpcServerConfigurationGenerator();
                    generator.generate(element, processingEnv);
                }
            }
            return true;
        } catch (Exception e) {
            System.err.println(e.getMessage());
            return false;
        }
    }
}
