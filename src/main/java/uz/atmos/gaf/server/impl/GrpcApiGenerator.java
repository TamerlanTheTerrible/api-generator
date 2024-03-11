package uz.atmos.gaf.server.impl;

import uz.atmos.gaf.exception.GafException;
import uz.atmos.gaf.server.ApiGenerator;

import javax.annotation.processing.Filer;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.swing.*;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Stream;

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
        map.put("Object", "Any");
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
                    className + ".proto", // Name of the resource file
                    element // Associated elements
            );

            // Write content to the resource file
            Set<Class<?>> convertedClassSet = new HashSet<>();
            try (PrintWriter writer = new PrintWriter(resourceFile.openWriter())) {
                writer.print("""
                        syntax = "proto3";
                        package com.proto;
                        option java_multiple_files = true;
                        
                        import "google/protobuf/any.proto";

                        """);

                // fields
                final List<? extends Element> enclosedElements = element.getEnclosedElements();
                // handle methods
                final List<? extends Element> methods = enclosedElements.stream()
                        .filter(e -> ElementKind.METHOD.equals(e.getKind()))
                        .toList();

                for(Element methodElement: methods) {
                    generateService((ExecutableElement) methodElement, writer);
                }
            }
        } catch (Exception e) {
            System.out.println("ERROR: " + e.getMessage());
            e.printStackTrace(); // Handle or log the exception as needed
        }
    }

    private void generateService(ExecutableElement methodElement, PrintWriter writer) {
        TypeMirror returnType = methodElement.getReturnType();
        String returnTypeMessage = generateMessage(returnType);
        writer.write(returnTypeMessage);
    }

    private String generateMessage(TypeMirror typeMirror) {
        StringBuilder sb = new StringBuilder();
        // Process return type (if it's a class)
        if (typeMirror.getKind() == TypeKind.DECLARED && ((DeclaredType) typeMirror).asElement().getKind() == ElementKind.CLASS) {
            // Get the return type's element
            Element returnTypeElement = ((DeclaredType) typeMirror).asElement();
            generateMessage(returnTypeElement, sb);
        } else {
            //TODO
        }

        return sb.toString();
    }

    private void generateMessage(Element element, StringBuilder sb) {
        List<String> nestedMessages = new ArrayList<>();
        // Filter fields
        List<? extends Element> fields = element.getEnclosedElements()
                .stream()
                .filter(e -> e.getKind() == ElementKind.FIELD)
                .toList();
        System.out.println("Message name: " + element.getSimpleName());

        // write message name
        sb.append("message ").append(element.getSimpleName()).append(" {\n");

        // Process each field
        for (int i=0; i<fields.size(); i++) {
            Element field = fields.get(i);
            String fieldType = field.asType().toString();
            String fieldName = field.getSimpleName().toString();
            TypeKind fieldKind = field.asType().getKind();
            System.out.println("Message field type: " + fieldType + ", name: " + fieldName + ", kind: " + fieldKind);

            // Process class type
            if(fieldKind == TypeKind.DECLARED) {
                processReferenceType(sb, field, i, nestedMessages);
            } else if (fieldKind == TypeKind.ARRAY) {
                processArray(sb, field, i);
            } else {
                processPrimitive(sb, field, i);
            }
        }
        sb.append("\n}");
        nestedMessages.forEach(m -> sb.append("\n").append(m));
    }

    private void processPrimitive(StringBuilder sb, Element element, int i) {
        final String protoName = getProtoName(element.asType().toString());
        appendFieldRecord(sb, protoName, element.getSimpleName().toString(), i);
    }

    private void processArray(StringBuilder sb, Element element, int i) {
        String fieldType = element.asType().toString();
        String fieldName = element.getSimpleName().toString();
        final String protoName = getProtoName(fieldType.replace("[", "").replace("]", ""));
        sb.append(" ").append("repeated");
        appendFieldRecord(sb, protoName, fieldName, i);
    }

    private void processReferenceType(StringBuilder sb, Element field, int i, List<String> nestedMessages) {
        String fieldType = field.asType().toString();
        String fieldName = field.getSimpleName().toString();
        // identify class type, get generic param if the class is collection
        DeclaredType declaredType = (DeclaredType) field.asType();
        if(isCollection(fieldType)) {
            List<? extends TypeMirror> genericParamTypes = declaredType.getTypeArguments();
            declaredType = genericParamTypes.isEmpty() ? null : ((DeclaredType) genericParamTypes.get(0));
            sb.append(" ").append("repeated");
        }
        // get proto name
        String simpleClassName = declaredType == null ? "Object" : declaredType.asElement().getSimpleName().toString();
        String protoName = map.get(simpleClassName);
        // Write field if the type has appropriate proto mapping,
        if(protoName != null) {
            appendFieldRecord(sb, protoName, fieldName, i);
        } else {
            // else the type is declared class by user, so write the field and call the method generateMessage() recursively
            appendFieldRecord(sb, simpleClassName, fieldName, i);
            if(declaredType != null) {
                nestedMessages.add(generateMessage(declaredType));
            }
        }
    }

    private static boolean isCollection(String fieldType) {
        return Stream.of("List", "ArrayList", "Set", "HashSet", "Collection").anyMatch(fieldType::contains);
    }

    private String getProtoName(String fieldType) {
        String protoName = map.get(fieldType);
        if (protoName == null) {
            throw new GafException("Could not map java type: " + fieldType);
        }
        return protoName;
    }

    private static void appendFieldRecord(StringBuilder sb, String protoName, String fieldName, int i) {
        sb.append(" ").append(protoName).append(" ").append(fieldName).append(" = ").append(i + 1).append(";").append("\n");
    }
}
