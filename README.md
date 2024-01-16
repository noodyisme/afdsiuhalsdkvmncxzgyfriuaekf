# Identity Builder Policy Core · [![Build Status](https://digitechjenkins.cloud.capitalone.com/buildStatus/icon?job=Bogie/identitybuilder/identity-builder-policy-core/main)](https://digitechjenkins.cloud.capitalone.com/job/Bogie/job/identitybuilder/job/identity-builder-policy-core/job/main/) [![Coverage](https://sonar.cloud.capitalone.com/api/project_badges/measure?project=com.capitalone.identity.identitybuilder%3Aidentity-builder-policy-core&metric=coverage&token=8ce9d614581e29ca204359f28ee2989a4f503760)](https://sonar.cloud.capitalone.com/dashboard?id=com.capitalone.identity.identitybuilder%3Aidentity-builder-policy-core)

JAR dependency to standardize Journey and Domain servers

### [Check out the **wiki** for a guide on about how to use the Policy Core to setup a Domain](https://github.cloud.capitalone.com/identitybuilder/identity-builder-policy-core/wiki)

### Dependencies
- [Chassis](https://github.cloud.capitalone.com/chassis-framework/chassis-spring-boot-starters)
  - [Spring Boot](https://github.com/spring-projects/spring-boot)
- [Apache Camel](https://github.com/apache/camel)
- [Config Store Client](https://github.cloud.capitalone.com/identitybuilder/identity-builder-config-store-client)
- [Decision Engine](https://github.cloud.capitalone.com/identitybuilder/identity-builder-decision-engine)

Get the latest version of **identity-builder-policy-core** from [Artifactory](https://artifactory.cloud.capitalone.com/artifactory/webapp/#/search/quick/eyJzZWFyY2giOiJxdWljayIsInF1ZXJ5IjoiaWRlbnRpdHktYnVpbGRlci1wb2xpY3ktY29yZSJ9).

### Properties
| Feature | Value |
| --- | --- |
| name | `identitybuilder.policycore.feature.strict-policy-start-mode` |
| type | optional
| format | boolean
| default | `false`
| description | It's recommended to set this property to `true` only in non-prod environments.  If `true`, all policies must load without error for the service to start successfully. If false, service will start normally even if there are policy load errors.

| Feature | Value |
| --- | --- |
| name | `identitybuilder.policycore.feature.prerelease.enabled` |
| type | optional
| format | boolean
| default | `false`
| description | If `false`, then internal spring-injected classes and methods marked `@PreRelease` will result in errors. It's recommended to set this property to `true` in non-prod environments only.

| Feature | Value |
| --- | --- |
| name | `identitybuilder.policycore.feature.config-management.enabled` |
| type | optional
| format | boolean
| default | `false`
| description | If `true` services can consume policies with `policy-configuration` components.

| Feature | Value |
| --- | --- |
| name | `identitybuilder.policycore.decisionengine.healthCheck.enabled` |
| type | optional
| format | boolean
| default | `true`
| description | If `true`, then policy core will run a periodic scheduled health check on the decision engine. Setting to `false` will disable this health check. This health check involves loading and evaluation DMNs and could have a performance impact depending on load and configuration.

| Feature | Value |
| --- | --- |
| name | `identitybuilder.policycore.decisionengine.loadFixedThreadPoolSize` |
| type | optional
| format | integer
| default | `10`
| description | The number of threads allocated for DMN loading in the decision engine. Increase this value if DMN loading is a performance bottleneck.

| Feature | Value |
| --- | --- |
| name | `identitybuilder.policycore.decisionengine.evalFixedThreadPoolSize` |
| type | optional
| format | integer
| default | `20`
| description | The number of threads allocated for DMN evaluation in the decision engine. Increase this value if DMN evaluation is a performance bottleneck.

| Feature | Value |
| --- | --- |
| name | `identitybuilder.policycore.feature.decisionengine.output-audit-logger.enabled` |
| type | optional
| format | boolean
| default | `true`
| description | Set to `false` to disable audit logs from event code `IdentityBuilder-MasterBuilder.RulesAudit` at service-level.

| Feature | Value |
| --- | --- |
| name | `identitybuilder.policycore.feature.mock-mode.enabled` |
| type | optional
| format | boolean
| default | `false`
| description | This property **must not** be set to `true` prod environments. If `true`, policies are allowed to run in Mock Mode using `policy-mode:non-prod/policy-core.mockMode` component.

| Feature | Value |
| --- | --- |
| name | `identitybuilder.policycore.feature.mock-mode.url` |
| type | optional
| format | string
| default | `env.mimeoURL`
| description | If `identitybuilder.policycore.feature.mock-mode.enabled` is `true`, this value will replace the target hostname for any calls made by policies running in Mock Mode. If this value is not set, the property value for `env.mimeoURL` is used. If neither this nor `env.mimeoURL` is set, then Mock Mode executions will result in errors.
# afdsiuhalsdkvmncxzgyfriuaekf
