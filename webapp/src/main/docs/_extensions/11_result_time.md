---
layout: section
title: Result Time Extension
---

### Result Times

Next to the actual phenomenon time observation data may have a `resultTime` which 
indicates when the data became available. An example for such cases is forecast data
which may be (re-)calculated multiple times (optionally based on multiple models). 
Getting the right data values (belonging to a specific result time) a client can add 
the `resultTime=...` parameter when querying `datasets/<id>/data`.

In case of existing result times (which have to be different to the actual phenomenon 
time) are available to a client as `extra` data for a given dataset. 

Once activated no specific configuration is neccessary.

#### Enable Extension

{:.n52-callout .n52-callout-todo}
add description and output examples

{::options parse_block_html="true" /}
{: .n52-example-block}
<div>
<div class="btn n52-example-caption n52-example-toggler active" type="button" data-toggle="button">
Enable extension on `DatasetController`
</div>
```xml
<bean class="org.n52.web.ctrl.DatasetController" parent="parameterController">
    <property name="parameterService" ref="datasetService" />
    <property name="metadataExtensions">
        <list merge="true">
            <bean class="org.n52.io.extension.resulttime.ResultTimeExtension">
                <property name="service" ref="resultTimeService" />
            </bean>
        </list>
    </property>
</bean>
```
</div>
