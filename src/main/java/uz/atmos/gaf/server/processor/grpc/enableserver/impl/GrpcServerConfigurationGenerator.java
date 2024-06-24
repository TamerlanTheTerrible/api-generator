package uz.atmos.gaf.server.processor.grpc.enableserver.impl;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import uz.atmos.gaf.server.GafServer;
import uz.atmos.gaf.server.processor.grpc.enableserver.EnableGrpcServer;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Created by Temurbek Ismoilov on 10/06/24.
 */

public class GrpcServerConfigurationGenerator {
    private final Set<String> convertedClassSet;
    private final Configuration cfg;
    private final Set<String> packages;

    public GrpcServerConfigurationGenerator() {
        packages = new HashSet<>();
        convertedClassSet = new HashSet<>();
        cfg = new Configuration(Configuration.VERSION_2_3_31);
        cfg.setClassForTemplateLoading(getClass(), "/templates/server/grpc/");
    }

    public void generate(Element element, ProcessingEnvironment processingEnv, EnableGrpcServer gafServerAnnotation) {
        System.out.println("Generating gRPC server configuration for " + element);

        final String serviceClassName = element.getSimpleName().toString();
        String packageName = element.getEnclosingElement().toString();
        String className = serviceClassName + "GrpcImpl";
        String builderFullName = packageName + "." + "GrpcServerConfiguration";
        try (PrintWriter fileWriter = new PrintWriter(processingEnv.getFiler().createSourceFile(builderFullName).openWriter())) {
            //generate input
            Map<String, Object> input = new HashMap<>();
            input.put("packageName", packageName);
            input.put("className", className);
            // process the template
            Template template = cfg.getTemplate("grpc_configuration_template.ftl");
            template.process(input, fileWriter);
        } catch (IOException | TemplateException e) {
            System.err.println("gRPC config generation error: " + e);
            throw new RuntimeException(e);
        }
    }
}
