package uz.atmos.gaf.server.impl;

import uz.atmos.gaf.server.ApiGenerator;

import javax.annotation.processing.Filer;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.WildcardType;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import java.io.PrintWriter;
import java.util.*;

/**
 * Created by Temurbek Ismoilov on 05/03/24.
 */

public class GrpcApiGenerator implements ApiGenerator {

    private final Map<String, String> map;

    public GrpcApiGenerator() {
        this.map = new HashMap<>();
        map.put("String", "string");
        map.put("Integer", "int32");
        map.put("int", "int32");
        map.put("Long", "int64");
        map.put("long", "int64");
        map.put("Double", "double");
        map.put("double", "double");
        map.put("Float", "float");
        map.put("float", "float");
        map.put("Boolean", "bool");
        map.put("boolean", "bool");
        map.put("ByteString", "bytes");
    }

    @Override
    public void generate(Element element, ProcessingEnvironment processingEnv) {
        try {
            System.out.println("Invoking " + this.getClass().getSimpleName() + " for " + element);
            // Get the Filer from the ProcessingEnvironment
            Filer filer = processingEnv.getFiler();

            String className = element.getSimpleName().toString().replace("Service", "");
            // Create a new resource file (here, we'll create a properties file)
            FileObject resourceFile = filer.createResource(
                    StandardLocation.CLASS_OUTPUT,
                    element.getEnclosingElement().toString(), // No package for this example
                    className + "proto.proto", // Name of the resource file
                    element // Associated elements
            );

            // Write content to the resource file
            Set<Class<?>> convertedClassSet = new HashSet<>();
            try (PrintWriter writer = new PrintWriter(resourceFile.openWriter())) {
                writer.print("""
                        syntax = "proto3";
                        package com.proto;
                        option java_multiple_files = true;

                        """);

                // fields
                final List<? extends Element> enclosedElements = element.getEnclosedElements();
                // handle methods
                final List<? extends Element> methods = enclosedElements.stream()
                        .filter(e -> ElementKind.METHOD.equals(e.getKind()))
                        .toList();

                for(Element methodElement: methods) {
                    ExecutableElement method = (ExecutableElement) methodElement;
                    TypeMirror returnType = ((ExecutableElement) methodElement).getReturnType();
                    System.out.println("<<<>>> Return Type: " + returnType);
                    // Process return type (if it's a class)
                    if (returnType.getKind() == TypeKind.DECLARED) {
                        // Get the return type's element
                        Element returnTypeElement = ((DeclaredType) returnType).asElement();

                        // Process fields of the return type (if it's a class)
                        if (returnTypeElement.getKind() == ElementKind.CLASS) {
                            // Filter fields
                            List<? extends Element> fields = returnTypeElement.getEnclosedElements()
                                    .stream()
                                    .filter(e -> e.getKind() == ElementKind.FIELD)
                                    .toList();

                            // Process each field
                            for (Element field : fields) {
                                String fieldTypeName = field.asType().toString();
                                String fieldName = field.getSimpleName().toString();
                                System.out.println("Field Type: " + fieldTypeName);
                                System.out.println("Field Name: " + fieldName);

                                // Further processing of the field...
                                //TODO
                            }
                        } else {
                           //TODO
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("ERROR: " + e.getMessage());
            e.printStackTrace(); // Handle or log the exception as needed
        }
    }
}
