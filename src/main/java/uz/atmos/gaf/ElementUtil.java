package uz.atmos.gaf;

import org.springframework.web.bind.annotation.RequestMethod;
import uz.atmos.gaf.client.GafClient;
import uz.atmos.gaf.client.GafMethod;
import uz.atmos.gaf.exception.GafException;
import uz.atmos.gaf.server.GafServer;

import javax.lang.model.element.*;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by Temurbek Ismoilov on 13/03/24.
 */

public class ElementUtil {

    public static boolean isCollection(String fieldType) {
        return Stream.of("List", "ArrayList", "Set", "HashSet", "Collection").anyMatch(fieldType::contains);
    }


    public static String getRequestMethod(Element methodElement) {
        GafMethod gafMethodAnnotation = methodElement.getAnnotation(GafMethod.class);
        if (gafMethodAnnotation == null) {
            return "Post";
        }

        final RequestMethod requestMethod = gafMethodAnnotation.method();
        return switch (requestMethod) {
            case POST -> "Post";
            case GET -> "Get";
            case DELETE -> "Delete";
            case PATCH -> "Patch";
            case HEAD -> "Head";
            case PUT -> "Put";
            case OPTIONS -> "Options";
            case TRACE -> "Trace";
            default -> "Post";
        };
    }

    public static String processType(ExecutableElement method, Set<String> packages) {
        return processType(method.getReturnType(), new StringBuilder(), packages);

    }

    public static String processType(TypeMirror type, StringBuilder sb, Set<String> packages) {
        // append type name
        final TypeKind kind = type.getKind();
        String className = getClassName(type, packages);
        sb.append(className);
        // if type is declared continue processing
        if (!kind.isPrimitive() && kind != TypeKind.VOID) {
            DeclaredType declaredType = (DeclaredType) type;
            if(!declaredType.getTypeArguments().isEmpty()) {
                final List<? extends TypeMirror> typeArguments = declaredType.getTypeArguments();
                sb.append("<");
                for(int i=0; i<typeArguments.size(); i++) {
                    sb.append(processType(typeArguments.get(i), new StringBuilder(), packages));
                    if(i<typeArguments.size() - 1) {
                        sb.append(", ");
                    }
                }
                sb.append(">");
            }
        }
        return sb.toString();
    }

    public static String getClassName(TypeMirror returnType, Set<String> packages) {
        final TypeKind kind = returnType.getKind();
        if (kind.isPrimitive()) {
            return returnType.toString();
        } else if(kind == TypeKind.VOID) {
            return "void";
        } else {
            DeclaredType declaredType = (DeclaredType) returnType;
            final String className = declaredType.asElement().getSimpleName().toString();
            addToPackageSet(returnType.toString(), className, packages);
            return className;
        }
    }

    public static String generateImports(Set<String> packages) {
        System.out.println("PACKAGES: " + packages);
        StringBuilder sb = new StringBuilder();
        for (String pack : packages) {
            sb.append("import ").append(pack).append(";\n");
        }
        return sb.toString();
    }

    public static String getUrl(ExecutableElement methodElement) {
        GafMethod gafMethodAnnotation = methodElement.getAnnotation(GafMethod.class);
        String url = null;
        if (gafMethodAnnotation == null) {
            url = "/" + methodElement.getSimpleName().toString();
        } else {
            String urlValue = gafMethodAnnotation.url();
            url = urlValue != null ? urlValue : "/" + methodElement.getSimpleName().toString();
        }

        List<? extends VariableElement> parameters = methodElement.getParameters();
        validatePathVariables(url, parameters);
        return url;
    }

    private static void validatePathVariables(String url, List<? extends VariableElement> parameters) {
        //filter parameters
        final List<String> pathVariables = parameters.stream()
                .filter(p -> p.getAnnotation(PathVariable.class) != null)
                .map(p -> p.getSimpleName().toString())
                .toList();

        if(!pathVariables.isEmpty()) {
            Set<String> paramNames = getParamNames(url);
            //make sure all params are in the url
            for (String pathVariable: pathVariables) {
                if (!paramNames.contains(pathVariable)) {
                    System.err.println("Unknown request parameter: " + pathVariable);
                    throw new GafException("Unknown request parameter: " + pathVariable);
                }
                paramNames.remove(pathVariable);
            }
            //make sure if all url variables are in the method arguments
            if(!paramNames.isEmpty()) {
                final String errorMsg = "Request parameters not found : " + Arrays.toString(paramNames.toArray());
                System.err.println(errorMsg);
                throw new GafException(errorMsg);
            }
        }
    }

    public static Set<String> getParamNames(String urlValue) {
        return Arrays.stream(urlValue.split("/"))
                .filter(p -> p.startsWith("{") && p.endsWith("}"))
                .map(p -> p.substring(1, p.indexOf("}")))
                .collect(Collectors.toSet());
    }

    public static String getUrl(GafMethod gafMethodAnnotation, String defaultUrl) {
        if (gafMethodAnnotation == null) {
            return "/" + defaultUrl;
        }

        final String urlValue = gafMethodAnnotation.url();
        return urlValue != null ? urlValue : "/" + defaultUrl;
    }

    public static String getUrl(GafClient gafMethodAnnotation, String defaultUrl) {
        if (gafMethodAnnotation == null) {
            return "/" + defaultUrl;
        }

        final String urlValue = gafMethodAnnotation.url();
        return urlValue != null ? urlValue : "/" + defaultUrl;
    }

    public static String getUrl(GafServer gafMethodAnnotation, String defaultUrl) {
        if (gafMethodAnnotation == null) {
            return "/" + defaultUrl;
        }

        final String urlValue = gafMethodAnnotation.url();
        return urlValue != null ? urlValue : "/" + defaultUrl;
    }

    public static void addToPackageSet(String fullClassName, String className, Set<String> packages) {
        if (fullClassName.contains(".")) {
            if (fullClassName.contains("<")) {
                fullClassName = fullClassName.substring(0, fullClassName.indexOf("<"));
            }
            final String packageName = fullClassName.substring(0, fullClassName.lastIndexOf("."));
            packages.add(packageName + "." + className);
        }
    }
}
