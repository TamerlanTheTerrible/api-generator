package org.example;

import com.google.auto.service.AutoService;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
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
@SupportedAnnotationTypes("org.example.Api")
public class ApiProcessor extends AbstractProcessor {

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        annotations.forEach(annotation -> roundEnv.getElementsAnnotatedWith(annotation)
                        .forEach(this::generateApi)
        );

        return true;
    }

    private void generateApi(Element element) {
        String className = element.getSimpleName().toString();
        String packageName = element.getEnclosingElement().toString();
        String apiName = className + "Controller";
        String builderFullName = packageName + "." + apiName;
        try (PrintWriter writer = new PrintWriter(
                processingEnv.getFiler().createSourceFile(builderFullName).openWriter())){
            //
            writer.println("""
                    package %s;
                    
                    import org.springframework.web.bind.annotation.*;
                    import lombok.*;
                    
                    @RestController
                    @RequestMapping("/%s")
                    @RequiredArgsConstructor
                    public class %s{
                    """.formatted(packageName, className.toLowerCase(), apiName));

            // fields
            final List<? extends Element> enclosedElements = element.getEnclosedElements();
            System.out.println("ELEMENTS: " + Arrays.toString(enclosedElements.toArray()));
            // handle methods
            final List<? extends Element> methods = enclosedElements.stream()
                    .filter(e -> ElementKind.METHOD.equals(e.getKind()))
                    .toList();

            for(Element methodElement: methods) {
                ExecutableElement method = (ExecutableElement) methodElement;
                writer.println("""
                       @PostMapping("/%s")
                       %s %s(%s) {
                           return null;
                       }
                   """.formatted(
                           methodElement.getSimpleName(),
                        method.getReturnType().toString(),
                        methodElement.getSimpleName(),
                        generateParams(method.getParameters())));
            }

            writer.println("}");
        } catch (IOException e) {
            System.out.println("ApiProcessor error: " + e);
            throw new RuntimeException(e);
        }
    }



    private String generateParams(List<? extends VariableElement> list) {
        StringBuilder sb = new StringBuilder();
        int i=1;
        for(String paramType : list.stream().map(t -> t.asType().toString()).toList()) {
            sb
                    .append(paramType)
                    .append(" ").append("param").append(i++)
                    .append(" ");
        }

        return !sb.isEmpty() ? sb.substring(0, sb.length()-1) : sb.toString();
    }
}
