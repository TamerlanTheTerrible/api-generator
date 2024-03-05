package uz.atmos.gaf.server;

import com.google.auto.service.AutoService;
import uz.atmos.gaf.ApiType;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.tools.Diagnostic;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * Created by Temurbek Ismoilov on 27/02/24.
 */

@AutoService(Processor.class)
@SupportedSourceVersion(SourceVersion.RELEASE_17)
@SupportedAnnotationTypes("uz.atmos.gaf.server.GafServer")
public class GafServerProcessor extends AbstractProcessor {

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        for (TypeElement annotation : annotations) {
            for (Element element : roundEnv.getElementsAnnotatedWith(annotation)) {
                GafServer gafServerAnnotation = element.getAnnotation(GafServer.class);
                ApiType[] types = gafServerAnnotation.types();
                Set<ApiType> typeSet = new java.util.HashSet<>(Set.of(types));

                if (typeSet.isEmpty()) {
                    typeSet.addAll(List.of(ApiType.values()));
                }

                for (ApiType type : typeSet) {
                    // Process each type
                    System.out.println("Found type: " + type);
                    ApiGeneratorContainer.get(type).generate(element, processingEnv);
                }
            }
        }

        return true;
    }
}
