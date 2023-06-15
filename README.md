# Adhoc Web

Web code for Adhoc (final name to be decided).

[https://github.com/SpeculativeCoder/adhoc-web](https://github.com/SpeculativeCoder/adhoc-web)

[https://github.com/SpeculativeCoder/AdhocPlugin](https://github.com/SpeculativeCoder/AdhocPlugin)

[https://github.com/SpeculativeCoder/AdhocDocumentation](https://github.com/SpeculativeCoder/AdhocDocumentation) (not yet available)

This is a work in progress, experimental, and subject to major changes.

The eventual ideal/goal of Adhoc is to be a system for running a multi-user, multi-server 3D world in the cloud (e.g. AWS) using Unreal Engine with the HTML5 ES3 (WebGL2) platform plugin.

Live Example: [**AdhocCombat** (https://adhoccombat.com)](https://adhoccombat.com) - work in progress

## Usage

Out of the box, the application in this repository will run in a development mode without the Unreal / cloud server functionality.

To build the application:

`mvn clean install -DskipTests`

To run the application you can run:

`mvn spring-boot:run -f adhoc-manager`

or alternatively you can manually run `AdhocManagerApplication` in your IDE.

If you go to http://localhost you should see the application running in development mode.

Further functionality requires setup. See: [https://github.com/SpeculativeCoder/AdhocDocumentation](https://github.com/SpeculativeCoder/AdhocDocumentation) (not yet available)

## Copyright / License(s)

Copyright (c) 2022-2023 SpeculativeCoder (https://github.com/SpeculativeCoder)

[LICENSE](LICENSE) (**MIT License**) applies to the files in this repository unless otherwise indicated.

There are currently some files under a different license (indicated in the file and with license provided in adjacent *-LICENSE file):
- [src/main/resources/db/changelog/db.changelog-2.xml](src/main/resources/db/changelog/db.changelog-2.xml) uses SQL from Quartz Scheduler to set up the Quartz database tables - subject to **[Apache License, Version 2.0](src/main/resources/db/changelog/db.changelog-2.xml-LICENSE)**
- [src/main/java/adhoc/web/logging/AdhocColorConverter.java](src/main/java/adhoc/web/logging/AdhocColorConverter.java) is a modified version of the Spring Boot ColorConverter - subject to **[Apache License, Version 2.0](src/main/java/adhoc/web/logging/AdhocColorConverter.java-LICENSE)**
