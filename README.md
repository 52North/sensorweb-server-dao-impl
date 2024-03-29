# DAO Implementation of 52n Sensor Web Server Helgoland
[![Build Status](https://travis-ci.org/52North/sensorweb-server-dao-impl.svg)](https://travis-ci.org/52North/sensorweb-server-dao-impl) [![Java CI](https://github.com/52North/sensorweb-server-dao-impl/actions/workflows/maven.yml/badge.svg)](https://github.com/52North/sensorweb-server-dao-impl/actions/workflows/maven.yml) [![Maven Central](https://img.shields.io/maven-central/v/org.n52.sensorweb-server.dao-impl/dao-impl-parent.svg)](https://search.maven.org/search?q=g:org.n52.sensorweb-server.dao-impl) [![Total alerts](https://img.shields.io/lgtm/alerts/g/52North/sensorweb-server-dao-impl.svg?logo=lgtm&logoWidth=18)](https://lgtm.com/projects/g/52North/sensorweb-server-dao-impl/alerts/) [![Language grade: JavaScript](https://img.shields.io/lgtm/grade/javascript/g/52North/sensorweb-server-dao-impl.svg?logo=lgtm&logoWidth=18)](https://lgtm.com/projects/g/52North/sensorweb-server-dao-impl/context:javascript) [![Language grade: Java](https://img.shields.io/lgtm/grade/java/g/52North/sensorweb-server-dao-impl.svg?logo=lgtm&logoWidth=18)](https://lgtm.com/projects/g/52North/sensorweb-server-dao-impl/context:java)

<img style="width: 60%; height: 60%" alt="Sensor Web Server Helgoland architecture overview" src="https://52north.github.io/sensorweb-server-helgoland/version_3.x/assets/images/big-picture.png">

## Description

**Provide database data via 52n Sensor Web Server Helgoland.**

_This module is an SPI implementation of the 52°North Sensor Web Server Helgoland. It provides data access objects which
 access series data from relational databases. Using Hibernate the data model is not fixed to one specific._

The DAO module implements the Series API's Service Provider Interface (SPI). It serves as backend access layer 
to retrieve data from a relational database and to provide it via a well defined RESTful interface. Lightweight 
clients can then query and work with that series data easily in a common way (other data access mechanisms are 
available as well). Besides pure data access, the data can be preprocessed with common IO functionalities e.g. 
  * prerendering of series data, 
  * generalization, 
  * overlaying of data from multiple series
  * conversion of raw data to other formats like pdf and png

Using Hibernate makes it flexible to use almost arbitrary data models, i.e. there is no need to have convert your
data intot a specific data model. The combination of making adjustments to `hbm.xml` mapping files and database 
views (not always needed) make it possible to match existing databases quite well. The series data model of the 
52°North SOS is used by default and can be used right out-of-the-box.

The following main frameworks are used to provide this API:

- [Spring](https://spring.io/) 
- [Hibernate](https://hibernate.org/) 
- [52°North Series API](https://github.com/52North/sensorweb-server-helgoland/) 

## References
tbd

## License

The module is published under the [GNU General Public License v2 (GPLv2)](http://www.gnu.org/licenses/gpl-2.0.html).

## Changelog
- https://github.com/52North/sensorweb-server-dao-impl/blob/develop/CHANGELOG.md
- for detailed infos check https://github.com/52North/sensorweb-server-dao-impl/pulls?q=is%3Apr+is%3Aclosed

## Contributing
We try to follow [the GitFlow model](http://nvie.com/posts/a-successful-git-branching-model/), 
although we do not see it that strict. 

However, make sure to do pull requests for features, hotfixes, etc. by
making use of GitFlow. Altlassian provides [a good overview]
(https://www.atlassian.com/de/git/workflows#!workflow-gitflow). of the 
most common workflows.

## Contact
Henning Bredel (h.bredel@52north.org)

## Quick Start
### Webapp Installation
- tbd: deployment configuration
- tbd: build from source
- tbd: externalize config before build

### Configuration
- general config options 
  - Generalizer
  - Prerendering
  - Date formatting 
  - Rendering Hints
  - Status Intervals
  - Metadata from a Database

#### Logging
Depending on which build environment you've chosen open one the `WEB-INF/classes/logback-{dev,ci,prod}.xml`. Here
you can edit log levels and log outputs.

### Client development
Refer to the official [Series REST API documentation](http://52north.github.io/sensorweb-server-helgoland) to get a detailed 
overview on how to access the data provided by the API. 

## Credits

The development of the 52°North Sensor Web Server Helgoland DAO implementation was supported by several organizations and projects. Among other we would like to thank the following organisations and project

| Project/Logo | Description |
| :-------------: | :------------- |
| <a target="_blank" href="https://cos4cloud-eosc.eu/"><img alt="Cos4Cloud - Co-designed citizen observatories for the EOS-Cloud" align="middle" width="172" src="https://raw.githubusercontent.com/52North/sos/develop/spring/views/src/main/webapp/static/images/funding/cos4cloud.png" /></a> | The development of this version of the 52&deg;North Sensor Web Server DAO was supported by the <a target="_blank" href="https://ec.europa.eu/programmes/horizon2020/">European Union’s Horizon 2020</a> research project <a target="_blank" href="https://cos4cloud-eosc.eu/">Cos4Cloud</a> (co-funded by the European Commission under the grant agreement n&deg;863463) |
| <a target="_blank" href="https://bmbf.de/"><img alt="BMBF" align="middle" width="172" src="https://raw.githubusercontent.com/52North/sos/develop/spring/views/src/main/webapp/static/images/funding/bmbf_logo_en.png"/></a><a target="_blank" href="http://tamis.kn.e-technik.tu-dortmund.de/"><img alt="TaMIS - Das Talsperren-Mess-Informations-System" align="middle"  src="https://raw.githubusercontent.com/52North/sos/develop/spring/views/src/main/webapp/static/images/funding/TaMIS_Logo_small.png"/></a> |  The development of this version of the 52&deg;North Sensor Web Server DAO was supported by the <a target="_blank" href="https://www.bmbf.de/"> German Federal Ministry of Education and Research</a> research project <a target="_blank" href="http://tamis.kn.e-technik.tu-dortmund.de/">TaMIS</a> (co-funded by the German Federal Ministry of Education and Research, programme Geotechnologien, under grant agreement no. 03G0854[A-D]) |
| <a target="_blank" href="https://www.jerico-ri.eu/"><img alt="JERICO-S3 - Science - Services- Sustainability" align="middle" width="172" src="https://raw.githubusercontent.com/52North/sos/develop/spring/views/src/main/webapp/static/images/funding/jerico_s3.png" /></a> | The development of this version of the 52&deg;North Sensor Web Server DAO was supported by the <a target="_blank" href="https://ec.europa.eu/programmes/horizon2020/">European Union’s Horizon 2020</a> research project <a target="_blank" href="https://www.jerico-ri.eu/">JERICO-S3</a> (co-funded by the European Commission under the grant agreement n&deg;871153) |
| <a target="_blank" href="https://bmbf.de/"><img alt="BMBF" align="middle"  src="https://raw.githubusercontent.com/52North/sos/develop/spring/views/src/main/webapp/static/images/funding/bmbf_logo_en.png"/></a><a target="_blank" href="https://colabis.de/"><img alt="COLABIS - Collaborative Early Warning Information Systems for Urban Infrastructures" align="middle"  src="https://raw.githubusercontent.com/52North/sos/develop/spring/views/src/main/webapp/static/images/funding/colabis.png"/></a> | The development of this version of the 52&deg;North Sensor Web Server DAO was supported by the <a target="_blank" href="https://www.bmbf.de"> German Federal Ministry of Education and Research</a> research project <a target="_blank" href="https://colabis.de/">COLABIS</a> (co-funded by the German Federal Ministry of Education and Research, programme Geotechnologien, under grant agreement no. 03G0852A) |
| <a target="_blank" href="http://www.nexosproject.eu/"><img alt="NeXOS - Next generation, Cost-effective, Compact, Multifunctional Web Enabled Ocean Sensor Systems Empowering Marine, Maritime and Fisheries Management" align="middle" width="172" src="https://raw.githubusercontent.com/52North/sos/develop/spring/views/src/main/webapp/static/images/funding/logo_nexos.png" /></a> | The development of this version of the 52&deg;North Sensor Web Server DAO was supported by the <a target="_blank" href="http://cordis.europa.eu/fp7/home_en.html">European FP7</a> research project <a target="_blank" href="http://www.nexosproject.eu/">NeXOS</a> (co-funded by the European Commission under the grant agreement n&deg;614102) |
| <a target="_blank" href="http://www.fixo3.eu/"><img alt="FixO3 - Fixed-Point Open Ocean Observatories" align="middle" width="172" src="https://raw.githubusercontent.com/52North/sos/develop/spring/views/src/main/webapp/static/images/funding/logo_fixo3.png" /></a> | The development of this version of the 52&deg;North Sensor Web Server DAO was supported by the <a target="_blank" href="http://cordis.europa.eu/fp7/home_en.html">European FP7</a> research project <a target="_blank" href="http://www.fixo3.eu/">FixO3</a> (co-funded by the European Commission under the grant agreement n&deg;312463) |
| <a target="_blank" href="http://www.odip.org"><img alt="ODIP II - Ocean Data Interoperability Platform" align="middle" width="100" src="https://raw.githubusercontent.com/52North/sos/develop/spring/views/src/main/webapp/static/images/funding/odip-logo.png"/></a> | The development of this version of the 52&deg;North Sensor Web Server DAO was supported by the <a target="_blank" href="https://ec.europa.eu/programmes/horizon2020/">Horizon 2020</a> research project <a target="_blank" href="http://www.odip.org/">ODIP II</a> (co-funded by the European Commission under the grant agreement n&deg;654310) |
| <a target="_blank" href="https://www.seadatanet.org/About-us/SeaDataCloud/"><img alt="SeaDataCloud" align="middle" width="156" src="https://raw.githubusercontent.com/52North/sos/develop/spring/views/src/main/webapp/static/images/funding/logo_seadatanet.png"/></a> | The development of this version of the 52&deg;North Sensor Web Server DAO was supported by the <a target="_blank" href="https://ec.europa.eu/programmes/horizon2020/">Horizon 2020</a> research project <a target="_blank" href="https://www.seadatanet.org/About-us/SeaDataCloud/">SeaDataCloud</a> (co-funded by the European Commission under the grant agreement n&deg;730960) |
| <a target="_blank" href="http://www.wupperverband.de"><img alt="Wupperverband" align="middle" width="196" src="https://raw.githubusercontent.com/52North/sos/develop/spring/views/src/main/webapp/static/images/funding/logo_wv.jpg"/></a> | The <a target="_blank" href="http://www.wupperverband.de/">Wupperverband</a> for water, humans and the environment (Germany) |
| <a target="_blank" href="http://www.irceline.be/en"><img alt="Belgian Interregional Environment Agency (IRCEL - CELINE)" align="middle" width="130" src="https://raw.githubusercontent.com/52North/sos/develop/spring/views/src/main/webapp/static/images/funding/logo_irceline_no_text.png"/></a> | The <a href="http://www.irceline.be/en" target="_blank" title="Belgian Interregional Environment Agency (IRCEL - CELINE)">Belgian Interregional Environment Agency (IRCEL - CELINE)</a> is active in the domain of air quality (modelling, forecasts, informing the public on the state of their air quality, e-reporting to the EU under the air quality directives, participating in scientific research on air quality, etc.). IRCEL &mdash; CELINE is a permanent cooperation between three regional environment agencies: <a href="http://www.awac.be/" title="Agence wallonne de l&#39Air et du Climat (AWAC)">Agence wallonne de l'Air et du Climat (AWAC)</a>, <a href="http://www.ibgebim.be/" title="Bruxelles Environnement - Leefmilieu Brussel">Bruxelles Environnement - Leefmilieu Brussel</a> and <a href="http://www.vmm.be/" title="Vlaamse Milieumaatschappij (VMM)">Vlaamse Milieumaatschappij (VMM)</a>. |
| <a target="_blank" href="https://cordis.europa.eu/project/id/282915"><img alt="GEOWOW - GEOSS interoperability for Weather, Ocean and Water" align="middle" width="172" src="https://raw.githubusercontent.com/52North/sos/develop/spring/views/src/main/webapp/static/images/funding/logo_geowow.png"/></a> | The development of this version of the 52&deg;North Sensor Web Server DAO was supported by the <a target="_blank" href="http://cordis.europa.eu/fp7/home_en.html">European FP7</a> research project <a href="https://cordis.europa.eu/project/id/282915" title="GEOWOW">GEOWOW</a> (co-funded by the European Commission under the grant agreement n&deg;282915) |
