<#assign packageName = packageName>
<#assign imports = imports>
<#assign className = className>
<#assign apiName = apiName>
<#assign serviceClassName = serviceClassName>
package ${packageName};

import feign.Headers;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

${imports}

@RestController
@RequestMapping(value = "${baseUrl?lower_case}")
public class ${apiName} {

    private final ${serviceClassName} ${serviceClassName?lower_case};

    public ${apiName}(${serviceClassName} ${serviceClassName?lower_case}) {
        this.${serviceClassName?lower_case} = ${serviceClassName?lower_case};
    }
<#list methods as method>
    <#assign requestMethod = method.requestMethod>
    <#assign methodName = method.methodName>
    <#assign urlValue = method.urlValue>
    <#assign returnType = method.returnType>
    <#assign controllerParams = method.controllerParams>
    <#assign serviceParams = method.serviceParams>

    @${requestMethod}Mapping("${urlValue}")
    ${returnType} ${methodName}(${controllerParams}) {
        return ${serviceClassName?lower_case}.${methodName}(${serviceParams});
    }
</#list>
}
