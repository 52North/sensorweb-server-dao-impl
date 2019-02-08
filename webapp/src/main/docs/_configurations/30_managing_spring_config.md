---
layout: section
title: Manage Spring Configuration
---

### Managing Configuration

Spring configuration is done via mixing Java based configuration (for static configuration which is not 
changed) and classic xml files laying under `WEB-INF/spring/` directory which can be adjusted as needed. 
This section gives some notes how Spring configuration is being managed.

#### Separate Properties
It is helpful to separate all properties values from XML configuration for several
reasons. First, it may be tedious to find all single properties within verbose XML. 
However, it's more important to keep sensitive information (like database
configuration) from the project itself.

Properties are read from `WEB-INF/classes/application.properties` at startup. One can override properties
by passing `-Dlocal.configFile=<absolute-file-location>` when building with Maven. In case of an already 
built Web application, one have to adjust two files to add the customized properties file: 
`WEB-INF/spring/dispatcher-servlet.xml` and `WEB-INF/classes/logback.xml`. 

{::options parse_block_html="true" /}
{: .n52-example-block}
<div>
<div class="btn n52-example-caption n52-example-toggler active" type="button" data-toggle="button">
A property replacement (properties from last location will be used)
</div>
```xml
  <!-- local.configFile overrides defaults from application.properties -->
  <ctx:property-placeholder location="classpath:/application.properties,file://${local.configFile}"
      ignore-resource-not-found="true" ignore-unresolvable="false" />
``` 
</div>

A placeholder can now be declared within Spring XML files via `${placeholder:default}`.
If present in the application properties file (your one or the default) it will be 
replaced, otherwise the given default will be used.


#### Separate Configuration Sections

To keep overview we can separate parts of the configuration files and include them
via file import, e.g. `<import resource="mvc.xml" />`.

