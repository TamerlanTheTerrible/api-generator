package ${packageName};

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import java.io.IOException;
import io.grpc.stub.StreamObserver;
import uz.atmos.gaf.${outerClassName}.*;
import uz.atmos.gaf.${className};
${imports}
@Configuration
public class GrpcServerConfig {

    private final ${serviceClassName} ${serviceClassName?lower_case};

    public GrpcServerConfig(BillingConfigService billingConfigService) {
        this.billingConfigService = billingConfigService;
    }

    @Bean
    public Server startGRPC(){
        System.out.println("gRPC server start attempt");

        final int port = 9999;
        Server server = ServerBuilder
                .forPort(port)
                .addService(new ${implementationClassName}())
                .build();

        try {
            server.start();
        } catch (IOException e) {
            System.err.println("gRPC server started on port " + port);
            throw new RuntimeException(e);
        }

        System.out.println("gRPC server started on port " + port);
        return server;
    }

    static class ${implementationClassName} extends ${className}.${baseClassName}{
        <#list methods as method>
            <#assign methodName = method.methodName>
            <#assign returnType = method.returnType>
            <#assign protoParamTypeAndName = method.params.protoParamTypeAndName>
            <#assign protoParamName = method.params.protoParamName>

        @Override
        public void ${methodName}(<#if protoParamName?has_content>${protoParamTypeAndName}, <#else>com.google.protobuf.Empty request, </#if> StreamObserver<${returnType}> responseObserver) {
            ${returnType} ${returnType?lower_case} = ${returnType}.newBuilder().build();
            responseObserver.onNext(${returnType?lower_case});
            responseObserver.onCompleted();
        }
        </#list>
    }
    // TODO add to the method
    try {
        org.example.billingconfig.dto.BillingConfigCreateDto billingConfigCreateDto =  new org.example.billingconfig.dto.BillingConfigCreateDto();
        billingConfigCreateDto.setUrl(dto.getUrl());
        org.example.billingconfig.dto.BillingConfigDto billingConfigDto = billingConfigService.create(billingConfigCreateDto);

        BillingConfigDto billingconfigdto = BillingConfigDto.newBuilder()
        .setUrl(billingConfigDto.getUrl())
        .build();

        responseObserver.onNext(billingconfigdto);
    } catch (Exception e) {

    }
    responseObserver.onCompleted();
}
