package uz.atmos.gaf.server.impl;

import uz.atmos.gaf.server.ApiGenerator;

import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.*;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;

/**
 * Created by Temurbek Ismoilov on 05/03/24.
 */

public class RestApiGenerator implements ApiGenerator {
    @Override
    public void generate(Element element, ProcessingEnvironment processingEnv) {
        System.out.println("Invoking " + this.getClass().getSimpleName() + " for " + element);

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
                    
                    @RestController
                    @RequestMapping("/%s")
                    public class %s{
                    """.formatted(packageName, className.toLowerCase(), apiName));

            // fields
            final List<? extends Element> enclosedElements = element.getEnclosedElements();
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
