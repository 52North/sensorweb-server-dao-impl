/*
 * Copyright (C) 2015-2018 52°North Initiative for Geospatial Open Source
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

package org.n52.series.db.old.dao;

import static java.util.stream.Collectors.toSet;

import java.util.Date;
import java.util.List;
import java.util.Set;

import org.geotools.geometry.jts.JTS;
import org.hibernate.Criteria;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Disjunction;
import org.hibernate.criterion.Restrictions;
import org.hibernate.criterion.Subqueries;
import org.hibernate.spatial.criterion.SpatialRestrictions;
import org.hibernate.sql.JoinType;
import org.joda.time.DateTime;
import org.joda.time.Instant;
import org.joda.time.Interval;
import org.n52.io.IntervalWithTimeZone;
import org.n52.io.crs.BoundingBox;
import org.n52.io.crs.CRSUtils;
import org.n52.io.request.FilterResolver;
import org.n52.io.request.IoParameters;
import org.n52.io.request.Parameters;
import org.n52.io.response.PlatformType;
import org.n52.io.response.dataset.ValueType;
import org.n52.series.db.beans.DataEntity;
import org.n52.series.db.beans.DatasetEntity;
import org.n52.series.db.beans.DescribableEntity;
import org.n52.series.db.beans.IdEntity;
import org.n52.series.db.old.DataModelUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.PrecisionModel;

public class DbQuery {

    private static final Logger LOGGER = LoggerFactory.getLogger(DbQuery.class);

    private static final String PROPERTY_LOCALE = "locale";

    private static final String PROPERTY_TRANSLATIONS = "translations";

    private static final int DEFAULT_LIMIT = 10000;

    private final GeometryFactory geometryFactory;

    private final String databaseSridCode;

    private IoParameters parameters = IoParameters.createDefaults();

    public DbQuery(final IoParameters parameters) {
        this(parameters, CRSUtils.DEFAULT_CRS);
    }

    public DbQuery(final IoParameters parameters, final String databaseSridCode) {
        if (parameters != null) {
            this.parameters = parameters;
        }
        this.databaseSridCode= databaseSridCode == null
                ? CRSUtils.DEFAULT_CRS
                : databaseSridCode;

        final PrecisionModel pm = new PrecisionModel(PrecisionModel.FLOATING);
        this.geometryFactory = new GeometryFactory(pm, CRSUtils.getSrsIdFrom(databaseSridCode));
    }

    /**
     * Creates a new instance and removes spatial filter parameters.
     *
     * @return a new instance with spatial filters removed
     */
    public DbQuery removeSpatialFilter() {
        return new DbQuery(parameters.removeAllOf(Parameters.BBOX)
                                     .removeAllOf(Parameters.NEAR));
    }

    public GeometryFactory getGeometryFactory() {
        return geometryFactory;
    }

    /**
     * Create a new instance and replaces given parameter values.
     *
     * @param parameter
     *        the parameter which values to be replaced
     * @param values
     *        the new values
     * @return a new instance with containing the new parameter values
     */
    public DbQuery replaceWith(String parameter, String... values) {
        return new DbQuery(parameters.replaceWith(parameter, values));
    }

    public DbQuery replaceWith(String parameter, List<String> values) {
        return new DbQuery(parameters.replaceWith(parameter, values));
    }

    /**
     * Creates a new instance and removes all given parameters.
     *
     * @param parameterNames
     *        the parameters to remove
     * @return a new instance with given parameters removed
     */
    public DbQuery removeAllOf(String... parameterNames) {
        IoParameters ioParameters = parameters;
        if (parameterNames != null) {
            for (String parameterName : parameterNames) {
                ioParameters = ioParameters.removeAllOf(parameterName);
            }
        }
        return new DbQuery(ioParameters);
    }

    public String getDatabaseSridCode() {
        return databaseSridCode;
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

    public Geometry getSpatialFilter() {
        BoundingBox spatialFilter = parameters.getSpatialFilter();
        if (spatialFilter != null) {
            CRSUtils crsUtils = CRSUtils.createEpsgForcedXYAxisOrder();
            Point ll = spatialFilter.getLowerLeft();
            Point ur = spatialFilter.getUpperRight();
            GeometryFactory geomFactory = crsUtils.createGeometryFactory(databaseSridCode);
            Envelope envelope = new Envelope(ll.getCoordinate(), ur.getCoordinate());

            Geometry geometry = JTS.toGeometry(envelope, geomFactory);
            geometry.setSRID(CRSUtils.getSrsIdFromEPSG(databaseSridCode));
            return geometry;
        }
        return null;
    }

    private CRSUtils getCrsUtils() {
        return CRSUtils.createEpsgForcedXYAxisOrder();
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
        return !criteria.add(Restrictions.like(PROPERTY_LOCALE, getCountryCode()))
                        .list()
                        .isEmpty();
    }

    public Criteria addLocaleTo(Criteria criteria, Class< ? > clazz) {
        if ((getLocale() != null) && DataModelUtil.isEntitySupported(clazz, criteria)) {
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
            criteria.add(Restrictions.or(Restrictions.between(DataEntity.PROPERTY_SAMPLING_TIME_START, start, end),
                                         Restrictions.between(DataEntity.PROPERTY_SAMPLING_TIME_END, start, end)));
        }
        return criteria;
    }

    public Criteria addFilters(Criteria criteria, String datasetProperty) {
        addLimitAndOffsetFilter(criteria);
        addDetachedFilters(datasetProperty, criteria);
        return criteria;
    }

    public Criteria addOdataFilterForData(Criteria criteria) {
        FESCriterionGenerator generator = new DataFESCriterionGenerator(criteria,
                                                                        true,
                                                                        isMatchDomainIds(),
                                                                        isComplexParent());
        return addOdataFilter(generator, criteria);
    }

    public Criteria addOdataFilterForDataset(Criteria criteria) {
        FESCriterionGenerator generator = new DatasetFESCriterionGenerator(criteria,
                                                                           true,
                                                                           isMatchDomainIds(),
                                                                           isComplexParent());
        return addOdataFilter(generator, criteria);
    }

    private Criteria addOdataFilter(FESCriterionGenerator generator, Criteria criteria) {
        return parameters.getODataFilter()
                         .map(generator::create)
                         .map(criteria::add)
                         .orElse(criteria);
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

        if (! (hasValues(platforms)
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
        QueryUtils.setFilterProjectionOn(datasetName, filter);

        if (hasValues(platforms)) {
            features.addAll(getStationaryIds(platforms));
            procedures.addAll(getMobileIds(platforms));
        }

        addHierarchicalFilterRestriction(phenomena, DatasetEntity.PROPERTY_PHENOMENON, filter);
        addHierarchicalFilterRestriction(procedures, DatasetEntity.PROPERTY_PROCEDURE, filter);
        addHierarchicalFilterRestriction(offerings, DatasetEntity.PROPERTY_OFFERING, filter);
        addHierarchicalFilterRestriction(features, DatasetEntity.PROPERTY_FEATURE, filter);
        addFilterRestriction(categories, DatasetEntity.PROPERTY_CATEGORY, filter);
        addFilterRestriction(datasets, filter);

        criteria.add(Subqueries.propertyIn(IdEntity.PROPERTY_ID, filter));
        return criteria;
    }

    private DetachedCriteria addHierarchicalFilterRestriction(Set<String> values,
                                                              String property,
                                                              DetachedCriteria filter) {
        if (hasValues(values)) {
            String itemAlias = property + "_filter";
            String parentAlias = property + "_parent";
            String parentId = QueryUtils.createAssociation(parentAlias, IdEntity.PROPERTY_ID);
            filter.createCriteria(property, itemAlias)
                  // join the parents to enable filtering via parent ids
                  .createAlias(itemAlias + ".parents", parentAlias, JoinType.LEFT_OUTER_JOIN)
                  .add(Restrictions.or(createIdCriterion(values, itemAlias),
                                       Restrictions.in(parentId, QueryUtils.parseToIds(values))));
        }
        return filter;
    }

    private DetachedCriteria addFilterRestriction(Set<String> values, DetachedCriteria filter) {
        return addFilterRestriction(values, null, filter);
    }

    private DetachedCriteria addFilterRestriction(Set<String> values, String entity, DetachedCriteria filter) {
        if (hasValues(values)) {
            Criterion restriction = createIdCriterion(values);
            if ((entity == null) || entity.isEmpty()) {
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
        String column = QueryUtils.createAssociation(alias, DescribableEntity.PROPERTY_DOMAIN_ID);
        return filterValues.stream()
                           .map(filter -> Restrictions.ilike(column, filter))
                           .collect(Restrictions::disjunction,
                                    Disjunction::add,
                                    (a, b) -> b.conditions()
                                               .forEach(a::add));
    }

    private Criterion createIdFilter(Set<String> filterValues, String alias) {
        String column = QueryUtils.createAssociation(alias, IdEntity.PROPERTY_ID);
        return Restrictions.in(column, QueryUtils.parseToIds(filterValues));
    }

    private boolean hasValues(Set<String> values) {
        return (values != null) && !values.isEmpty();
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
            criteria.add(parameters.getResultTimes()
                                   .stream()
                                   .map(Instant::parse)
                                   .map(Instant::toDate)
                                   .map(x -> Restrictions.eq(DataEntity.PROPERTY_RESULT_TIME, x))
                                   .collect(Restrictions::disjunction,
                                            Disjunction::add,
                                            (a, b) -> b.conditions()
                                                       .forEach(a::add)));
        }
        return criteria;
    }

    public Criteria addSpatialFilter(Criteria criteria) {
        Criterion filter = createSpatialFilter();
        return filter != null
            ? criteria.add(filter)
            : criteria;
    }

    public DetachedCriteria addSpatialFilter(DetachedCriteria criteria) {
        Criterion filter = createSpatialFilter();
        return filter != null
            ? criteria.add(filter)
            : criteria;
    }

    private Criterion createSpatialFilter() {
        BoundingBox bbox = parameters.getSpatialFilter();
        if (bbox != null) {
            Geometry envelope = getSpatialFilter();
            String geometryMember = DataEntity.PROPERTY_GEOMETRY_ENTITY + ".geometry";

            return SpatialRestrictions.intersects(geometryMember, envelope);

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
