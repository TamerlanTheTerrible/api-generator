package uz.atmos.gaf.server.impl;

import uz.atmos.gaf.server.ApiGenerator;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

/**
 * Created by Temurbek Ismoilov on 05/03/24.
 */

public class RestApiGenerator implements ApiGenerator {
    @Override
    public void generate(Element element, ProcessingEnvironment processingEnv) {
        System.out.println("Invoking " + this.getClass().getSimpleName() + " for " + element);

        String serviceClassName = element.getSimpleName().toString();
        String className = serviceClassName.replace("Service", "");
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
                        private final %s %s;
                        
                        public %s(%s %s){
                            this.%s = %s;
                        }
                    """.formatted(
                            packageName,
                    className.toLowerCase(),
                    apiName,
                    serviceClassName,
                    serviceClassName.toLowerCase(),
                    apiName,
                    serviceClassName,
                    serviceClassName.toLowerCase(),
                    serviceClassName.toLowerCase(),
                    serviceClassName.toLowerCase()
                    )
            );

            // handle methods
            final List<? extends Element> methods = element.getEnclosedElements().stream()
                    .filter(e -> ElementKind.METHOD.equals(e.getKind()))
                    .toList();

            for(Element methodElement: methods) {
                ExecutableElement method = (ExecutableElement) methodElement;
                final List<? extends VariableElement> parameters = method.getParameters();
                VariableElement param = parameters.isEmpty() ? null : parameters.get(0);
                String paramString = generateParams(param);
                writer.println("""
                       @PostMapping("/%s")
                       %s %s(@RequestBody %s) {
                           return %s.%s(%s);
                       }
                   """.formatted(
                        methodElement.getSimpleName(),
                        method.getReturnType().toString(),
                        methodElement.getSimpleName(),
                        paramString,
                        serviceClassName.toLowerCase(),
                        methodElement.getSimpleName(),
                        paramString == null ? "" : paramString.split(" ")[1]
                        )

                );
            }

            writer.println("}");
        } catch (IOException e) {
            System.out.println("ApiProcessor error: " + e);
            throw new RuntimeException(e);
        }
    }

    private <T extends VariableElement> String generateParams(T element) {
        if (element == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        String paramType = element.asType().toString();
        sb
                .append(paramType)
                .append(" ").append("param")
                .append(" ");

        return !sb.isEmpty() ? sb.substring(0, sb.length()-1) : sb.toString();
    }

    class TestClass {
        private String testString;
        private String testInteger;

        public TestClass() {}

        public String getTestString() {
            return testString;
        }

        public void setTestString(String testString) {
            this.testString = testString;
        }

        public String getTestInteger() {
            return testInteger;
        }

        public void setTestInteger(String testInteger) {
            this.testInteger = testInteger;
        }
    }
}
