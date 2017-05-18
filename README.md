# DAO Implementation of 52n Series REST API

<img style="width: 60%; height: 60%" alt="series-rest-api architecture overview" src="https://52north.github.io/series-rest-api/img/big-picture.png">

## Description

**Provide database series data via 52n Series REST API.**

_This module is an SPI implementation of the 52°North Series REST API. It provides data access objects which
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
- [52°North Series API](https://github.com/52North/series-rest-api/) 

## References
tbd

## License

The module is published under the [GNU General Public License v2 (GPLv2)](http://www.gnu.org/licenses/gpl-2.0.html).

## Changelog
- https://github.com/52North/dao-series-api/blob/develop/CHANGELOG.md
- for detailed infos check https://github.com/52North/dao-series-api/pulls?q=is%3Apr+is%3Aclosed

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
Refer to the official [Series REST API documentation](http://52north.github.io/series-rest-api) to get a detailed 
overview on how to access the data provided by the API. 
