package uz.atmos.gaf.client.impl;

import org.springframework.web.bind.annotation.RequestMethod;
import uz.atmos.gaf.client.GafClient;
import uz.atmos.gaf.client.GafMethod;
import uz.atmos.gaf.client.RequestHeader;
import uz.atmos.gaf.exception.GafException;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.*;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

/**
 * Created by Temurbek Ismoilov on 12/03/24.
 */

public class RestClientGeneratorGenerator implements uz.atmos.gaf.client.ClientGenerator {

    private final Set<String> packages = new HashSet<>();

    @Override
    public void generate(Element element, ProcessingEnvironment processingEnv, GafClient gafClientAnnotation) {
        generateSourceCode(element, processingEnv, gafClientAnnotation);
        generateFeignConfig(element, processingEnv, gafClientAnnotation);
    }

    private void generateFeignConfig(Element element, ProcessingEnvironment processingEnv, GafClient gafClientAnnotation) {
        String serviceClassName = element.getSimpleName().toString();
        String className = serviceClassName.replace("Service", "");
        String packageName = element.getEnclosingElement().toString();
        String apiName = className + "FeignConfig";
        String builderFullName = packageName + "." + apiName;

        String url = gafClientAnnotation.url();
        if (url == null) {
            url = "";
        }

        try (PrintWriter writer = new PrintWriter(processingEnv.getFiler().createSourceFile(builderFullName).openWriter())){
            writer.println("""
                    package %s;
                                        
                    import feign.Feign;
                    import org.springframework.cloud.openfeign.support.SpringMvcContract;
                    import org.springframework.context.annotation.Bean;
                    import org.springframework.context.annotation.Configuration;
                                        
                    @Configuration
                    public class %s {
                                        
                        @Bean
                        public %s %s() {
                            return Feign.builder()
                                    .contract(new SpringMvcContract())
                                    .target(%sFeignClient.class, "%s");
                        }
                    }""".formatted(packageName,
                    apiName,
                    serviceClassName,
                    serviceClassName.substring(0,1).toLowerCase() + serviceClassName.substring(1),
                    className,
                    url
                    )
            );
        } catch (IOException e) {
            System.out.println("Feign config generation error: " + e);
            throw new RuntimeException(e);
        }
    }

    private void generateSourceCode(Element element, ProcessingEnvironment processingEnv, GafClient gafClientAnnotation) {
        System.out.println("Invoking " + this.getClass().getSimpleName() + " for " + element);

        String serviceClassName = element.getSimpleName().toString();
        String className = serviceClassName.replace("Service", "");
        String packageName = element.getEnclosingElement().toString();
        String apiName = className + "FeignClient";
        String builderFullName = packageName + "." + apiName;

        try (PrintWriter writer = new PrintWriter(processingEnv.getFiler().createSourceFile(builderFullName).openWriter())){
            // handle methods
            final List<? extends Element> methods = element.getEnclosedElements().stream()
                    .filter(e -> ElementKind.METHOD.equals(e.getKind()))
                    .toList();

            List<String> methodStrings = new ArrayList<>();
            for(Element methodElement: methods) {
                ExecutableElement method = (ExecutableElement) methodElement;
                String parameters = processParams(method);
                String returnType = processType(method);
                String methodString = """
                            @Override
                            @%sMapping("%s")
                            %s %s(%s);
                        """.formatted(
                                getRequestMethod(methodElement),
                        getUrlValue(methodElement),
                        returnType,
                        method.getSimpleName(),
                        parameters);
                methodStrings.add(methodString);
            }

            // start source code
            writer.println("""
                    package %s;
                    
                    import org.springframework.cloud.openfeign.FeignClient;
                    import org.springframework.web.bind.annotation.*;
                    %s
                    
                    @FeignClient(value = "%s", url = "%s")
                    public interface %s extends %s {
                    %s
                    }
                    """.formatted(
                            packageName,
                    generateImports(),
                    className.toLowerCase() + "-feign-client",
                    gafClientAnnotation.url(),
                    apiName,
                    serviceClassName,
                    String.join("\n", methodStrings)
                    )
            );
        } catch (IOException e) {
            System.out.println("ApiProcessor error: " + e);
            throw new RuntimeException(e);
        }
    }

    private String getRequestMethod(Element methodElement) {
        GafMethod gafMethodAnnotation = methodElement.getAnnotation(GafMethod.class);
        if (gafMethodAnnotation == null) {
            return "Post";
        }

        final RequestMethod requestMethod = gafMethodAnnotation.method();
        return "Post";
    }


    private String getUrlValue(Element methodElement) {
        GafMethod gafMethodAnnotation = methodElement.getAnnotation(GafMethod.class);
        if (gafMethodAnnotation == null) {
            return "";
        }

        final String urlValue = gafMethodAnnotation.value();
        return urlValue != null ? urlValue : "";
    }

    private String processType(ExecutableElement method) {
        return processType(method.getReturnType(), new StringBuilder());

    }

    private String processType(TypeMirror returnType, StringBuilder sb) {
        // append type name
        final TypeKind kind = returnType.getKind();
        String className = getClassName(returnType);
        sb.append(className);
        // if type is declared continue processing
        if (!kind.isPrimitive() && kind != TypeKind.VOID) {
            DeclaredType declaredType = (DeclaredType) returnType;
            if(!declaredType.getTypeArguments().isEmpty()) {
                final List<? extends TypeMirror> typeArguments = declaredType.getTypeArguments();
                for(int i=0; i<typeArguments.size(); i++) {
                    sb.append("<").append(processType(typeArguments.get(i), new StringBuilder()));
                    if(i<typeArguments.size() - 1) {
                        sb.append(", ");
                    }
                }
                sb.append(">");
            }
        }
        return sb.toString();
    }

    private String processParams(ExecutableElement method) {
        final List<? extends VariableElement> parameters = method.getParameters();
        if(parameters.isEmpty()) {
            return "";
        } else if (parameters.size() == 1) {
            final VariableElement variable = parameters.get(0);
            return processParam(variable);
        } else {
            StringBuilder sb = new StringBuilder();
            for(int i=0; i<parameters.size(); i++) {
                sb.append(processParam(parameters.get(i)));
                if(i < parameters.size() - 1) {
                    sb.append(", ");
                }
            }
            return sb.toString();
        }
    }

    private String processParam(VariableElement variable) {
        String className = processType(variable.asType(), new StringBuilder());
        String varName = variable.getSimpleName().toString();
        //if parameter has RequestHeader annotate it with spring's RequestHeader, else annotate is as RequestBody
        if(variable.getAnnotation(RequestHeader.class) != null ) {
            final String headerName = variable.getAnnotation(RequestHeader.class).value();
            if(headerName == null) {
                throw new GafException("Request header name is null");
            }

            if(!Objects.equals(variable.asType().toString(), "java.lang.String")) {
                throw new GafException("Request header should be type of string");
            }

            return """
                    @RequestHeader(value="%s") %s %s""".formatted(headerName, className, varName);
        } else {
            return """
                    @RequestBody %s %s""".formatted(className, varName);
        }
    }

    private String getClassName(TypeMirror returnType) {
        final TypeKind kind = returnType.getKind();
        if (kind.isPrimitive()) {
            return returnType.toString();
        } else if(kind == TypeKind.VOID) {
            return "void";
        } else {
            DeclaredType declaredType = (DeclaredType) returnType;
            final String className = declaredType.asElement().getSimpleName().toString();
            addToPackageSet(returnType.toString(), className);
            return className;
        }
    }

    private String generateImports() {
        StringBuilder sb = new StringBuilder();
        for(String pack: packages) {
            sb.append("import ").append(pack).append(";\n");
        }
        System.out.println("IMPORTS: " + sb);
        return sb.toString();
    }

    private void addToPackageSet(String fullClassName, String className) {
        if (fullClassName.contains(".")) {
            if (fullClassName.contains("<")) {
                fullClassName = fullClassName.substring(0, fullClassName.indexOf("<"));
            }
            final String packageName = fullClassName.substring(0, fullClassName.lastIndexOf("."));
            packages.add(packageName + "." + className);
        }
    }
}
