package uz.atmos.gaf.client.processor;

import uz.atmos.gaf.ApiType;
import uz.atmos.gaf.client.processor.grpc.GrpcClientProcessor;
import uz.atmos.gaf.client.processor.rest.RestClientProcessor;
import uz.atmos.gaf.client.processor.thrift.ThriftClientProcessor;

import java.util.EnumMap;
import java.util.Map;

/**
 * Created by Temurbek Ismoilov on 05/03/24.
 */

public final class ClientProcessorContainer {
    public static final Map<ApiType, ClientProcessor> container = new EnumMap<>(ApiType.class);

    static {
        container.put(ApiType.GRPC, new GrpcClientProcessor());
        container.put(ApiType.REST, new RestClientProcessor());
        container.put(ApiType.THRIFT, new ThriftClientProcessor());
    }

    public static ClientProcessor get(ApiType type) {
        return container.get(type);
    }
}

