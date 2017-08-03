/*
 * Copyright (C) 2015-2017 52°North Initiative for Geospatial Open Source
 * Software GmbH
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 as published
 * by the Free Software Foundation.
 *
 * If the program is linked with libraries which are licensed under one of
 * the following licenses, the combination of the program with the linked
 * library is not considered a "derivative work" of the program:
 *
 *     - Apache License, version 2.0
 *     - Apache Software License, version 1.0
 *     - GNU Lesser General Public License, version 3
 *     - Mozilla Public License, versions 1.0, 1.1 and 2.0
 *     - Common Development and Distribution License (CDDL), version 1.0
 *
 * Therefore the distribution of the program linked with libraries licensed
 * under the aforementioned licenses, is permitted by the copyright holders
 * if the distribution is compliant with both the GNU General Public License
 * version 2 and the aforementioned licenses.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 */

package org.n52.series.db.dao;

import java.util.Date;
import java.util.Set;
import java.util.stream.Collectors;

import org.hibernate.Criteria;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Disjunction;
import org.hibernate.criterion.LogicalExpression;
import org.hibernate.criterion.Restrictions;
import org.hibernate.criterion.Subqueries;
import org.hibernate.spatial.GeometryType;
import org.hibernate.spatial.GeometryType.Type;
import org.hibernate.spatial.criterion.SpatialRestrictions;
import org.hibernate.sql.JoinType;
import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.n52.io.IntervalWithTimeZone;
import org.n52.io.crs.BoundingBox;
import org.n52.io.crs.CRSUtils;
import org.n52.io.request.FilterResolver;
import org.n52.io.request.IoParameters;
import org.n52.io.request.Parameters;
import org.n52.io.response.PlatformType;
import org.n52.io.response.dataset.ValueType;
import org.n52.series.db.DataModelUtil;
import org.n52.series.db.beans.DataEntity;
import org.n52.series.db.beans.DatasetEntity;
import org.n52.series.db.beans.DescribableEntity;
import org.n52.series.db.beans.ObservationConstellationEntity;
import org.n52.series.db.beans.PlatformEntity;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.operation.TransformException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Point;

public class DbQuery {

    private static final Logger LOGGER = LoggerFactory.getLogger(DbQuery.class);

    private static final String PROPERTY_PKID = "pkid";

    private static final String PROPERTY_LOCALE = "locale";

    private static final String PROPERTY_TRANSLATIONS = "translations";

    private static final String PROPERTY_GEOMETRY_ENTITY = "geometryEntity.geometry";

    private static final int DEFAULT_LIMIT = 10000;

    private IoParameters parameters = IoParameters.createDefaults();

    private String databaseSridCode = "EPSG:4326";

    public DbQuery(IoParameters parameters) {
        if (parameters != null) {
            this.parameters = parameters;
        }
    }

    public String getDatabaseSridCode() {
        return databaseSridCode;
    }

    public void setDatabaseSridCode(String databaseSridCode) {
        this.databaseSridCode = databaseSridCode;
    }

    public String getHrefBase() {
        return parameters.getHrefBase();
    }

    public String getLocale() {
        return parameters.getLocale();
    }

    public String getSearchTerm() {
        return parameters.getAsString(Parameters.SEARCH_TERM);
    }

    public Interval getTimespan() {
        return parameters.getTimespan()
                         .toInterval();
    }

    public BoundingBox getSpatialFilter() {
        return parameters.getSpatialFilter();
    }

    public boolean isExpanded() {
        return parameters.isExpanded();
    }

    public boolean isMatchDomainIds() {
        return parameters.getAsBoolean(Parameters.MATCH_DOMAIN_IDS, Parameters.DEFAULT_MATCH_DOMAIN_IDS);
    }

    public void setComplexParent(boolean complex) {
        parameters = parameters.extendWith(Parameters.COMPLEX_PARENT, Boolean.toString(complex));
    }

    public boolean isComplexParent() {
        return parameters.getAsBoolean(Parameters.COMPLEX_PARENT, false);
    }

    public Set<String> getValueTypes() {
        return parameters.getValueTypes();
    }

    public boolean isSetValueTypeFilter() {
        return !parameters.getValueTypes()
                          .isEmpty();
    }

    public Date getResultTime() {
        return parameters.containsParameter(Parameters.RESULTTIME)
                ? parameters.getResultTime()
                            .toDate()
                : null;
    }

    public String getHandleAsValueTypeFallback() {
        return parameters.containsParameter(Parameters.HANDLE_AS_VALUE_TYPE)
                ? parameters.getAsString(Parameters.HANDLE_AS_VALUE_TYPE)
                : ValueType.DEFAULT_VALUE_TYPE;
    }

    public boolean checkTranslationForLocale(Criteria criteria) {
        return !criteria.add(Restrictions.like(PROPERTY_LOCALE, getCountryCode()))
                        .list()
                        .isEmpty();
    }

    public Criteria addLocaleTo(Criteria criteria, Class< ? > clazz) {
        if (getLocale() != null && DataModelUtil.isEntitySupported(clazz, criteria)) {
            Criteria translations = criteria.createCriteria(PROPERTY_TRANSLATIONS, JoinType.LEFT_OUTER_JOIN);
            translations.add(Restrictions.or(Restrictions.like(PROPERTY_LOCALE, getCountryCode()),
                                             Restrictions.isNull(PROPERTY_LOCALE)));
        }
        return criteria;
    }

    private String getCountryCode() {
        return getLocale().split("_")[0];
    }

    public Criteria addTimespanTo(Criteria criteria) {
        IntervalWithTimeZone timespan = parameters.getTimespan();
        if (timespan != null) {
            Interval interval = timespan.toInterval();
            DateTime startDate = interval.getStart();
            DateTime endDate = interval.getEnd();
            Date start = startDate.toDate();
            Date end = endDate.toDate();
            criteria.add(Restrictions.or(Restrictions.between(DataEntity.PROPERTY_PHENOMENON_TIME_START, start, end),
                                         Restrictions.between(DataEntity.PROPERTY_PHENOMENON_TIME_END, start, end)));
        }
        return criteria;
    }

    public Criteria addFilters(Criteria criteria, String datasetProperty) {
        addLimitAndOffsetFilter(criteria);
        addDetachedFilters(datasetProperty, criteria);
        addPlatformTypeFilter(datasetProperty, criteria);
        addValueTypeFilter(datasetProperty, criteria);
        return addSpatialFilterTo(criteria);
    }

    private Criteria addLimitAndOffsetFilter(Criteria criteria) {
        if (getParameters().containsParameter(Parameters.OFFSET)) {
            int limit = (getParameters().containsParameter(Parameters.LIMIT))
                    ? getParameters().getLimit()
                    : DEFAULT_LIMIT;
            limit = (limit > 1)
                    ? limit
                    : DEFAULT_LIMIT;
            criteria.setFirstResult(getParameters().getOffset() * limit);
        }
        if (getParameters().containsParameter(Parameters.LIMIT)) {
            criteria.setMaxResults(getParameters().getLimit());
        }
        return criteria;
    }

    public Criteria addDetachedFilters(String datasetName, Criteria criteria) {
        Set<String> categories = parameters.getCategories();
        Set<String> procedures = parameters.getProcedures();
        Set<String> phenomena = parameters.getPhenomena();
        Set<String> offerings = parameters.getOfferings();
        Set<String> platforms = parameters.getPlatforms();
        Set<String> features = parameters.getFeatures();
        Set<String> datasets = parameters.getDatasets();

        if (!(hasValues(platforms)
                || hasValues(phenomena)
                || hasValues(procedures)
                || hasValues(offerings)
                || hasValues(features)
                || hasValues(categories)
                || hasValues(datasets))) {
            // no subquery neccessary
            return criteria;
        }

        String constellation = "constellation";
        DetachedCriteria filter = DetachedCriteria.forClass(DatasetEntity.class);
        DetachedCriteria observationConstellationFilter = createObservationConstellationFilter(filter, constellation);

        String[] associationPathElements = datasetName.split("\\.", 2);
        if (associationPathElements.length == 2) {
            String property = associationPathElements[1];
            QueryUtils.projectOnPkid(constellation, property, observationConstellationFilter);
        } else {
            filter = !datasetName.isEmpty()
                    ? filter.setProjection(QueryUtils.projectionOn(datasetName))
                    : filter.setProjection(QueryUtils.projectionOnPkid());
        }

        if (hasValues(platforms)) {
            features.addAll(getStationaryIds(platforms));
            procedures.addAll(getMobileIds(platforms));
        }

        if (hasValues(phenomena)) {
            addHierarchicalFilterRestriction(phenomena,
                                             ObservationConstellationEntity.OBSERVABLE_PROPERTY,
                                             observationConstellationFilter);
        }

        if (hasValues(procedures)) {
            addHierarchicalFilterRestriction(procedures,
                                             ObservationConstellationEntity.PROCEDURE,
                                             observationConstellationFilter);
        }

        if (hasValues(offerings)) {
            addHierarchicalFilterRestriction(offerings,
                                             ObservationConstellationEntity.OFFERING,
                                             observationConstellationFilter);
        }

        if (hasValues(features)) {
            addHierarchicalFilterRestriction(features, DatasetEntity.PROPERTY_FEATURE, filter);
        }

        if (hasValues(categories)) {
            addFilterRestriction(categories, DatasetEntity.PROPERTY_CATEGORY, filter);
        }

        if (hasValues(datasets)) {
            addFilterRestriction(datasets, filter);
        }

        criteria.add(Subqueries.propertyIn(DescribableEntity.PROPERTY_PKID, filter));
        return criteria;
    }

    private DetachedCriteria createObservationConstellationFilter(DetachedCriteria filter, String alias) {
        return filter.createCriteria(DatasetEntity.PROPERTY_OBSERVATION_CONSTELLATION, alias);
    }

    private DetachedCriteria addHierarchicalFilterRestriction(Set<String> values,
                                                              String property,
                                                              DetachedCriteria filter) {
        if (hasValues(values)) {
            String itemAlias = property + "_filter";
            String parentAlias = property + "_parent";
            filter.createCriteria(property, itemAlias)
                  // join the parents to enable filtering via parent ids
                  .createAlias(itemAlias + ".parents", parentAlias, JoinType.LEFT_OUTER_JOIN)
                  .add(Restrictions.or(createIdCriterion(values, itemAlias),
                                       Restrictions.in(parentAlias + ".pkid", QueryUtils.parseToIds(values))));
        }
        return filter;
    }

    private DetachedCriteria addFilterRestriction(Set<String> values, DetachedCriteria filter) {
        return addFilterRestriction(values, null, filter);
    }

    private DetachedCriteria addFilterRestriction(Set<String> values, String entity, DetachedCriteria filter) {
        if (hasValues(values)) {
            Criterion restriction = createIdCriterion(values);
            if (entity == null || entity.isEmpty()) {
                return filter.add(restriction);
            } else {
                // return subquery for further chaining
                return filter.createCriteria(entity)
                             .add(restriction);
            }
        }
        return filter;
    }

    private Criterion createIdCriterion(Set<String> values) {
        return createIdCriterion(values, null);
    }

    private Criterion createIdCriterion(Set<String> values, String alias) {
        return parameters.isMatchDomainIds()
                ? createDomainIdFilter(values, alias)
                : createIdFilter(values, alias);
    }

    private Criterion createDomainIdFilter(Set<String> filterValues, String alias) {
        String column = QueryUtils.createAssociation(alias, DatasetEntity.PROPERTY_DOMAIN_ID);
        Disjunction disjunction = Restrictions.disjunction();
        for (String filter : filterValues) {
            disjunction.add(Restrictions.ilike(column, filter));
        }
        return disjunction;
    }

    private Criterion createIdFilter(Set<String> filterValues, String alias) {
        String column = QueryUtils.createAssociation(alias, PROPERTY_PKID);
        return Restrictions.in(column, QueryUtils.parseToIds(filterValues));
    }

    private boolean hasValues(Set<String> values) {
        return values != null && !values.isEmpty();
    }

    private Set<String> getStationaryIds(Set<String> platforms) {
        return platforms.stream()
                        .filter(e -> PlatformType.isStationaryId(e))
                        .map(e -> PlatformType.extractId(e))
                        .collect(Collectors.toSet());
    }

    private Set<String> getMobileIds(Set<String> platforms) {
        return platforms.stream()
                        .filter(e -> PlatformType.isMobileId(e))
                        .map(e -> PlatformType.extractId(e))
                        .collect(Collectors.toSet());
    }

    /**
     * Adds an platform type filter to the query.
     *
     * @param parameter
     *        the parameter to filter on.
     * @param criteria
     *        the criteria to add the filter to.
     * @return the criteria to chain.
     */
    private Criteria addPlatformTypeFilter(String parameter, Criteria criteria) {
        FilterResolver filterResolver = getFilterResolver();
        if (!filterResolver.shallIncludeAllPlatformTypes()) {
            LogicalExpression platformTypeRestriction = Restrictions.and(createMobileExpression(filterResolver),
                                                                         createInsituExpression(filterResolver));

            if (parameter == null || parameter.isEmpty()) {
                // join starts from dataset table
                criteria.createCriteria(DatasetEntity.PROPERTY_OBSERVATION_CONSTELLATION)
                        .createCriteria(ObservationConstellationEntity.PROCEDURE)
                        .add(platformTypeRestriction);
            } else if (parameter.endsWith(ObservationConstellationEntity.PROCEDURE)) {
                // restrict directly on procedure table
                criteria.add(platformTypeRestriction);
            } else {
                // join procedure table via dataset table
                String alias = "platformTypeFilter";
                DetachedCriteria c = DetachedCriteria.forClass(DatasetEntity.class);
                c.createCriteria(DatasetEntity.PROPERTY_OBSERVATION_CONSTELLATION, alias)
                 .createCriteria(ObservationConstellationEntity.PROCEDURE)
                 .add(platformTypeRestriction);

                String[] associationPathElements = parameter.split("\\.", 2);
                if (associationPathElements.length == 2) {
                    // other observationconstellation members
                    String member = associationPathElements[1];
                    c.setProjection(QueryUtils.projectionOnPkid(alias, member));
                } else {
                    // feature case only
                    c.setProjection(QueryUtils.projectionOn(parameter));
                }
                criteria.add(Subqueries.propertyIn(DescribableEntity.PROPERTY_PKID, c));
            }
        }
        return criteria;
    }

    private Criteria addValueTypeFilter(String parameter, Criteria criteria) {
        Set<String> valueTypes = getParameters().getValueTypes();
        if (!valueTypes.isEmpty()) {
            FilterResolver filterResolver = getFilterResolver();
            if (filterResolver.shallBehaveBackwardsCompatible() || !filterResolver.shallIncludeAllDatasetTypes()) {
                Criterion containsValueType = Restrictions.in(DatasetEntity.PROPERTY_VALUE_TYPE, valueTypes);
                if (parameter == null || parameter.isEmpty()) {
                    // join starts from dataset table
                    criteria.add(containsValueType);
                } else {
                    String alias = "valueTypeFilter";
                    DetachedCriteria c = DetachedCriteria.forClass(DatasetEntity.class);
                    c.createCriteria(DatasetEntity.PROPERTY_OBSERVATION_CONSTELLATION, alias)
                     .add(Restrictions.in(DatasetEntity.PROPERTY_VALUE_TYPE, valueTypes));

                    String[] associationPathElements = parameter.split("\\.", 2);
                    if (associationPathElements.length == 2) {
                        // other observationconstellation members
                        String member = associationPathElements[1];
                        c.setProjection(QueryUtils.projectionOnPkid(alias, member));
                    } else {
                        // feature case only
                        c.setProjection(QueryUtils.projectionOn(parameter));
                    }
                    criteria.add(Subqueries.propertyIn(DescribableEntity.PROPERTY_PKID, c));
                }
            }
        }
        return criteria;
    }

    private LogicalExpression createMobileExpression(FilterResolver filterResolver) {
        boolean includeStationary = filterResolver.shallIncludeStationaryPlatformTypes();
        boolean includeMobile = filterResolver.shallIncludeMobilePlatformTypes();
        return Restrictions.or(Restrictions.eq(PlatformEntity.PROPERTY_MOBILE, includeMobile),
                               // inverse to match filter
                               Restrictions.eq(PlatformEntity.PROPERTY_MOBILE, !includeStationary));
    }

    private LogicalExpression createInsituExpression(FilterResolver filterResolver) {
        boolean includeInsitu = filterResolver.shallIncludeInsituPlatformTypes();
        boolean includeRemote = filterResolver.shallIncludeRemotePlatformTypes();
        return Restrictions.or(Restrictions.eq(PlatformEntity.PROPERTY_INSITU, includeInsitu),
                               // inverse to match filter
                               Restrictions.eq(PlatformEntity.PROPERTY_INSITU, !includeRemote));
    }

    public Criteria addSpatialFilterTo(Criteria criteria) {
        if (DataModelUtil.isPropertyNameSupported(PROPERTY_GEOMETRY_ENTITY, criteria)) {
            BoundingBox spatialFilter = parameters.getSpatialFilter();
            if (spatialFilter != null) {
                try {
                    CRSUtils crsUtils = CRSUtils.createEpsgForcedXYAxisOrder();
                    int databaseSrid = crsUtils.getSrsIdFrom(databaseSridCode);
                    Point ll = (Point) crsUtils.transformInnerToOuter(spatialFilter.getLowerLeft(), databaseSridCode);
                    Point ur = (Point) crsUtils.transformInnerToOuter(spatialFilter.getUpperRight(), databaseSridCode);
                    Envelope envelope = new Envelope(ll.getCoordinate(), ur.getCoordinate());

                    criteria.add(SpatialRestrictions.filter(PROPERTY_GEOMETRY_ENTITY, envelope, databaseSrid));

                    // TODO intersect with linestring
                    // XXX do sampling filter only on generated line strings stored in FOI table,
                    // otherwise we would have to check each observation row
                } catch (FactoryException e) {
                    LOGGER.error("Could not create transformation facilities.", e);
                } catch (TransformException e) {
                    LOGGER.error("Could not perform transformation.", e);
                }
            }

            Set<String> geometryTypes = parameters.getGeometryTypes();
            for (String geometryType : geometryTypes) {
                if (!geometryType.isEmpty()) {
                    Type type = getGeometryType(geometryType);
                    if (type != null) {
                        criteria.add(SpatialRestrictions.geometryType(PROPERTY_GEOMETRY_ENTITY, type));
                    }
                }
            }
        }
        return criteria;
    }

    private Type getGeometryType(String geometryType) {
        for (GeometryType.Type type : GeometryType.Type.values()) {
            if (type.name()
                    .equalsIgnoreCase(geometryType)) {
                return type;
            }
        }
        return null;
    }

    public IoParameters getParameters() {
        return parameters;
    }

    public FilterResolver getFilterResolver() {
        return parameters.getFilterResolver();
    }

    @Override
    public String toString() {
        return "DbQuery{ parameters=" + getParameters().toString() + "'}'";
    }

}
