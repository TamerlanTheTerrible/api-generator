package uz.atmos.gaf.server.processor.grpc.config;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import uz.atmos.gaf.ElementUtil;
import uz.atmos.gaf.server.processor.grpc.scheme.ProtoUtil;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

import static uz.atmos.gaf.ElementUtil.*;
import static uz.atmos.gaf.server.processor.grpc.scheme.ProtoUtil.createWrapperName;
import static uz.atmos.gaf.server.processor.grpc.scheme.ProtoUtil.getProtoName;

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
            input.put("serviceClassName", serviceClassName);
            input.put("className", serviceClassName + "Grpc");
            input.put("implementationClassName", serviceClassName + "GrpcImpl");
            input.put("baseClassName", serviceClassName + "ImplBase");
            input.put("outerClassName", serviceClassName + "OuterClass");
            input.put("methods", generateMethods(element));
            input.put("imports", generateImports(packages));

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
            methodMap.put("returnType", generateReturnType(((ExecutableElement) methodElement).getReturnType()));
            methodMap.put("methodName", method.getSimpleName().toString());
            methodMap.put("params", processParams(methodElement));
            methodMapList.add(methodMap);
        }

        return methodMapList;
    }

    private Map<String, Object> processParams(Element methodElement) {
        Map<String, Object> result = new HashMap<>();
        String protoParamTypeAndName;
        String protoParamName;
        String serviceParamType;

        final List<? extends VariableElement> parameters = ((ExecutableElement) methodElement).getParameters();
        if(parameters.isEmpty()) {
            protoParamTypeAndName = "com.google.protobuf.Empty empty";
            protoParamName = "";
            serviceParamType = "";
        } else {
            final VariableElement variableElement = parameters.get(0);
            final Element element = ((DeclaredType) variableElement.asType()).asElement();
            String className = element.getSimpleName().toString();

            protoParamName = variableElement.getSimpleName().toString();
            protoParamTypeAndName =  getProtoParamTypeName(className) + " " + protoParamName;
            serviceParamType = element.getEnclosingElement() + "." + className;
            // get fields
            if(!variableElement.asType().getKind().isPrimitive() && !ProtoUtil.protoJavaMap.containsKey(className)) {
                final List<String> fields = element.getEnclosedElements().stream()
                        .filter(e -> ElementKind.FIELD.equals(e.getKind()))
                        .map(f -> f.getSimpleName().toString())
                        .toList();
                result.put("fields", fields);
            }
        }

        result.put("protoParamTypeAndName", protoParamTypeAndName);
        result.put("protoParamName", protoParamName);
        result.put("serviceParamType", serviceParamType);

        return result;
    }

    private static String getProtoParamTypeName(String className) {
        String protoTypeName = className;
        if(ProtoUtil.protoJavaMap.containsKey(className)) {
            final String protoName = ProtoUtil.protoJavaMap.get(className);
            protoTypeName = createWrapperName(protoName);
        }
        return protoTypeName;
    }

    private String getProtoTypeName(VariableElement variableElement) {
        String className = ((DeclaredType) variableElement.asType()).asElement().getSimpleName().toString();
        if(ProtoUtil.protoJavaMap.containsKey(className)) {
            final String protoName = ProtoUtil.protoJavaMap.get(className);
            className = createWrapperName(protoName);
        }
        return className;
    }

    private Map<String, String> generateReturnType(TypeMirror returnType) {
        Map<String, String> result = new HashMap<>();
        String serviceReturnType;
        String protoReturnType;
        final TypeKind kind = returnType.getKind();
        if(kind.isPrimitive()) {
            final String primitiveName = ((PrimitiveType) returnType).toString();
            serviceReturnType = primitiveName;
            protoReturnType = createWrapperName(getProtoName(primitiveName));
        } else if(kind == TypeKind.VOID) {
            serviceReturnType = "void";
            protoReturnType = "google.protobuf.Empty";
        } else {
            DeclaredType declaredType = (DeclaredType) returnType;
            Element element = declaredType.asElement();
            String className = element.getSimpleName().toString();
            serviceReturnType = element.getEnclosingElement() + "." + className;
            if(ElementUtil.isCollection(className)) {
                // get collection generic type
                List<? extends TypeMirror> genericParamTypes = declaredType.getTypeArguments();
                if(genericParamTypes.isEmpty()) {
                    className =  "Object";
                    serviceReturnType = serviceReturnType + "<Object>";
                } else {
                    final Element genericElement = ((DeclaredType) genericParamTypes.get(0)).asElement();
                    className = genericElement.getSimpleName().toString();
                    serviceReturnType = serviceReturnType + "<" + genericElement.getEnclosingElement() + "." + className + ">";
                }
                // change classname to proto type, if possible
                if (ProtoUtil.protoJavaMap.containsKey(className)) {
                    className = ProtoUtil.protoJavaMap.get(className);
                }
                protoReturnType = createWrapperName(className + "Array");
            } else {
                //write proto wrapper of this java primitive wrapper class
                if (ProtoUtil.protoJavaMap.containsKey(className)) {
                    String protoName = ProtoUtil.protoJavaMap.get(className);
                    protoReturnType = createWrapperName(protoName);
                } else {
                    protoReturnType = className;
                }
            }
        }
        result.put("serviceReturnType", serviceReturnType);
        result.put("protoReturnType", protoReturnType);
        return result;
    }
}
