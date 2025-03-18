# Adhoc Web

Web code for Adhoc (final name to be decided).

[https://github.com/SpeculativeCoder/adhoc-web](https://github.com/SpeculativeCoder/adhoc-web)

[https://github.com/SpeculativeCoder/AdhocPlugin](https://github.com/SpeculativeCoder/AdhocPlugin)

[https://github.com/SpeculativeCoder/AdhocDocumentation](https://github.com/SpeculativeCoder/AdhocDocumentation)

This is a work in progress, experimental, and subject to major changes.

The eventual ideal/goal of Adhoc is to be a system for running a multi-user, multi-server 3D world in the cloud (e.g. AWS) using Unreal Engine with the HTML5 ES3 (WebGL2) platform plugin.

Live Example: [**AdhocCombat** (https://adhoccombat.com)](https://adhoccombat.com) - work in progress

## Usage

Out of the box, the application in this repository will run in a development mode without the Unreal / cloud server functionality.

### Run in IDE

To build the application (at least once to make sure the Angular app is built):

`mvn clean package -DskipTests`

You can then run this Spring Boot application class in your IDE: `AdhocManagerApplication`

If you go to http://localhost you should see the application running in development mode.

Further functionality requires setup. See: [https://github.com/SpeculativeCoder/AdhocDocumentation](https://github.com/SpeculativeCoder/AdhocDocumentation)

### Run from command line

`mvn clean install -DskipTests`

`mvn spring-boot:run -f adhoc-manager`

## Copyright / License(s)

Copyright (c) 2022-2025 SpeculativeCoder (https://github.com/SpeculativeCoder)

[LICENSE](LICENSE) (**MIT License**) applies to the files in this repository unless otherwise indicated.

There are currently some files under a different license (indicated in the file and with license provided in adjacent *.LICENSE file):

- [00-03-changelog-quartz.xml](adhoc-core/src/main/resources/db/changelog/0000/00/00-03-changelog-quartz.xml) uses SQL from Quartz Scheduler which is subject to **[Apache License, Version 2.0](adhoc-core/src/main/resources/db/changelog/0000/00/00-03-changelog-quartz.xml.LICENSE)**
- [00-04-changelog-spring-session.xml](adhoc-core/src/main/resources/db/changelog/0000/00/00-04-changelog-spring-session.xml) uses SQL from Spring Session which is subject to **[Apache License, Version 2.0](adhoc-core/src/main/resources/db/changelog/0000/00/00-04-changelog-spring-session.xml.LICENSE)**
- [AdhocColorLogbackConverter](adhoc-core/src/main/java/adhoc/system/logback/AdhocColorLogbackConverter.java) is a modified version of the Spring Boot ColorConverter - subject to **[Apache License, Version 2.0](adhoc-core/src/main/java/adhoc/system/logback/AdhocColorLogbackConverter.java.LICENSE)**
