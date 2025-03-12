## 2.5.0 - Unreleased

## 2.4.0 - Released (Sunflower R1 2025)
The focus of this release was to update dependencies

[Full Changelog](https://github.com/folio-org/mod-orders/compare/v2.3.0...v2.4.0)

### Stories
* [FOLIO-4203](https://folio-org.atlassian.net/browse/FOLIO-4203) - Update to mod-ebsconet Java 21

### Dependencies
* Bump `raml` from `35.3.0` to `35.4.0`
* Bump `java` from `17` to `21`


## 2.3.0 - Released (Ramsons R2 2024)
The focus of this release was to update dependencies

[Full Changelog](https://github.com/folio-org/mod-orders/compare/v2.2.0...v2.3.0)

### Stories
* [MODEBSNET-83](https://folio-org.atlassian.net/browse/MODEBSNET-83) - Update libraries of dependant acq modules to the latest versions

### Dependencies
* Bump `raml` from `35.2.0` to `35.3.0`
* Added `folio-module-descriptor-validator` version `1.0.0`


## 2.2.0 - Released (Quesnelia R1 2024)
The primary focus of this release was error handling improvements and spring version upgrade

[Full Changelog](https://github.com/folio-org/mod-ebsconet/compare/v2.1.0...v2.2.0)

### Stories
* [MODEBSNET-73] (https://folio-org.atlassian.net/browse/MODEBSNET-73) - mod-ebsconet: spring upgrade
* [MODEBSNET-70] (https://folio-org.atlassian.net/browse/MODEBSNET-70) - Add error code if default note type "General note" is not exists

### Dependencies
* Bump `spring` from `3.1.4` to `3.2.3`

## 2.1.0 - Released (Poppy R2 2023)
The focus of this release was to update dependencies and remove unnecessary code

[Full Changelog](https://github.com/folio-org/mod-orders/compare/v2.0.0...v2.1.0)

### Stories
* [MODEBSNET-65](https://issues.folio.org/browse/MODEBSNET-65) spring boot upgrade
* [MODEBSNET-64](https://issues.folio.org/browse/MODEBSNET-64) Folio Spring base updated
* [MODEBSNET-62](https://issues.folio.org/browse/MODEBSNET-62) Delete unused Kafka and database references
* [FOLIO-3678](https://issues.folio.org/browse/FOLIO-3678) Use GitHub Workflows api-lint and api-schema-lint and api-doc
* [MODEBSNET-59](https://issues.folio.org/browse/MODEBSNET-59) Migrate to folio-spring-support v7.0.0
* [MODEBSNET-58](https://issues.folio.org/browse/MODEBSNET-58) Delete beginFolioExecutionContext from TestBase

### Dependencies
* Bump `spring boot` from `3.0.2` to `3.1.4`
* Bump `folio spring base` from `6.0.0` to `7.2.0`

## 2.0.0 - Released (Orchid R1 2023)
The focus of this release was to update to Java 17 and Spring boot to 3.0.2

[Full Changelog](https://github.com/folio-org/mod-orders/compare/v1.4.0...v2.0.0)

### Stories
* [MODEBSNET-52](https://issues.folio.org/browse/MODEBSNET-52) Update the module to Spring boot v3.0.0 and identify issues.
* [MODEBSNET-51](https://issues.folio.org/browse/MODEBSNET-51) Update to Java 17
* [MODEBSNET-50](https://issues.folio.org/browse/MODEBSNET-50) Logging improvement - Configuration
* [MODEBSNET-28](https://issues.folio.org/browse/MODEBSNET-28) Logging improvement

### Bug Fixes
* [MODEBSNET-53](https://issues.folio.org/browse/MODEBSNET-53) Customer test showing InternalServerError

## 1.4.0 Nolana R3 2022
The focus of this release was to RMB upgrade and interface version update

[Full Changelog](https://github.com/folio-org/mod-orders/compare/v1.3.0...v1.4.0)

### Stories
* [MODEBSNET-47](https://issues.folio.org/browse/MODEBSNET-47) Spring base upgrade to 5.0.1
* [MODEBSNET-40](https://issues.folio.org/browse/MODEBSNET-40) mod-notes interface version update

## 1.3.0 Morning Glory R2 2022 - Released
This release contains update logic to support customer note, renwal note, cancelling POL(s) through EBSCONET integration

[Full Changelog](https://github.com/folio-org/mod-orders/compare/v1.2.0...v1.3.0)

### Stories
* [MODEBSNET-38](https://issues.folio.org/browse/MODEBSNET-38) Remove org.json:json, license is not open source
* [MODEBSNET-35](https://issues.folio.org/browse/MODEBSNET-35) Mod-ebsconet: spring update
* [MODEBSNET-33](https://issues.folio.org/browse/MODEBSNET-33) Update logic for supporting "customer note" from EBSCONET(Part 2)
* [MODEBSNET-30](https://issues.folio.org/browse/MODEBSNET-30) Update logic for supporting "Renewal note" from EBSCONET
* [MODEBSNET-27](https://issues.folio.org/browse/MODEBSNET-27) Update logic for supporting "customer note" from EBSCONET
* [MODEBSNET-22](https://issues.folio.org/browse/MODEBSNET-22) Support canceling POL(s) through EBSCONET integration
* [MODEBSNET-13](https://issues.folio.org/browse/MODEBSNET-13) Update logic for supporting fund expense classes

## 1.2.0 - Released
This release contains schema updates in order to support mod-orders interfaces

[Full Changelog](https://github.com/folio-org/mod-orders/compare/v1.1.0...v1.2.0)

### Stories
* [MODORDSTOR-267](https://issues.folio.org/browse/MODORDSTOR-267) Rename collection field name for Acquisition method collection
* [MODORDSTOR-266](https://issues.folio.org/browse/MODORDSTOR-266) added automaticExport to poline schemes
* [MODORDSTOR-256](https://issues.folio.org/browse/MODORDSTOR-256) Major API versions have been increased


## 1.1.0 - Released
The primary focus of this release to update core dependencies

[Full Changelog](https://github.com/folio-org/mod-orders/compare/v1.0.0...v1.1.0)

### Stories
* [MODEBSNET-20](https://issues.folio.org/browse/MODEBSNET-20) folio-spring-base v2 update

### Bug Fixes
* [MODEBSNET-18](https://issues.folio.org/browse/MODEBSNET-18) Allow EBSCONET to update order with multiple locations
* [MODEBSNET-10](https://issues.folio.org/browse/MODEBSNET-10) Handling mixed orders for ebsconet

 
## 1.0.0 - Released
The primary focus of this release to create module and implement basic logic interact with mod-orders 

### Stories
* [MODEBSNET-8](https://issues.folio.org/browse/MODEBSNET-8) Update postgresql to 42.2.18 or spring-boot-starter-parent to 2.3.5.RELEASE
* [MODEBSNET-7](https://issues.folio.org/browse/MODEBSNET-7) Add /ebsconet/validate endpoint configuration
* [MODEBSNET-4](https://issues.folio.org/browse/MODEBSNET-4) Implement update ebsconet order line logic 
* [MODEBSNET-3](https://issues.folio.org/browse/MODEBSNET-3) Implement retrieve ebsconet order line logic 
* [MODEBSNET-1](https://issues.folio.org/browse/MODEBSNET-1) Project setup in GitHub: mod-ebsconet 

### Bug Fixes
* [MODEBSNET-9](https://issues.folio.org/browse/MODEBSNET-9) Attempts to renew through EBSCONET are failing in Rancher env

