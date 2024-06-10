package uz.atmos.gaf.server.source_generator;

import com.google.auto.service.AutoService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RestController;
import uz.atmos.gaf.ApiType;
import uz.atmos.gaf.server.GafServer;

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
                    if(type == ApiType.REST && (element.getAnnotation(Controller.class) != null || element.getAnnotation(RestController.class) != null)) {
                        continue;
                    }
                    ApiGeneratorContainer.get(type).generate(element, processingEnv, gafServerAnnotation);
                }
            }
        }

        return true;
    }
}
