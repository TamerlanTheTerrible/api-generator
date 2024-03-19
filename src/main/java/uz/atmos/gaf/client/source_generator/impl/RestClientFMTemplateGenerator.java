package uz.atmos.gaf.client.source_generator.impl;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import uz.atmos.gaf.ElementUtil;
import uz.atmos.gaf.RequestParam;
import uz.atmos.gaf.client.GafClient;
import uz.atmos.gaf.client.GafMethod;
import uz.atmos.gaf.RequestHeader;
import uz.atmos.gaf.client.configuration.GafClientConfiguration;
import uz.atmos.gaf.client.source_generator.ClientGenerator;
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

public class RestClientFMTemplateGenerator implements ClientGenerator {

    private final Set<String> packages ;
    private final Configuration cfg;

    public RestClientFMTemplateGenerator() {
        packages = new HashSet<>();
        cfg = new Configuration(Configuration.VERSION_2_3_31);
        cfg.setClassForTemplateLoading(getClass(), "/templates/client/");
    }

    @Override
    public void generate(Element element, ProcessingEnvironment processingEnv, GafClient gafClientAnnotation) {
        generateSourceCode(element, processingEnv, gafClientAnnotation);
        generateFeignConfig(element, processingEnv, gafClientAnnotation);
    }

    private void generateFeignConfig(Element element, ProcessingEnvironment processingEnv, GafClient gafClientAnnotation) {
        System.out.println("Generating rest client config template for " + element);

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
            input.put("baseUrl", getUrlValue(element.getAnnotation(GafClient.class), "/" + className.toLowerCase()));

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
            String urlValue = getUrlValue(methodElement.getAnnotation(GafMethod.class), methodElement.getSimpleName().toString());
            Map<String, Object> methodMap = new HashMap<>();

            methodMap.put("requestMethod", ElementUtil.getRequestMethod(methodElement));
            methodMap.put("urlValue", urlValue);
            methodMap.put("returnType", processType(method, packages));
            methodMap.put("methodName", method.getSimpleName().toString());
            methodMap.put("parameters", processParams(method, urlValue));
            methodMapList.add(methodMap);
        }
        return methodMapList;
    }

    private String processParams(ExecutableElement method, String urlValue) {
        List<String> paramStrings = new ArrayList<>();
        Set<String> paramNames = getParamNames(urlValue);
        for (VariableElement parameter : method.getParameters()) {
            paramStrings.add(processParam(parameter, paramNames));
        }
        //check if all request params are provided
        if(!paramNames.isEmpty()) {
            final String errorMsg = "Request parameters not found : " + Arrays.toString(paramNames.toArray());
            System.err.println(errorMsg);
            throw new GafException(errorMsg);
        }
        // Join parameter strings with comma
        return String.join(", ", paramStrings);
    }


    private String processParam(VariableElement variable, Set<String> paramNames) {
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
            //check if the param is in url
            String requestParamAnnotation = variable.getAnnotation(RequestParam.class).value();
            if(!paramNames.contains(varName) && !paramNames.contains(requestParamAnnotation)) {
                System.err.println("Unknown request parameter: " + varName);
                throw new GafException("Unknown request parameter: " + varName);
            }
            paramNames.remove(varName);
            return """
                    @Param(value="%s") %s %s""".formatted(varName, className, className);
        } else {
            return """
                    @RequestBody %s %s""".formatted(className, varName);
        }
    }
}
