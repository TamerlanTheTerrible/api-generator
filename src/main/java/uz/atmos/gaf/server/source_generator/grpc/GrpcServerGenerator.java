package uz.atmos.gaf.server.source_generator.grpc;

import freemarker.template.Configuration;
import freemarker.template.Template;
import uz.atmos.gaf.ElementUtil;
import uz.atmos.gaf.exception.GafException;
import uz.atmos.gaf.server.GafServer;
import uz.atmos.gaf.server.source_generator.ApiGenerator;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.tools.StandardLocation;
import java.io.PrintWriter;
import java.util.*;

/**
 * Created by Temurbek Ismoilov on 05/06/24.
 */

public class GrpcServerGenerator implements ApiGenerator {
    private final Map<String, String> map;
    private final Set<String> convertedClassSet;
    private final Configuration cfg;

    public GrpcServerGenerator() {
        this.convertedClassSet = new HashSet<>();
        // Java-Proto map
        this.map = new HashMap<>();
        map.put("String", "string");
        map.put("char", "string");
        map.put("Char", "string");
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
        map.put("Object", "google.protobuf.Any");
        map.put("byte", "byte");
        map.put("Byte", "byte");
        map.put("T", "google.protobuf.Any");
        map.put("Map", "map<string, string>");
        // template config
        cfg = new Configuration(Configuration.VERSION_2_3_31);
        cfg.setClassForTemplateLoading(getClass(), "/templates/server/grpc/");
    }

    @Override
    public void generate(Element element, ProcessingEnvironment processingEnv, GafServer gafServerAnnotation) {
        System.out.println("Generating grpc template for " + element);

        final String serviceName = element.getSimpleName().toString();

        try (PrintWriter writer = new PrintWriter(
                processingEnv.getFiler().createResource(
                        StandardLocation.CLASS_OUTPUT,
                        element.getEnclosingElement().toString(), // No package for this example
                        serviceName + ".proto", // Name of the resource file
                        element).openWriter())
        ) {
            Map<String, Object> input = new HashMap<>();
            input.put("serviceName", element.getSimpleName().toString());
            final Map<String, Object> map = generateMethod(element, gafServerAnnotation);
            input.put("methods", map.get("methods"));
            input.put("messages", map.get("messages"));

            Template template = cfg.getTemplate("grpc_template.ftl");
            template.process(input, writer);
        } catch (Exception e) {
            System.err.println("Error while processing " + serviceName + " : " + e.getMessage());
        }
    }

    private Map<String, Object> generateMethod(Element element, GafServer gafServerAnnotation) {
        System.out.println("Generating GRPC methods");

        List<String> messages = new ArrayList<>();
        List<Map<String, String>> methodList = new ArrayList<>();

        final List<? extends Element> methods = element.getEnclosedElements().stream()
                .filter(e -> ElementKind.METHOD.equals(e.getKind()))
                .toList();
        for(Element methodElement: methods) {
            Map<String, String> methodMap = new HashMap<>();
            //process return type
            TypeMirror returnType = ((ExecutableElement) methodElement).getReturnType();
            String returnTypeName = generateReturnType(returnType, messages);
            generateMessage(returnType, messages);
            //process params
            List<? extends VariableElement> parameters = ((ExecutableElement) methodElement).getParameters();
            System.out.println("PARAMS: " + Arrays.toString(parameters.stream().map(ve -> ve.asType().toString()).toArray()));
            String paramString = getParamString(parameters, messages);
            parameters.forEach(ve -> generateMessage(ve.asType(), messages));
            //full method parts the map
            methodMap.put("name", methodElement.getSimpleName().toString());
            methodMap.put("paramString", paramString);
            methodMap.put("returnTypeName", returnTypeName);
            methodList.add(methodMap);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("methods", methodList);
        result.put("messages", messages);

        return result;
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

    private String generateReturnType(TypeMirror returnType, List<String> messages) {
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
            if(ElementUtil.isCollection(className)) {
                // get collection generic type
                List<? extends TypeMirror> genericParamTypes = declaredType.getTypeArguments();
                className = genericParamTypes.isEmpty() ? "Object" : ((DeclaredType) genericParamTypes.get(0)).asElement().getSimpleName().toString();
                // change classname to proto type, if possible
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

    private String generateMessage(TypeMirror typeMirror, List<String> messages) {
        StringBuilder sb = new StringBuilder();

        // Process return type (if it's a class)
        if (typeMirror.getKind() == TypeKind.DECLARED ) {
            final Element element = ((DeclaredType) typeMirror).asElement();
            //check if the element is primitive or the primitive's wrapper
            if (map.containsKey(element.getSimpleName().toString())) {
                final String protoName = map.get(element.getSimpleName().toString());
                String wrapperName = createWrapperName(protoName);
                addToTheMessageList(messages, wrapperName, protoName + " " + protoName.toLowerCase());
                return wrapperName;
            }

            if (element.getKind() == ElementKind.CLASS) {
                return generateMessage(element, sb, messages);
            } else if (element.getKind() == ElementKind.ENUM) {
                return  generateEnum(element, sb, messages);
            }
        } else {
            //write proto wrapper of this primitive
            String protoName = getProtoName(((PrimitiveType) typeMirror).toString());
            String wrapperName = createWrapperName(protoName);
            addToTheMessageList(messages, wrapperName, protoName + " " + protoName.toLowerCase());
            return wrapperName;
        }

        return null;
    }

    private String generateMessage(Element element, StringBuilder sb, List<String> messages) {
        final String messageName = element.getSimpleName().toString();
        if(convertedClassSet.contains(messageName)) {
            return "";
        }
        // write message name
        sb.append("\n").append("message ").append(messageName).append(" {\n");

        List<String> nestedMessages = new ArrayList<>();
        // Filter fields
        List<? extends Element> fields = element.getEnclosedElements()
                .stream()
                .filter(e -> e.getKind() == ElementKind.FIELD)
                .toList();

        // Process each field
        for (int i=0; i<fields.size(); i++) {
            //Get variables for log
            Element field = fields.get(i);
            TypeKind fieldKind = field.asType().getKind();
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
        nestedMessages.forEach(sb::append);

        //add to message list
        final String message = sb.toString();
        messages.add(message);
        convertedClassSet.add(messageName);

        return message;
    }

    private String generateEnum(Element element, StringBuilder sb, List<String> messages) {
        final String messageName = element.getSimpleName().toString();
        if(convertedClassSet.contains(messageName)) {
            return "";
        }
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

        //add to message list
        final String message = sb.toString();
        messages.add(message);
        convertedClassSet.add(messageName);

        return message;
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
        if(ElementUtil.isCollection(fieldType)) {
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
                generateMessage(declaredType, nestedMessages);
            }
        }
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
