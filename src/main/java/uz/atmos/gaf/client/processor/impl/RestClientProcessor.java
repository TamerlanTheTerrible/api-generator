package uz.atmos.gaf.client.processor.impl;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import uz.atmos.gaf.PathVariable;
import uz.atmos.gaf.RequestParam;
import uz.atmos.gaf.RequestParamMap;
import uz.atmos.gaf.client.GafClient;
import uz.atmos.gaf.RequestHeader;
import uz.atmos.gaf.client.configuration.GafClientConfiguration;
import uz.atmos.gaf.client.processor.ClientProcessor;
import uz.atmos.gaf.exception.GafException;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.type.TypeMirror;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

import static uz.atmos.gaf.ElementUtil.*;

public class RestClientProcessor implements ClientProcessor {

    private final Set<String> packages ;
    private final Configuration cfg;

    public RestClientProcessor() {
        packages = new HashSet<>();
        cfg = new Configuration(Configuration.VERSION_2_3_31);
        cfg.setClassForTemplateLoading(getClass(), "/templates/client/");
    }

    @Override
    public void processor(Element element, ProcessingEnvironment processingEnv, GafClient gafClientAnnotation) {
        generateSourceCode(element, processingEnv, gafClientAnnotation);
        generateFeignConfig(element, processingEnv, gafClientAnnotation);
    }

    private void generateFeignConfig(Element element, ProcessingEnvironment processingEnv, GafClient gafClientAnnotation) {
        System.out.println("Generating rest client config template for " + element);

        String serviceClassName = element.getSimpleName().toString();
        String className = serviceClassName.replace("Service", "");
        String packageName = element.getEnclosingElement().toString();
        String apiName = className + "GafClientConfiguration";
        String feignClientClassName = className + "FeignClient";
        String builderFullName = packageName + "." + apiName;

        String url = gafClientAnnotation.url();
        if (url == null) {
            url = "";
        }

        try(PrintWriter fileWriter = new PrintWriter(processingEnv.getFiler().createSourceFile(builderFullName).openWriter())) {
            //generate input
            Map<String, Object> input = new HashMap<>();
            input.put("packageName", packageName);
            input.put("apiName", apiName);
            input.put("feignClientClassName", feignClientClassName);
            input.put("feignClientVariableName", feignClientClassName.substring(0,1).toLowerCase() + feignClientClassName.substring(1));
            input.put("url", url);
            input.put("configVariableName", getConfigClassString(gafClientAnnotation));
            // process the template
            Template template = cfg.getTemplate("feign_config_template.ftl");
            template.process(input, fileWriter);
        } catch (IOException | TemplateException e) {
            System.err.println("Feign config generation error: " + e);
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
            String configClassName = getClassName(configurationTypeMirror, packages);
            if(!configClassName.equals("GafClientConfiguration")) {
                configClassString = "new " + configClassName + "()";
            }
        }
        return configClassString;
    }

    private void generateSourceCode(Element element, ProcessingEnvironment processingEnv, GafClient gafClientAnnotation) {
        System.out.println("Generating rest client template for " + element);

        String serviceClassName = element.getSimpleName().toString();
        String className = serviceClassName.replace("Service", "");
        String packageName = element.getEnclosingElement().toString();
        String apiName = className + "FeignClient";
        String builderFullName = packageName + "." + apiName;

        try (PrintWriter writer = new PrintWriter(processingEnv.getFiler().createSourceFile(builderFullName).openWriter())) {
            Template template = cfg.getTemplate("feign_client_template.ftl");
            Map<String, Object> input = new HashMap<>();
            final List<Map<String, Object>> methodStrings = generateMethodStrings(element);

            input.put("packageName", packageName);
            input.put("imports", generateImports(packages));
            input.put("className", className);
            input.put("apiName", apiName);
            input.put("serviceClassName", serviceClassName);
            input.put("methods", methodStrings);
            input.put("baseUrl", getUrl(element.getAnnotation(GafClient.class), "/" + className.toLowerCase()));

            template.process(input, writer);
        } catch (IOException | TemplateException e) {
            System.out.println(getClass().getSimpleName() + " error: " + e);
            throw new GafException(e.getMessage());
        }
    }

    private List<Map<String, Object>> generateMethodStrings(Element element) {
        List<Map<String, Object>> methodMapList = new ArrayList<>();
        // handle methods
        final List<? extends Element> methods = element.getEnclosedElements().stream()
                .filter(e -> ElementKind.METHOD.equals(e.getKind()))
                .toList();

        for (Element methodElement : methods) {
            ExecutableElement method = (ExecutableElement) methodElement;
            Map<String, Object> methodMap = new HashMap<>();
            methodMap.put("requestMethod", getRequestMethod(methodElement));
            methodMap.put("parameters", processParams(method));
            methodMap.put("urlValue", getUrl(method));
            methodMap.put("returnType", processType(method, packages));
            methodMap.put("methodName", method.getSimpleName().toString());
            methodMapList.add(methodMap);
        }
        return methodMapList;
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
        String className = processType(variable.asType(), new StringBuilder(), packages);
        String varName = variable.getSimpleName().toString();
        //if parameter has RequestHeader annotate it with spring's RequestHeader, else annotate is as RequestBody
        if(variable.getAnnotation(RequestHeader.class) != null) {
            final String headerName = variable.getAnnotation(RequestHeader.class).value();
            if(headerName == null) {
                throw new GafException("Request header name is null");
            }

            if(!Objects.equals(variable.asType().toString(), "java.lang.String")) {
                throw new GafException("Request header should be type of string");
            }

            return """
                    @RequestHeader(value="%s") %s %s""".formatted(headerName, className, varName);
        } else if(variable.getAnnotation(RequestParam.class) != null) {
            return """
                    @RequestParam(value="%s") %s %s""".formatted(varName, className, varName);
        } else if(variable.getAnnotation(RequestParamMap.class) != null) {
            return """
                    @SpringQueryMap %s %s""".formatted(className, varName);
        } else if(variable.getAnnotation(PathVariable.class) != null) {
            return """
                    @PathVariable(value="%s") %s %s""".formatted(varName, className, varName);
        } else {
            return """
                    @RequestBody %s %s""".formatted(className, varName);
        }
    }
}
