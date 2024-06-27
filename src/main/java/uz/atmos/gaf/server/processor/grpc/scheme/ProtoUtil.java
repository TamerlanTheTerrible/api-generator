package uz.atmos.gaf.server.processor.grpc.scheme;

import uz.atmos.gaf.exception.GafException;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Temurbek Ismoilov on 26/06/24.
 */

public class ProtoUtil {
    public final static Map<String, String> protoJavaMap = new HashMap<>();

    static {
        protoJavaMap.put("String", "string");
        protoJavaMap.put("char", "string");
        protoJavaMap.put("Char", "string");
        protoJavaMap.put("Integer", "int32");
        protoJavaMap.put("int", "int32");
        protoJavaMap.put("Long", "int64");
        protoJavaMap.put("long", "int64");
        protoJavaMap.put("Double", "double");
        protoJavaMap.put("double", "double");
        protoJavaMap.put("Float", "float");
        protoJavaMap.put("float", "float");
        protoJavaMap.put("Boolean", "bool");
        protoJavaMap.put("boolean", "bool");
        protoJavaMap.put("ByteString", "bytes");
        protoJavaMap.put("Object", "google.protobuf.Any");
        protoJavaMap.put("byte", "byte");
        protoJavaMap.put("Byte", "byte");
        protoJavaMap.put("T", "google.protobuf.Any");
        protoJavaMap.put("Map", "map<string, string>");
    }

    public static String createWrapperName(String protoName) {
        return Character.toUpperCase(protoName.charAt(0)) + protoName.substring(1) + "Wrapper";
    }

    public static String getProtoName(String fieldType) {
        String protoName = ProtoUtil.protoJavaMap.get(fieldType);
        if (protoName == null) {
            throw new GafException("Could not map java type: " + fieldType);
        }
        return protoName;
    }

}
