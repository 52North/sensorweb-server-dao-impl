/*
 * Copyright (C) 2015-2019 52°North Initiative for Geospatial Open Source
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

import static java.util.stream.Collectors.toSet;

import java.util.Date;
import java.util.Set;

import org.hibernate.Criteria;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Disjunction;
import org.hibernate.criterion.Property;
import org.hibernate.criterion.Restrictions;
import org.hibernate.criterion.Subqueries;
import org.hibernate.spatial.criterion.SpatialFilter;
import org.hibernate.spatial.criterion.SpatialRestrictions;
import org.hibernate.sql.JoinType;
import org.joda.time.Instant;
import org.joda.time.Interval;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.operation.TransformException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Point;

public class DbQuery {

    private static final Logger LOGGER = LoggerFactory.getLogger(DbQuery.class);

    private static final String PROPERTY_PKID = "pkid";

    private static final String PROPERTY_LOCALE = "locale";

    private static final String PROPERTY_TRANSLATIONS = "translations";

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

    public Envelope getSpatialFilter() {
        BoundingBox spatialFilter = parameters.getSpatialFilter();
        if (spatialFilter != null) {
            try {
                CRSUtils crsUtils = CRSUtils.createEpsgForcedXYAxisOrder();
                Point ll = (Point) crsUtils.transformInnerToOuter(spatialFilter.getLowerLeft(), databaseSridCode);
                Point ur = (Point) crsUtils.transformInnerToOuter(spatialFilter.getUpperRight(), databaseSridCode);
                return new Envelope(ll.getCoordinate(), ur.getCoordinate());
            } catch (FactoryException e) {
                LOGGER.error("Could not create transformation facilities.", e);
            } catch (TransformException e) {
                LOGGER.error("Could not perform transformation.", e);
            }
        }
        return null;
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

    public String getHandleAsValueTypeFallback() {
        return parameters.containsParameter(Parameters.HANDLE_AS_VALUE_TYPE)
                ? parameters.getAsString(Parameters.HANDLE_AS_VALUE_TYPE)
                : ValueType.DEFAULT_VALUE_TYPE;
    }

    public boolean checkTranslationForLocale(Criteria criteria) {
        return !criteria.add(Restrictions.like(PROPERTY_LOCALE, getCountryCode())).list().isEmpty();
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
            Date start = interval.getStart().toDate();
            Date end = interval.getEnd().toDate();
            criteria.add(Restrictions.or(Restrictions.between(DataEntity.PROPERTY_TIMESTART, start, end),
                                         Restrictions.between(DataEntity.PROPERTY_TIMEEND, start, end)));
        }
        return criteria;
    }

    public Criteria addFilters(Criteria criteria, String datasetProperty) {
        addLimitAndOffsetFilter(criteria);
        addDetachedFilters(datasetProperty, criteria);
        return criteria;
    }

    public Criteria addOdataFilterForData(Criteria criteria) {
        FESCriterionGenerator generator
                = new DataFESCriterionGenerator(criteria, true, isMatchDomainIds(), isComplexParent());
        return addOdataFilter(generator, criteria);
    }

    public Criteria addOdataFilterForDataset(Criteria criteria) {
        FESCriterionGenerator generator
                = new DatasetFESCriterionGenerator(criteria, true, isMatchDomainIds(), isComplexParent());
        return addOdataFilter(generator, criteria);
    }

    private Criteria addOdataFilter(FESCriterionGenerator generator, Criteria criteria) {
        return parameters.getODataFilter().map(generator::create).map(criteria::add).orElse(criteria);
    }

    private Criteria addLimitAndOffsetFilter(Criteria criteria) {
        if (getParameters().containsParameter(Parameters.OFFSET)) {
            int limit = (getParameters().containsParameter(Parameters.LIMIT))
                    ? getParameters().getLimit()
                    : DEFAULT_LIMIT;
            limit = (limit > 0)
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
        Set<String> series = parameters.getSeries();

        if (!(hasValues(platforms)
                || hasValues(phenomena)
                || hasValues(procedures)
                || hasValues(offerings)
                || hasValues(features)
                || hasValues(categories)
                || hasValues(datasets)
                || hasValues(series))) {
            // no subquery neccessary
            return criteria;
        }

        DetachedCriteria filter = DetachedCriteria.forClass(DatasetEntity.class);
        if (hasValues(platforms)) {
            features.addAll(getStationaryIds(platforms));
            procedures.addAll(getMobileIds(platforms));
        }

        addFilterRestriction(phenomena, DatasetEntity.PROPERTY_PHENOMENON, filter);
        addHierarchicalFilterRestriction(procedures, DatasetEntity.PROPERTY_PROCEDURE, filter, "p_");
        addHierarchicalFilterRestriction(offerings, DatasetEntity.PROPERTY_OFFERING, filter, "off_");
        addFilterRestriction(features, DatasetEntity.PROPERTY_FEATURE, filter);
        addFilterRestriction(categories, DatasetEntity.PROPERTY_CATEGORY, filter);
        addFilterRestriction(series, filter);

        addFilterRestriction(datasets.stream().map(ValueType::extractId).collect(toSet()),
                             filter);

        // TODO refactory/simplify projection
        String projectionProperty = QueryUtils.createAssociation(datasetName, PROPERTY_PKID);
        filter.setProjection(Property.forName(projectionProperty));

        String filterProperty = QueryUtils.createAssociation(datasetName, PROPERTY_PKID);
        criteria.add(Subqueries.propertyIn(filterProperty, filter));
        return criteria;
    }

    private DetachedCriteria addHierarchicalFilterRestriction(Set<String> values,
                                                              String entity,
                                                              DetachedCriteria filter,
                                                              String prefix) {
        if (hasValues(values)) {
            filter.createCriteria(entity, prefix + "e")
                  // join the parents to enable filtering via parent ids
                  .createAlias(prefix + "e.parents", prefix + "p", JoinType.LEFT_OUTER_JOIN)
                  .add(Restrictions.or(createIdCriterion(values, prefix + "e"),
                                       Restrictions.in(prefix + "p.pkid", QueryUtils.parseToIds(values))));
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
        return filterValues.stream().map(filter -> Restrictions.ilike(column, filter))
                .collect(Restrictions::disjunction, Disjunction::add,
                         (a, b) -> b.conditions().forEach(a::add));
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
                        .filter(PlatformType::isStationaryId)
                        .map(PlatformType::extractId)
                        .collect(toSet());
    }

    private Set<String> getMobileIds(Set<String> platforms) {
        return platforms.stream()
                .filter(PlatformType::isMobileId)
                .map(PlatformType::extractId)
                .collect(toSet());
    }

    public Criteria addResultTimeFilter(Criteria criteria) {
        if (parameters.shallClassifyByResultTimes()) {
            criteria.add(parameters.getResultTimes().stream()
                    .map(Instant::parse).map(Instant::toDate)
                    .map(x -> Restrictions.eq(DataEntity.PROPERTY_RESULTTIME, x))
                    .collect(Restrictions::disjunction, Disjunction::add,
                             (a, b) -> b.conditions().forEach(a::add)));
        }
        return criteria;
    }

    public Criteria addSpatialFilter(Criteria criteria) {
        SpatialFilter filter = createSpatialFilter();
        return filter != null ? criteria.add(filter) : criteria;
    }

    public DetachedCriteria addSpatialFilter(DetachedCriteria criteria) {
        SpatialFilter filter = createSpatialFilter();
        return filter != null ? criteria.add(filter) : criteria;
    }

    public SpatialFilter createSpatialFilter() {
        Envelope envelope = getSpatialFilter();
        if (envelope != null) {
            CRSUtils crsUtils = CRSUtils.createEpsgForcedXYAxisOrder();
            int databaseSrid = crsUtils.getSrsIdFrom(databaseSridCode);
            String geometryMember = DataEntity.PROPERTY_GEOMETRY_ENTITY + ".geometry";
            return SpatialRestrictions.filter(geometryMember, envelope, databaseSrid);

            // TODO intersect with linestring
            // XXX do sampling filter only on generated line strings stored in FOI table,
            // otherwise we would have to check each observation row
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

    public DbQuery withoutFieldsFilter() {
        return new DbQuery(parameters.removeAllOf(Parameters.FILTER_FIELDS));
    }

}
