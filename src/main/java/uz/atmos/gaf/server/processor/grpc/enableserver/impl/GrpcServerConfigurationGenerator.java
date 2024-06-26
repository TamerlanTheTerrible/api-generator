package uz.atmos.gaf.server.processor.grpc.enableserver.impl;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import uz.atmos.gaf.server.processor.grpc.enableserver.EnableGrpcServer;

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
 * Created by Temurbek Ismoilov on 10/06/24.
 */

public class GrpcServerConfigurationGenerator {
    private final Configuration cfg;
    private final Set<String> packages;

    public GrpcServerConfigurationGenerator() {
        packages = new HashSet<>();
        cfg = new Configuration(Configuration.VERSION_2_3_31);
        cfg.setClassForTemplateLoading(getClass(), "/templates/server/grpc/");
    }

    public void generate(Element element, ProcessingEnvironment processingEnv) {
        System.out.println("Generating gRPC server configuration for " + element);

        final String serviceClassName = element.getSimpleName().toString();
        String packageName = element.getEnclosingElement().toString();
        String builderFullName = packageName + "." + "GrpcServerConfig";
        try (PrintWriter fileWriter = new PrintWriter(processingEnv.getFiler().createSourceFile(builderFullName).openWriter())) {
            //generate input
            Map<String, Object> input = new HashMap<>();
            input.put("packageName", packageName);
            input.put("imports", generateImports(packages));
            input.put("className", serviceClassName + "Grpc");
            input.put("implementationClassName", serviceClassName + "GrpcImpl");
            input.put("baseClassName", serviceClassName + "ImplBase");
            input.put("methods", generateMethods(element));
            // process the template
            Template template = cfg.getTemplate("grpc_configuration_template.ftl");
            template.process(input, fileWriter);
        } catch (IOException | TemplateException e) {
            System.err.println("gRPC config generation error: " + e);
            throw new RuntimeException(e);
        }
    }

    private List<Map<String, Object>> generateMethods(Element element) {
        List<Map<String, Object>> methodMapList = new ArrayList<>();
        // handle methods
        final List<? extends Element> methods = element.getEnclosedElements().stream()
                .filter(e -> ElementKind.METHOD.equals(e.getKind()))
                .toList();

        for (Element methodElement : methods) {
            ExecutableElement method = (ExecutableElement) methodElement;
            Map<String, Object> methodMap = new HashMap<>();
            methodMap.put("returnType", processType(method, packages));
            methodMap.put("methodName", method.getSimpleName().toString());
            methodMap.put("paramTypesAndNames", getParamTypesAndNames(methodElement));
            methodMap.put("paramNames", getParamNames(methodElement));
            methodMapList.add(methodMap);
        }

        return methodMapList;
    }

    private String getParamTypesAndNames(Element methodElement) {
        ExecutableElement method = (ExecutableElement) methodElement;
        List<String> paramStrings = new ArrayList<>();
        for (VariableElement parameter : method.getParameters()) {
            String className = processType(parameter.asType(), new StringBuilder(), packages);
            String varName = parameter.getSimpleName().toString();
            paramStrings.add(className + " " + varName);
        }
        // Join parameter strings with comma
        return String.join(", ", paramStrings);
    }

    private String getParamNames(Element methodElement) {
        ExecutableElement method = (ExecutableElement) methodElement;
        List<String> paramStrings = new ArrayList<>();
        for (VariableElement parameter : method.getParameters()) {
            String varName = parameter.getSimpleName().toString();
            paramStrings.add(varName);
        }
        // Join parameter strings with comma
        return String.join(", ", paramStrings);
    }
}
