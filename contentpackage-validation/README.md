# Contentpackage-validation-maven-plugin

Using this package you can validate restrictions on content packages. At the moment it supports 2 different restrictions

* checking for the existence of certain files inside packages and subpackages
* check for the existence of subpackages

Additionally to a pure reporting of collisions with these rules you can also break the build.

## Standalone mode

provide the name of the content package via the "validation.filename" system property.

```
mvn de.joerghoh.maven:contentpackage-validation-maven-plugin:validate -Dvalidation.filename=<path_to_the_contentpackage> "-Dvalidation.filteredPaths=.*\.jar"  -Dvalidation.breakBuildOnValiationFailures=true
```

This is probably the most often used mode; it allows you to scan a content package in the filesystem for all files with the extension ".jar" (full regular expression support!). You can also check for multiple expressions at once:

```
"-Dvalidation.filteredPaths=.*\.jar,.*\.conf"
```

## Running as part of the build
The plugin can run as part of a maven build; in case any restriction triggers, it will be reported, but the build does not break. If the configuration setting "breakBuild" is set, the build will fail.

Example how to configure:

```
<plugin>
  <groupId>de.joerghoh.maven</groupId>
  <artifactId>contentpackage-validation-maven-plugin</artifactId>
  <version>1.0-SNAPSHOT</version>
  <configuration>
    <breakBuild>true</breakBuild>
    <allowSubpackages>true</allowSubpackages>
    <filteredPaths>
      <param>jcr_root/libs/.*</param>
      <param>jcr_root/content/.*</param>
    </filteredPaths>
  </configuration>

</plugin>
```
This configuration will disallow content for /libs and /content, allow sub packages and break the build if a violation is detected.


## Supported restrictions
At the moment the plugin supports these restrictions

* path restrictions (system property: validation.filteredPaths, maven configuration: filteredPaths): A multivalue list of regular expressions, which denote files which should be blocked.
* subpackage restrictions (system property: validation.allowSubpackages,  maven configuration: allowSubpackages): either "true" or "false"; if "false" any subpackage will be reported.


## Dumping the content of a package

If you just want to dump the content of a package (including subpackages), use the "dump" goal:

```
mvn de.joerghoh.maven:contentpackage-validation-maven-plugin:dump -Dvalidation.filename=<path_to_the_contentpackage> 
```









