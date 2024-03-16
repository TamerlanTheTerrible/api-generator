package uz.atmos.gaf.client.source_generator;

import uz.atmos.gaf.ApiType;
import uz.atmos.gaf.client.source_generator.impl.GrpcClientGeneratorGenerator;
import uz.atmos.gaf.client.source_generator.impl.RestClientGeneratorGenerator;
import uz.atmos.gaf.client.source_generator.impl.ThriftClientGeneratorGenerator;

import java.util.EnumMap;
import java.util.Map;

/**
 * Created by Temurbek Ismoilov on 05/03/24.
 */

public final class ClientGeneratorContainer {
    public static final Map<ApiType, ClientGenerator> container = new EnumMap<>(ApiType.class);

    static {
        container.put(ApiType.GRPC, new GrpcClientGeneratorGenerator());
        container.put(ApiType.REST, new RestClientGeneratorGenerator());
        container.put(ApiType.THRIFT, new ThriftClientGeneratorGenerator());
    }

    public static ClientGenerator get(ApiType type) {
        return container.get(type);
    }
}

