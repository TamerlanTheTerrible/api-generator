syntax = "proto3";

option java_package = "uz.atmos.gaf";

import "google/protobuf/any.proto";
import "google/protobuf/empty.proto";

service ${serviceName} {
<#list methods as method>
    rpc ${method.name} (${method.paramString}) returns (${method.returnTypeName}) {};
</#list>
}
<#list messages as message>
    ${message}
</#list>
