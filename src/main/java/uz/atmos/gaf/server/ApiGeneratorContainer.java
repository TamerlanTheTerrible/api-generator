package uz.atmos.gaf.server;

import uz.atmos.gaf.ApiType;
import uz.atmos.gaf.server.impl.RestApiGenerator;
import uz.atmos.gaf.server.impl.ThriftApiGenerator;

import java.util.EnumMap;
import java.util.Map;

/**
 * Created by Temurbek Ismoilov on 05/03/24.
 */

public final class ApiGeneratorContainer {
    public static final Map<ApiType, ApiGenerator> container = new EnumMap<>(ApiType.class);

    static {
        container.put(ApiType.GRPC, new RestApiGenerator());
        container.put(ApiType.REST, new RestApiGenerator());
        container.put(ApiType.THRIFT, new ThriftApiGenerator());
    }

    public static ApiGenerator get(ApiType type) {
        return container.get(type);
    }
}
