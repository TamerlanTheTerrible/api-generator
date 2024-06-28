<#assign serviceClassVarName = serviceClassName?lower_case>

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

    private final ${serviceClassName} ${serviceClassVarName};

    public GrpcServerConfig(${serviceClassName} ${serviceClassVarName}) {
        this.${serviceClassVarName} = ${serviceClassVarName} ;
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
            <#assign serviceReturnType = method.returnType.serviceReturnType>
            <#assign protoReturnType = method.returnType.protoReturnType>
            <#assign protoParamTypeAndName = method.params.protoParamTypeAndName>
            <#assign protoParamName = method.params.protoParamName>
            <#assign fields = method.params.fields>

        @Override
        public void ${methodName}(${protoParamTypeAndName}, StreamObserver<${protoReturnType}> responseObserver) {
            try {
                ${serviceReturnType} serviceParam = new ${serviceReturnType}();
                <#list fields as field>
                    <#assign capCaseField = field?cap_first>
<#--                    <#assign getterField = protoParamName.getcapCaseField()>-->
                    serviceParam.set${capCaseField}(${protoParamName}.get${capCaseField}());
                </#list>

                ${protoReturnType} protoResponse = ${protoReturnType}.newBuilder().build();
                responseObserver.onNext(protoResponse);
                responseObserver.onCompleted();
            } catch (Exception e) {

            }
        }
        </#list>
    }

<#--    try {-->
<#--        org.example.billingconfig.dto.BillingConfigCreateDto billingConfigCreateDto =  new org.example.billingconfig.dto.BillingConfigCreateDto();-->
<#--        billingConfigCreateDto.setUrl(dto.getUrl());-->
<#--        org.example.billingconfig.dto.BillingConfigDto billingConfigDto = billingConfigService.create(billingConfigCreateDto);-->

<#--        BillingConfigDto billingconfigdto = BillingConfigDto.newBuilder()-->
<#--        .setUrl(billingConfigDto.getUrl())-->
<#--        .build();-->

<#--        responseObserver.onNext(billingconfigdto);-->
<#--    } catch (Exception e) {-->

<#--    }-->
<#--    responseObserver.onCompleted();-->
}
