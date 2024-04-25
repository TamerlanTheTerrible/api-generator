package uz.atmos.gaf.server.source_generator;

import uz.atmos.gaf.ApiType;
import uz.atmos.gaf.server.source_generator.impl.GrpcApiGenerator;
import uz.atmos.gaf.server.source_generator.impl.RestApiFMTemplateGenerator;
import uz.atmos.gaf.server.source_generator.impl.ThriftApiGenerator;

import java.util.EnumMap;
import java.util.Map;

/**
 * Created by Temurbek Ismoilov on 05/03/24.
 */

public final class ApiGeneratorContainer {
    public static final Map<ApiType, ApiGenerator> container = new EnumMap<>(ApiType.class);

    static {
        container.put(ApiType.GRPC, new GrpcApiGenerator());
        container.put(ApiType.REST, new RestApiFMTemplateGenerator());
        container.put(ApiType.THRIFT, new ThriftApiGenerator());
    }

    public static ApiGenerator get(ApiType type) {
        return container.get(type);
    }
}

