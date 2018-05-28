# Contentpackage-validation-maven-plugin

Using this package you can validate restrictions on content packages. At the moment it supports 2 different restrictions

* checking for the existence of certain files inside packages and subpackages
* check for the existence of subpackages

Additionally to a pure reporting of collisions with these rules you can also break the build.

## Standalone mode

```
mvn de.joerghoh.maven:contentpackage-validation-maven-plugin:validate -Dvalidation.filename=<path_to_the_contentpackage> "-Dvalidation.filteredPaths=.*\.jar"  -Dvalidation.breakBuildOnValiationFailures=true
```

This is probably the most often used mode; it allows you to scan a content package in the filesystem for all files with the extension ".jar" (full regular expression support!). You can also check for multiple expressions at once:

```
"-Dvalidation.filteredPaths=.*\.jar,.*\.conf"
```

## Running as part of the build

tbc










