package uz.atmos.gaf.server.processor.rest;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import uz.atmos.gaf.ElementUtil;
import uz.atmos.gaf.RequestHeader;
import uz.atmos.gaf.client.GafMethod;
import uz.atmos.gaf.exception.GafException;
import uz.atmos.gaf.server.GafServer;
import uz.atmos.gaf.server.processor.ApiProcessor;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

import static uz.atmos.gaf.ElementUtil.*;

/**
 * Created by Temurbek Ismoilov on 05/03/24.
 */

public class RestServerProcessor implements ApiProcessor {

    private final Set<String> packages ;
    private final Configuration cfg;

    public RestServerProcessor() {
        packages = new HashSet<>();
        cfg = new Configuration(Configuration.VERSION_2_3_31);
        cfg.setClassForTemplateLoading(getClass(), "/templates/server/rest/");
    }

    @Override
    public void process(Element element, ProcessingEnvironment processingEnv, GafServer gafServerAnnotation) {
        System.out.println("Generating rest api template for " + element);

        String serviceClassName = element.getSimpleName().toString();
        String className = serviceClassName.replace("Service", "");
        String packageName = element.getEnclosingElement().toString();
        String apiName = className + "Controller";
        String builderFullName = packageName + "." + apiName;
        final List<Map<String, Object>> methodStrings = generateMethods(element);

        try (PrintWriter writer = new PrintWriter(processingEnv.getFiler().createSourceFile(builderFullName).openWriter())){
            //generate input
            Map<String, Object> input = new HashMap<>();
            input.put("packageName", packageName);
            input.put("imports", generateImports(packages));
            input.put("apiName", apiName);
            input.put("className", className);
            input.put("baseUrl", getUrl(gafServerAnnotation, className.toLowerCase()));
            input.put("serviceClassName", serviceClassName);
            input.put("methods", methodStrings);
            //process template
            Template template = cfg.getTemplate("controller_template.ftl");
            template.process(input, writer);
        } catch (IOException | TemplateException e) {
            System.out.println(getClass().getSimpleName() + " error: " + e);
            throw new GafException(e.getMessage());
        }
    }

    private List<Map<String, Object>> generateMethods(Element element) {
        List<Map<String, Object>> methodMapList = new ArrayList<>();
        // handle methods
        final List<? extends Element> methods = element.getEnclosedElements().stream()
                .filter(e -> ElementKind.METHOD.equals(e.getKind()))
                .toList();

        for(Element methodElement: methods) {
            ExecutableElement method = (ExecutableElement) methodElement;
            Map<String, Object> methodMap = new HashMap<>();
            methodMap.put("requestMethod", ElementUtil.getRequestMethod(methodElement));
            methodMap.put("urlValue", getUrl(methodElement.getAnnotation(GafMethod.class), methodElement.getSimpleName().toString()));
            methodMap.put("returnType", processType(method, packages));
            methodMap.put("methodName", method.getSimpleName().toString());
            methodMap.put("controllerParams", processParams(methodElement, false));
            methodMap.put("serviceParams", processParams(methodElement, true));
            methodMapList.add(methodMap);
        }

        return methodMapList;
    }

    private String processParams(Element methodElement, boolean isServiceParam) {
        ExecutableElement method = (ExecutableElement) methodElement;
        List<String> paramStrings = new ArrayList<>();
        for (VariableElement parameter : method.getParameters()) {
            paramStrings.add(processParam(methodElement, parameter, isServiceParam));
        }
        // Join parameter strings with comma
        return String.join(", ", paramStrings);
    }

    private String processParam(Element methodElement, VariableElement variable, boolean isServiceParam) {
        String className = processType(variable.asType(), new StringBuilder(), packages);
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
                    %s%s%s""".formatted(
                            isServiceParam ? "" : "@RequestHeader(value=\"" + headerName + "\") ",
                    isServiceParam ? "" : className + " ",
                    varName);
        } else {
            return """ 
                    %s%s%s""".formatted(
                    generateMethodAnnotation(methodElement, isServiceParam),
                    isServiceParam ? "" : className + " ",
                    varName);
        }
    }

    private static String generateMethodAnnotation(Element methodElement, boolean isServiceParam) {
        if(isServiceParam) {
            return "";
        }
        final String url = getUrl(methodElement.getAnnotation(GafMethod.class), "");
        boolean isPathVariable = url.contains("{") && url.contains("}");
        if(!isPathVariable) {
            return "@RequestBody ";
        } else {
            String pathVariable = url.substring(url.indexOf('{') + 1, url.indexOf('}'));
            return "@PathVariable(\"" + pathVariable + "\")";
        }
    }
}
