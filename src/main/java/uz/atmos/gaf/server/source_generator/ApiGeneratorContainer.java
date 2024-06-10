package uz.atmos.gaf.server.source_generator;

import uz.atmos.gaf.ApiType;
import uz.atmos.gaf.server.source_generator.grpc.GrpcServerGenerator;
import uz.atmos.gaf.server.source_generator.rest.RestServerGenerator;
import uz.atmos.gaf.server.source_generator.thrift.ThriftApiGenerator;

import java.util.EnumMap;
import java.util.Map;

/**
 * Created by Temurbek Ismoilov on 05/03/24.
 */

public final class ApiGeneratorContainer {
    public static final Map<ApiType, ApiGenerator> container = new EnumMap<>(ApiType.class);

    static {
        container.put(ApiType.GRPC, new GrpcServerGenerator());
        container.put(ApiType.REST, new RestServerGenerator());
        container.put(ApiType.THRIFT, new ThriftApiGenerator());
    }

    public static ApiGenerator get(ApiType type) {
        return container.get(type);
    }
}

