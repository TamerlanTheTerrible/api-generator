package uz.atmos.gaf.client.source_generator;

import com.google.auto.service.AutoService;
import org.springframework.cloud.openfeign.FeignClient;
import uz.atmos.gaf.ApiType;
import uz.atmos.gaf.client.GafClient;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * Created by Temurbek Ismoilov on 12/03/24.
 */

@AutoService(Processor.class)
@SupportedSourceVersion(SourceVersion.RELEASE_17)
@SupportedAnnotationTypes("uz.atmos.gaf.client.GafClient")
public class GafClientProcessor extends AbstractProcessor {
    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        for(TypeElement annotation: annotations) {
            for(Element element: roundEnv.getElementsAnnotatedWith(annotation)) {
                GafClient gafClientAnnotation = element.getAnnotation(GafClient.class);
                ApiType[] types = gafClientAnnotation.types();
                Set<ApiType> typeSet = new java.util.HashSet<>(Set.of(types));

                if (typeSet.isEmpty()) {
                    typeSet.addAll(List.of(ApiType.values()));
                }

                for (ApiType type: typeSet) {
                    if(type == ApiType.REST && element.getAnnotation(FeignClient.class) != null) {
                        continue;
                    }
                    ClientGeneratorContainer.get(type).generate(element, processingEnv, gafClientAnnotation);
                }
            }
        }
        return true;
    }
}
