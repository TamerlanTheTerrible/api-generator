package uz.atmos.gaf.client.source_generator;

import uz.atmos.gaf.ApiType;
import uz.atmos.gaf.client.source_generator.impl.GrpcClientGenerator;
import uz.atmos.gaf.client.source_generator.impl.RestClientFMTemplateGenerator;
import uz.atmos.gaf.client.source_generator.impl.ThriftClientGenerator;

import java.util.EnumMap;
import java.util.Map;

/**
 * Created by Temurbek Ismoilov on 05/03/24.
 */

public final class ClientGeneratorContainer {
    public static final Map<ApiType, ClientGenerator> container = new EnumMap<>(ApiType.class);

    static {
        container.put(ApiType.GRPC, new GrpcClientGenerator());
        container.put(ApiType.REST, new RestClientFMTemplateGenerator());
        container.put(ApiType.THRIFT, new ThriftClientGenerator());
    }

    public static ClientGenerator get(ApiType type) {
        return container.get(type);
    }
}

