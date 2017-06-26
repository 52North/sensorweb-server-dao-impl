---
layout: section
title: Hierarchical Parameters Extension
permalink: /extensions/hierarchical_parameters
---

### Hierarchical Parameters

{:.n52-callout .n52-callout-todo}
add description and output examples

#### Enable Extension

{::options parse_block_html="true" /}
{: .n52-example-block}
<div>
<div class="btn n52-example-caption n52-example-toggler active" type="button" data-toggle="button">
Enable extension on `PlatformsParameterController`
</div>
```xml
<bean class="org.n52.web.ctrl.PlatformsParameterController" parent="parameterController">
    <property name="parameterService" ref="platformParameterService" />
    <property name="metadataExtensions">
        <list merge="true">
            <bean class="org.n52.io.extension.parents.HierarchicalParameterExtension">
                <property name="service" ref="hierarchicalParameterService" />
            </bean>
        </list>
    </property>
</bean>
```
</div>