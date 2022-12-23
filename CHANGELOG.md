# 2.x

## 3.0.0

### Changes
- Integration of Spring Data and QueryDSL

### Issues
- [#121](https://github.com/52North/dao-series-api/issues/121) Implement next/previous values beyond requested timespan

## 2.0.0

### Changes
- `offering` and `procedure` are now hierarchical
- result time extension
- add SOS `om:parameter` to `feature`/`platform`
- SOS named parameters are mapped to data output
- separated project into own repository project
- `service` is now a full qualified entity
- Support for pluralized filter parameters
- output includes href property
- more consistent filter handling
- result filtering via OData $filter

### Features
- [#129](https://github.com/52North/series-rest-api/issues/129) Implement Paging for DAO SPI implemenation
- [#232](https://github.com/52North/series-rest-api/issues/232) possibility to use `domainId` instead of database id
- [#251](https://github.com/52North/series-rest-api/issues/251) allow offering to be full qualified parameter
- [#266](https://github.com/52North/series-rest-api/issues/266) observations having time intervals
- [#320](https://github.com/52North/series-rest-api/issues/320) register configurable timezone type
- [#31](https://github.com/52North/dao-series-api/issues/31) support for profile observations
- [#391](https://github.com/52North/series-rest-api/issues/391] support spatial filter for data requests
- [#161](https://github.com/52North/series-rest-api/issues/161] support alternative GeoJSON `bbox` format
- 

### Issues 
- [#246](https://github.com/52North/series-rest-api/issues/246) no use of deleted flag in `series` table
- [#279](https://github.com/52North/series-rest-api/issues/279) Querying multiple (time-)series causes race condition 

## 1.x
Below `v2.x` the project was part of [the series-rest-api](https://github.com/52North/series-rest-api/). You can find [the SPI implementation](https://github.com/52North/series-rest-api/tree/version/1.9.x/timeseries-spi-impl/sos-series-dao) by following the `v1.x` development line.
