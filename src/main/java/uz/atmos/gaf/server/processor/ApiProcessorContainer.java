package uz.atmos.gaf.server.processor;

import uz.atmos.gaf.ApiType;
import uz.atmos.gaf.server.processor.grpc.scheme.GrpcServerProcessor;
import uz.atmos.gaf.server.processor.rest.RestServerProcessor;
import uz.atmos.gaf.server.processor.thrift.ThriftApiProcessor;

import java.util.EnumMap;
import java.util.Map;

/**
 * Created by Temurbek Ismoilov on 05/03/24.
 */

public final class ApiProcessorContainer {
    public static final Map<ApiType, ApiProcessor> container = new EnumMap<>(ApiType.class);

    static {
        container.put(ApiType.GRPC, new GrpcServerProcessor());
        container.put(ApiType.REST, new RestServerProcessor());
        container.put(ApiType.THRIFT, new ThriftApiProcessor());
    }

    public static ApiProcessor get(ApiType type) {
        return container.get(type);
    }
}

