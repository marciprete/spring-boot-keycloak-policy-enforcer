![workflow](https://github.com/marciprete/spring-boot-keycloak-policy-enforcer/actions/workflows/maven.yml/badge.svg)


# Spring Boot - Keycloak policy enforcer configurator 

## Overview

A Spring Boot 3 library that automates the configuration of Keycloak policy enforcement in your applications. 
This library simplifies the integration between Spring Boot applications and Keycloak's authorization services by automatically
generating policy enforcement configurations based on your application's endpoints and Swagger/OpenAPI annotations.

## Why Use This Library?

Traditional Keycloak policy enforcement configuration requires manual definition of paths and scopes in configuration files, which can lead to:
- Lengthy and complex configurations
- Potential duplication of information
- Increased chance of configuration errors
- Maintenance overhead

This library solves these problems by:
- Automatically generating policy enforcement configurations
- Using existing Swagger/OpenAPI annotations and Spring Web annotations
- Providing a single point of configuration
- Reducing manual configuration errors

## Key Features

### 1. Runtime Configuration
- Automatic scanning of @RestController annotated classes
- Parsing of @RequestMapping and related annotations
- Integration with Swagger/OpenAPI security annotations
- Support for multiple HTTP methods and paths

### 2. Keycloak Settings Generator
- Provides a built-in endpoint for generating Keycloak configuration
- Generates JSON settings compatible with Keycloak import
- Customizable export path
- Security features for protecting the export endpoint


## Motivation
When it's used as an **authorization server**, it can be necessary to configure the **policy enforcement** in a configuration file, like this:

```yaml
keycloak:
  enabled: true
  auth-server-url: http://keycloak-host:8080/
  realm: my-realm
  resource: my-client
  credentials:
    secret: my-secret
  lazy-load-paths: true
  enforcement-mode: ENFORCING
  paths:
    - path: /cars/{id}
      methods:
        - method: GET
          scopes:
            - "car:view-detail"
    - path: /car
      methods:
        - method: POST
          scopes:
            - "car:create"
```

Anyway, this approach can lead to a lot of configuration, possible duplications (especially when using api-documentation annotations)
and possibly errors.
  
The aim of this project is to provide an automatic configuration process, based on the Swagger api annotations and on the 
 Spring web annotations, to have one single configuration point. The only **required** configuration is 
```yaml
keycloak:
  enabled: true
  auth-server-url: http://keycloak-host:8080/
  realm: my-realm
  resource: my-client
  credentials:
    secret: my-secret
  lazy-load-paths: true
  enforcement-mode: ENFORCING
```

## Requirements
* Java 17 or higher
* Spring Boot 3
* Keycloak 25.0.1 or higher
* Swagger annotations (v1.5 or v2)
  

## Installation
Just add it as maven dependency:
```xml
<dependency>
  <groupId>it.maconsultingitalia.keycloak</groupId>
  <artifactId>spring-boot-keycloak-policy-enforcer</artifactId>
  <version>1.0.0</version>
</dependency>
```
No other dependencies are required.

## Usage

### Automatic Configuration
Any controller annotated with `@RestController` is scanned from the autoconfigurator, then all its methods are parsed too,
searching for any `@RequestMapping` alias.
The found endpoints are added to the `KeycloakSpringBootProperties`, in the policyEnforcementConfigurations.
Since the bean is lazy loaded, the configurations in the application.properties or application.yml files are kept.
The authorization scopes defined within the `@ApiOperation` or `@Operation` annotations are added too, according to the http verb of the 
annotated method. This means that if the rest controllers are correctly annotated with swagger, no extra configuration is required.

1. Add the `@EnableKeycloakResourcesAutoconfig` to your Spring Boot application or to a specific configuration class.
2. Add a keycloak configuration class with a `FilterRegistrationBean`
```java
@Bean
public FilterRegistrationBean<ServletPolicyEnforcerFilter> keycloakPolicyEnforcerFilter(PolicyEnforcerConfig policyEnforcerConfig) {
    FilterRegistrationBean<ServletPolicyEnforcerFilter> registrationBean = new FilterRegistrationBean<>();
    registrationBean.setFilter(new ServletPolicyEnforcerFilter(httpRequest -> policyEnforcerConfig));
    registrationBean.addUrlPatterns("/*"); 
    registrationBean.setOrder(1); 
    return registrationBean;
}
```
_please note that the policyEnforcerConfig is declared in this library, so it's not necessary to add it manually_

## Examples
##### SimplestRestController
```java
@RestController
public Class SimplestController {

    @GetMapping
    public ResponseEntity<String> getString() { ... }

}
```
In the simplest case the autoconfigurer will create the single endpoint `/` with no extra information.

##### Multiple Mappings Controller
```java
@RestController
@RequestMapping("/foo", "bar")
public Class MultipleMappingController {

    @GetMapping
    public ResponseEntity<String> getString() { ... }

}
```
Regardless of the trailing slash in the `@RequestMapping`, the configurator will add 2 endpoints in the policy enforcement:
`/foo` and `/bar`
The same happens if the method mapping has more than one path.

##### Auth Scope Based Controller (Swagger v3 / Annotations v2)
```java
@RestController
public Class AuthzController {

    @GetMapping
    @Operation(
            summary = "Read my awesome entity",
            operationId = "Entity Getter",
            security = {
                    @SecurityRequirement(
                            name = "get",
                            scopes = "entity:read")
            })
    public ResponseEntity<String> getString() { ... }

}
```
This example will produce the equivalent of the yaml
```yaml
keycloak:
  ...
  policy-enforcer-config:
      enforcement-mode: ENFORCING
      paths:
        - path: /
          methods:
            - method: GET
              scopes:
                - entity:read
```

##### Auth Scope Based Controller (Swagger v2 / Annotations v1.5)
```java
@RestController
public Class AuthzController {

    @GetMapping
    @ApiOperation(
            nickname = "Entity Reader",
            value = "Read my awesome entity",
            authorizations = {
                    @Authorization(
                            value = "get",
                            scopes = {@AuthorizationScope(scope = "entity:read", description = "read entity")})
            })
    public ResponseEntity<String> getString() { ... }

}
```
This example will produce the equivalent of the yaml
```yaml
keycloak:
  ...
  policy-enforcer-config:
      enforcement-mode: ENFORCING
      paths:
        - path: /
          methods:
            - method: GET
              scopes:
                - entity:read
```
> [!WARNING]<br>
> **If you are using nickname**: Be very careful, because keycloak adapter will **only** search for resources 
> with this value as resource name, and it will **skip the search by path**!
> Therefore, if the resource name is not defined in Keycloak, or it is not the same, the permission id will always be null
> and the policy enforcement will DENY the access.

## Keycloak Settings Generator

The `@EnableKeycloakConfigurationExportController` annotation enables an endpoint with a simple Thymeleaf page
that prints on screen the Json Settings.
The service behind this controller uses the keycloak configuration to generate the script that can be imported in 
Keycloak, whose structure is the following:
```json
{
  "allowRemoteResourceManagement": false,
  "policyEnforcementMode": "ENFORCING",
  "decisionStrategy": "AFFIRMATIVE",
  "policies": [],
  "resources": [
    {
      "name": "ResourceName",
      "ownerManagedAccess": false,
      "displayName": "ResourceName",
      "uris": [
        "/path/as/defined/in/controller"
      ],
      "scopes": [
        {
          "name": "resource:operation"
        }
      ]
    }
  ],
  "scopes": [
    {
      "name": "resource:operation"
    }
  ]
}
```
At the moment, the export functions creates a file where the global decision strategy is always `AFFIRMATIVE`,
 and no policies are defined.

All the resources and the Authorization Scopes can be imported from the Keycloak's console.

  * _ResourceName_ is set as defined in the Api (#ApiOperation.nickname or #Operation.operationId).
  * _DisplayName_ is set as defined in the Api (#ApiOperation.value or #Operation.description).
  If the field is not present, the method name will be used in place.
  * OwnerManagedAccess is false by default.

**NOTE**: Existing resources are not added to the export file. That is, if a resource uri is present in the keycloak client, 
it will be skipped

## Configuration 

The library can be configured via `application.yml` or `application.properties`.
Here's the list of the available properties:
```yaml
kcautoconf:
    export-path: /config
    protect-export-path: false
    map-name: true
    export-path-access-scope: configuration:export
```

* `export-path`: the path where the Json Configuration is exported. Default to  `/mac/configuration/export` 

> [!Warning] This endpoint will be available to all the authenticated user. For security reasons, it's strongly recommended to disable
> the Json Configuration export in production.

* `protect-export-path`: whether to apply policy enforcement. (`boolean`, default to `false`)
* `export-path-access-scope`: the authorization scope to be assigned to this resource.  (`String`, default to `configuration:export`, only meaningful when `protect-export-path` is set to `true`)


## Known limitations
At the moment, the endpoints are added only if the methods are mapped with `@GetMapping`, `@PostMapping`, `@PutMapping` etc.
If the method is annotated via `@RequestMapping`, then the http verb is not inferred thus the endpoint is not added. 
