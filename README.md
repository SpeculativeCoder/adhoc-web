# Adhoc Web

Web code for Adhoc (final name to be decided).

https://github.com/SpeculativeCoder/adhoc-web

This is a work in progress.

The eventual ideal/goal of Adhoc is to be a system for running a multi-user, multi-server 3D world in the cloud (e.g. AWS) using Unreal Engine with the HTML5 (WebGL) platform plugin.

Full functionality of this repository depends upon the Adhoc Unreal project code and content **which is not in this repository and is not yet available**. Also, the full build process and AWS setup is **not yet documented / included in this repository**. Thus, the Unreal functionality and cloud server management won't currently be usable out of the box.

However, elements of the code may still be of interest and could be useful if diagnosing bugs / issues with a deployed Adhoc web application.

## Example

[AdhocCombat (https://adhoccombat.com)](https://adhoccombat.com) - work in progress

## Usage

Running the application in its current state will only show a development mode application without the Unreal / cloud server functionality.

You can either run:

`mvn spring-boot:run`

which builds and runs the app, or alternatively:

`mvn clean package -DskipTests`

to build it then manually run `AdhocApplication` in your IDE.

If you go to http://localhost you should see the application running.

## Copyright / License(s)

Copyright (c) 2022-2023 SpeculativeCoder (https://github.com/SpeculativeCoder)

[LICENSE](LICENSE) (**MIT License**) applies to all files in this repository unless otherwise indicated.

There are currently some files under a different license (indicated in the file and with license provided in adjacent *-LICENSE file):
- [src/main/resources/db/changelog/db.changelog-2.xml](src/main/resources/db/changelog/db.changelog-2.xml) uses SQL from Quartz Scheduler to set up the Quartz database tables - subject to **[Apache License, Version 2.0](src/main/resources/db/changelog/db.changelog-2.xml-LICENSE)**
- [src/main/java/adhoc/web/logging/AdhocColorConverter.java](src/main/java/adhoc/web/logging/AdhocColorConverter.java) is a modified version of the Spring Boot ColorConverter - subject to **[Apache License, Version 2.0](src/main/java/adhoc/web/logging/AdhocColorConverter.java-LICENSE)**
