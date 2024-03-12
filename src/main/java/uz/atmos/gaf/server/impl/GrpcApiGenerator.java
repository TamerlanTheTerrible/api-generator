package uz.atmos.gaf.server.impl;

import uz.atmos.gaf.exception.GafException;
import uz.atmos.gaf.server.ApiGenerator;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;
import java.util.stream.Stream;

/**
 * Created by Temurbek Ismoilov on 05/03/24.
 */

public class GrpcApiGenerator implements ApiGenerator {

    private final Map<String, String> map;
    private final Set<String> convertedClassSet;

    public GrpcApiGenerator() {
        this.convertedClassSet = new HashSet<>();
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
        map.put("byte", "byte");
        map.put("Byte", "byte");
    }

    @Override
    public void generate(Element element, ProcessingEnvironment processingEnv) {
        final String serviceName = element.getSimpleName().toString();
        try {
            System.out.println("Invoking " + this.getClass().getSimpleName() + " for " + serviceName);
            // Create a new resource file (here, we'll create a properties file)
            FileObject resourceFile = processingEnv.getFiler().createResource(
                    StandardLocation.CLASS_OUTPUT,
                    element.getEnclosingElement().toString(), // No package for this example
                    serviceName.replace("Service", "") + ".proto", // Name of the resource file
                    element // Associated elements
            );

            // Write content to the resource file
            generate(element, resourceFile);
        } catch (Exception e) {
            System.err.println("Error while processing " + serviceName + " : " + e.getMessage());
        }
    }

    private void generate(Element element, FileObject resourceFile) {
        try (PrintWriter writer = new PrintWriter(resourceFile.openWriter())) {
            writer.print("""
                syntax = "proto3";
                package com.proto;
                option java_multiple_files = true;
                
                import "google/protobuf/any.proto";
                import "google/protobuf/empty.proto";

                service Proto%s {
                """.formatted(element.getSimpleName().toString()));

            // handle methods
            final List<? extends Element> methods = element.getEnclosedElements().stream()
                    .filter(e -> ElementKind.METHOD.equals(e.getKind()))
                    .toList();

            List<String> messages = new ArrayList<>();
            for(Element methodElement: methods) {
                //process return type
                TypeMirror returnType = ((ExecutableElement) methodElement).getReturnType();
                String returnTypeName = getReturnTypeName(returnType, messages);
                messages.add(generateMessage(returnType));
                //process params
                List<? extends VariableElement> parameters = ((ExecutableElement) methodElement).getParameters();
                String paramString = getParamString(parameters, messages);
                parameters.forEach(ve -> generateMessage(ve, new StringBuilder()));
                //write down methods
                writer.print("""
                          rpc %s(%s) returns (%s){};
                        """.formatted(
                        methodElement.getSimpleName(),
                        paramString,
                        returnTypeName
                ));
            }
            writer.write("}\n");
            messages.forEach(writer::write);
        } catch (IOException e) {
            System.err.println(e.getMessage());
            throw new RuntimeException(e);
        }

    }

    private String getParamString(List<? extends VariableElement> parameters, List<String> messages) {
        if (parameters.isEmpty()) {
            return "google.protobuf.Empty";
        }
        Element element = ((DeclaredType) parameters.get(0).asType()).asElement();
        String className = element.getSimpleName().toString();
        if(map.containsKey(className)) {
            final String protoName = map.get(className);
            String wrapperName = createWrapperName(protoName);
            addToTheMessageList(messages, wrapperName, protoName + " " + protoName.toLowerCase());
            return wrapperName;
        } else {
            return className;
        }
    }

    private String getReturnTypeName(TypeMirror returnType, List<String> messages) {
        final TypeKind kind = returnType.getKind();
        if(kind.isPrimitive()) {
            //write proto wrapper of this primitive
            String protoName = getProtoName(((PrimitiveType) returnType).toString());
            String wrapperName = createWrapperName(protoName);
            addToTheMessageList(messages, wrapperName, protoName + " " + protoName.toLowerCase());
            return wrapperName;
        } else if(kind == TypeKind.VOID) {
            return "google.protobuf.Empty";
        } else {
            DeclaredType declaredType = (DeclaredType) returnType;
            String className = declaredType.asElement().getSimpleName().toString();
            if(isCollection(className)) {
                //write proto wrapper for collection
                List<? extends TypeMirror> genericParamTypes = declaredType.getTypeArguments();
                className = genericParamTypes.isEmpty() ? "Object" : ((DeclaredType) genericParamTypes.get(0)).asElement().getSimpleName().toString();
                System.out.println("HERE: " + className);
                if (map.containsKey(className)) {
                    className = map.get(className);
                }
                String wrapperName = createWrapperName(className + "Array");
                addToTheMessageList(messages, wrapperName, "repeated " + className + " " + className.toLowerCase());
                return wrapperName;
            } else {
                //write proto wrapper of this java primitive wrapper class
                if (map.containsKey(className)) {
                    String protoName = map.get(className);
                    String wrapperName = createWrapperName(protoName);
                    addToTheMessageList(messages, wrapperName, protoName + " " + protoName.toLowerCase());
                    return wrapperName;
                } else {
                    return className;
                }
            }
        }
    }

    private static String createWrapperName(String protoName) {
        return Character.toUpperCase(protoName.charAt(0)) + protoName.substring(1) + "Wrapper";
    }

    private void addToTheMessageList(List<String> messages, String messageName, String messageContent) {
        //add wrapped message to the proto file, if it is not added already
        if (!convertedClassSet.contains(messageName)) {
            convertedClassSet.add(messageName);
            String message = """
                
                message %s {
                 %s = 1;
                }
                """ .formatted(messageName, messageContent);
            messages.add(message);
        }
    }

    private String generateMessage(TypeMirror typeMirror) {
        StringBuilder sb = new StringBuilder();
        // Process return type (if it's a class)
        if (typeMirror.getKind() == TypeKind.DECLARED ) {
            if (((DeclaredType) typeMirror).asElement().getKind() == ElementKind.CLASS) {
                generateMessage(((DeclaredType) typeMirror).asElement(), sb);
            } else if (((DeclaredType) typeMirror).asElement().getKind() == ElementKind.ENUM) {
                generateEnum(((DeclaredType) typeMirror).asElement(), sb);
            }
        } else {

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
        sb.append("\n").append("message ").append(element.getSimpleName()).append(" {\n");

        // Process each field
        for (int i=0; i<fields.size(); i++) {
            //Get variables for log
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
            } else if (fieldKind == TypeKind.VOID) {
                continue;
            } else {
                processPrimitive(sb, field, i);
            }
        }
        sb.append("}");
        nestedMessages.forEach(m -> sb.append("\n").append(m));
    }

    private void generateEnum(Element element, StringBuilder sb) {
        // filter elements
        List<? extends Element> fields = element.getEnclosedElements()
                .stream()
                .filter(e -> e.getKind() == ElementKind.ENUM_CONSTANT)
                .toList();

        sb.append("\nenum ").append(element.getSimpleName()).append(" {\n");
        for(int i=0; i<fields.size(); i++) {
            sb.append(" ")
                    .append(fields.get(i))
                    .append(" = ")
                    .append(i)
                    .append(";\n");
        }
        sb.append("}\n");
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
            if(declaredType != null && !convertedClassSet.contains(fieldType)) {
                //add to th set so that next time not to process it
                convertedClassSet.add(fieldType);
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
