package uz.atmos.gaf.client.source_generator.impl;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import org.springframework.web.bind.annotation.RequestMethod;
import uz.atmos.gaf.client.GafClient;
import uz.atmos.gaf.client.GafMethod;
import uz.atmos.gaf.client.RequestHeader;
import uz.atmos.gaf.client.configuration.GafClientConfiguration;
import uz.atmos.gaf.client.source_generator.ClientGenerator;
import uz.atmos.gaf.exception.GafException;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.*;

public class RestClientFreeMakerGenerator implements ClientGenerator {

    private final Set<String> packages = new HashSet<>();
    private final Configuration cfg;

    public RestClientFreeMakerGenerator() {
        cfg = new Configuration(Configuration.VERSION_2_3_31);
        cfg.setClassForTemplateLoading(getClass(), "/");
    }

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
        String feignClientClassName = className + "FeignClient";
        String builderFullName = packageName + "." + apiName;

        String url = gafClientAnnotation.url();
        if (url == null) {
            url = "";
        }

        try {
            Template template = cfg.getTemplate("feign_config_template.ftl");
            StringWriter writer = new StringWriter();
            Map<String, Object> input = new HashMap<>();
            input.put("packageName", packageName);
            input.put("apiName", apiName);
            input.put("feignClientClassName", feignClientClassName);
            input.put("feignClientVariableName", feignClientClassName.substring(0,1).toLowerCase() + feignClientClassName.substring(1));
            input.put("url", url);
            input.put("configVariableName", getConfigClassString(gafClientAnnotation));

            template.process(input, writer);

            try (PrintWriter fileWriter = new PrintWriter(processingEnv.getFiler().createSourceFile(builderFullName).openWriter())) {
                fileWriter.println(writer.toString());
            }
        } catch (IOException | TemplateException e) {
            System.out.println("Feign config generation error: " + e);
            throw new RuntimeException(e);
        }
    }

    private String getConfigClassString(GafClient gafClientAnnotation) {
        String configClassString = "new GafClientConfiguration(){}";
        Class<? extends GafClientConfiguration> clazz = null;
        try {
            clazz = gafClientAnnotation.configuration();
        } catch (MirroredTypeException e) {
            TypeMirror configurationTypeMirror = e.getTypeMirror();
            String configClassName = getClassName(configurationTypeMirror);
            if(!configClassName.equals("GafClientConfiguration")) {
                configClassString = "new " + configClassName + "()";
            }
        }
        return configClassString;
    }

    private void generateSourceCode(Element element, ProcessingEnvironment processingEnv, GafClient gafClientAnnotation) {
        System.out.println("Invoking " + this.getClass().getSimpleName() + " for " + element);

        String serviceClassName = element.getSimpleName().toString();
        String className = serviceClassName.replace("Service", "");
        String packageName = element.getEnclosingElement().toString();
        String apiName = className + "FeignClient";
        String builderFullName = packageName + "." + apiName;

        try {
            Template template = cfg.getTemplate("feign_client_template.ftl");
            StringWriter writer = new StringWriter();
            Map<String, Object> input = new HashMap<>();
            final List<Map<String, Object>> methodStrings = generateMethodStrings(element);
            input.put("packageName", packageName);
            input.put("imports", generateImports());
            input.put("className", className);
            input.put("apiName", apiName);
            input.put("serviceClassName", serviceClassName);
            input.put("methods", methodStrings);
            input.put("baseUrl", getUrlValue(element));

            template.process(input, writer);

            try (PrintWriter fileWriter = new PrintWriter(processingEnv.getFiler().createSourceFile(builderFullName).openWriter())) {
                fileWriter.println(writer.toString());
            }
        } catch (IOException | TemplateException e) {
            System.out.println("ApiProcessor error: " + e);
            throw new RuntimeException(e);
        }
    }

    private List<Map<String, Object>> generateMethodStrings(Element element) {
        List<Map<String, Object>> methodStrings = new ArrayList<>();
        // handle methods
        final List<? extends Element> methods = element.getEnclosedElements().stream()
                .filter(e -> ElementKind.METHOD.equals(e.getKind()))
                .toList();

        for (Element methodElement : methods) {
            ExecutableElement method = (ExecutableElement) methodElement;
            String parameters = processParams(method);
            String returnType = processType(method);
            Map<String, Object> methodMap = new HashMap<>();
            methodMap.put("requestMethod", getRequestMethod(methodElement));
            methodMap.put("urlValue", getUrlValue(methodElement));
            methodMap.put("returnType", returnType);
            methodMap.put("methodName", method.getSimpleName().toString());
            methodMap.put("parameters", parameters);
            methodStrings.add(methodMap);
        }
        return methodStrings;
    }

    private String getRequestMethod(Element methodElement) {
        GafMethod gafMethodAnnotation = methodElement.getAnnotation(GafMethod.class);
        if (gafMethodAnnotation == null) {
            return "Post";
        }

        final RequestMethod requestMethod = gafMethodAnnotation.method();
        return switch (requestMethod) {
            case POST -> "Post";
            case GET -> "Get";
            case DELETE -> "Delete";
            case PATCH -> "Patch";
            case HEAD -> "Head";
            case PUT -> "Put";
            case OPTIONS -> "Options";
            case TRACE -> "Trace";
            default -> "Post";
        };
    }

    private String processParams(ExecutableElement method) {
        List<String> paramStrings = new ArrayList<>();
        for (VariableElement parameter : method.getParameters()) {
            paramStrings.add(processParam(parameter));
        }
        // Join parameter strings with comma
        return String.join(", ", paramStrings);
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

    private String processType(ExecutableElement method) {
        return processType(method.getReturnType(), new StringBuilder());

    }

    private String processType(TypeMirror type, StringBuilder sb) {
        // append type name
        final TypeKind kind = type.getKind();
        String className = getClassName(type);
        sb.append(className);
        // if type is declared continue processing
        if (!kind.isPrimitive() && kind != TypeKind.VOID) {
            DeclaredType declaredType = (DeclaredType) type;
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
        System.out.println("PACKAGES: " + packages);
        StringBuilder sb = new StringBuilder();
        for (String pack : packages) {
            sb.append("import ").append(pack).append(";\n");
        }
        return sb.toString();
    }

    private String getUrlValue(Element methodElement) {
        GafMethod gafMethodAnnotation = methodElement.getAnnotation(GafMethod.class);
        if (gafMethodAnnotation == null) {
            return "";
        }

        final String urlValue = gafMethodAnnotation.value();
        return urlValue != null ? urlValue : "";
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
