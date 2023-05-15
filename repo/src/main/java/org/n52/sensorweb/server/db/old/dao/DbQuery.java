/*
 * Copyright (C) 2015-2023 52°North Spatial Information Research GmbH
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
package org.n52.sensorweb.server.db.old.dao;

import java.util.Collection;
import java.text.NumberFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.geotools.geometry.jts.JTS;
import org.hibernate.Criteria;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Disjunction;
import org.hibernate.criterion.Property;
import org.hibernate.criterion.Restrictions;
import org.hibernate.criterion.Subqueries;
import org.hibernate.spatial.criterion.SpatialRestrictions;
import org.hibernate.sql.JoinType;
import org.joda.time.DateTime;
import org.joda.time.Instant;
import org.joda.time.Interval;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.n52.io.IntervalWithTimeZone;
import org.n52.io.crs.BoundingBox;
import org.n52.io.crs.CRSUtils;
import org.n52.io.request.FilterResolver;
import org.n52.io.request.IoParameters;
import org.n52.io.request.Parameters;
import org.n52.sensorweb.server.db.old.DataModelUtil;
import org.n52.janmayen.i18n.LocaleHelper;
import org.n52.series.db.beans.DataEntity;
import org.n52.series.db.beans.DatasetEntity;
import org.n52.series.db.beans.DescribableEntity;
import org.n52.series.db.beans.OfferingEntity;
import org.n52.series.db.beans.ProcedureEntity;
import org.n52.series.db.beans.sampling.SamplingEntity;
import org.n52.series.db.beans.sampling.SamplingProfileDatasetEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Joiner;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

public class DbQuery {

    public static final String FEATURE_ALIAS = "feature_";

    public static final String PROCEDURE_ALIAS = "procedure_";

    public static final String PHENOMENON_ALIAS = "phenomenon_";

    public static final String OFFERING_ALIAS = "offering_";

    private static final Logger LOGGER = LoggerFactory.getLogger(DbQuery.class);

    private static final String PROPERTY_ID = "id";

    private static final String PROPERTY_LOCALE = "locale";

    private static final String PROPERTY_TRANSLATIONS = "translations";

    private static final String PROPERTY_SERVICE_ID = "service." + PROPERTY_ID;

    private static final int DEFAULT_LIMIT = 10000;

    private final GeometryFactory geometryFactory;

    private String databaseSridCode;

    private IoParameters parameters = IoParameters.createDefaults();

    private boolean includeHierarchy = true;

    private String locale;

    private String localeForLabel;

    private boolean isDefaultLocale;

    private String hrefBase;

    private Map<String, Criteria> subCriteria = new LinkedHashMap<>();


    private Boolean formatToUnixTime;

    private NumberFormat numberFormat;

    private Boolean isExpanded;

    public DbQuery(IoParameters parameters) {
        if (parameters != null) {
            this.parameters = parameters;
            this.locale = parameters.getLocale();
            this.isDefaultLocale = parameters.isDefaultLocal();
            this.localeForLabel = isDefaultLocal() ? null : getLocale();
            this.hrefBase = parameters.getHrefBase();
            this.formatToUnixTime = parameters.formatToUnixTime();
        }
        this.databaseSridCode = databaseSridCode == null ? CRSUtils.DEFAULT_CRS : databaseSridCode;

        final PrecisionModel pm = new PrecisionModel(PrecisionModel.FLOATING);
        this.geometryFactory = new GeometryFactory(pm, CRSUtils.getSrsIdFrom(databaseSridCode));
    }

    /**
     * Creates a new instance and removes spatial filter parameters.
     *
     * @return a new instance with spatial filters removed
     */
    public DbQuery removeSpatialFilter() {
        return new DbQuery(parameters.removeAllOf(Parameters.BBOX).removeAllOf(Parameters.NEAR));
    }

    public GeometryFactory getGeometryFactory() {
        return geometryFactory;
    }

    /**
     * Create a new instance and replaces given parameter values.
     *
     * @param parameter
     *            the parameter which values to be replaced
     * @param values
     *            the new values
     * @return a new instance with containing the new parameter values
     */
    public DbQuery replaceWith(String parameter, String... values) {
        return new DbQuery(parameters.replaceWith(parameter, values));
    }

    public DbQuery replaceWith(String parameter, Collection<String> values) {
        return new DbQuery(parameters.replaceWith(parameter, values));
    }

    /**
     * Creates a new instance and removes all given parameters.
     *
     * @param parameterNames
     *            the parameters to remove
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

    public void setDatabaseSridCode(String databaseSridCode) {
        this.databaseSridCode = databaseSridCode;
    }

    public String getHrefBase() {
        return hrefBase;
    }

    public String getLocale() {
        return locale;
    }

    public String getLocaleForLabel() {
        return localeForLabel;
    }

    public boolean isDefaultLocal() {
        return isDefaultLocale;
    }

    public String getSearchTerm() {
        return Joiner.on(",").join(parameters.getSearchTerms());
    }

    public Interval getTimespan() {
        return parameters.getTimespan().toInterval();
    }

    public Interval getLastValueMatches() {
        return parameters.getLastValueMatches() != null ? parameters.getLastValueMatches().toInterval() : null;
    }

    public Integer getLevel() {
        return parameters.getLevel();
    }

    public Geometry getSpatialFilter() {
        BoundingBox spatialFilter = parameters.getSpatialFilter();
        if (spatialFilter != null) {
            CRSUtils crsUtils = getCrsUtils();
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
        if (isExpanded == null) {
            isExpanded = parameters.isExpanded();
        }
        return isExpanded;
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

    public boolean isSetDatasetTypeFilter() {
        return !parameters.getDatasetTypes().isEmpty();
    }

    public boolean isSetObservationTypeFilter() {
        return !parameters.getObservationTypes().isEmpty();
    }

    public boolean isSetValueTypeFilter() {
        return !parameters.getValueTypes().isEmpty();
    }

    public Collection<String> getValueTypes() {
        return parameters.getValueTypes();
    }

    public boolean checkTranslationForLocale(Criteria criteria) {
        return !criteria.add(Restrictions.like(PROPERTY_LOCALE, getCountryCode())).list().isEmpty();
    }

    public Criteria addLocaleTo(Criteria criteria, Class<?> clazz) {
        return addLocaleTo(criteria, clazz, null);
    }

    public Criteria addLocaleTo(Criteria criteria, Class<?> clazz, String path) {
        if (getLocale() != null && DataModelUtil.isEntitySupported(clazz, criteria)) {
            Criteria translations = criteria.createCriteria(
                    path != null && !path.isEmpty() ? path + "." + PROPERTY_TRANSLATIONS : PROPERTY_TRANSLATIONS,
                    JoinType.LEFT_OUTER_JOIN);
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
        if (getParameters().getServices() != null && !getParameters().getServices().isEmpty()) {
            return criteria
                    .add(Restrictions.in(PROPERTY_SERVICE_ID, QueryUtils.parseToIds(getParameters().getServices())));
        }
        return criteria;
    }

    public Criteria addOdataFilterForData(Criteria criteria) {
        FESCriterionGenerator generator =
                new DataFESCriterionGenerator(criteria, true, isMatchDomainIds(), isComplexParent());
        return addOdataFilter(generator, criteria);
    }

    public Criteria addOdataFilterForDataset(Criteria criteria) {
        FESCriterionGenerator generator =
                new DatasetFESCriterionGenerator(criteria, true, isMatchDomainIds(), isComplexParent());
        return addOdataFilter(generator, criteria);
    }

    private Criteria addOdataFilter(FESCriterionGenerator generator, Criteria criteria) {
        return parameters.getODataFilter().map(generator::create).map(criteria::add).orElse(criteria);
    }

    private Criteria addLimitAndOffsetFilter(Criteria criteria) {
        if (getParameters().containsParameter(Parameters.OFFSET)) {
            int limit =
                    (getParameters().containsParameter(Parameters.LIMIT)) ? getParameters().getLimit() : DEFAULT_LIMIT;
            limit = (limit > 0) ? limit : DEFAULT_LIMIT;
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
        Set<String> tags = parameters.getTags();

        Set<String> samplings = parameters.getSamplings();
        Set<String> measuringPrograms = parameters.getMeasuringPrograms();

        boolean samplingSupported = DataModelUtil.isEntitySupported(SamplingEntity.class, criteria)
                ? hasValues(samplings) || hasValues(measuringPrograms)
                : false;

        if (!(hasValues(platforms) || hasValues(phenomena) || hasValues(procedures) || hasValues(offerings)
                || hasValues(features) || hasValues(categories) || hasValues(datasets) || hasValues(tags)
                || samplingSupported)) {
            // no subquery neccessary
            return criteria;
        }

        DetachedCriteria filter = DetachedCriteria.forClass(DatasetEntity.class);
        // if (hasValues(platforms)) {
        // features.addAll(getStationaryIds(platforms));
        // procedures.addAll(getMobileIds(platforms));
        // }

        addFilterRestriction(phenomena, DatasetEntity.PROPERTY_PHENOMENON, filter);
        // FIXME check for simple or full db models

        addProcedureRestriction(procedures, filter, criteria);
        addOfferingRestriction(offerings, filter, criteria);
        addFeatureRestriction(features, filter);
        addFilterRestriction(categories, DatasetEntity.PROPERTY_CATEGORY, filter);
        addFilterRestriction(platforms, DatasetEntity.PROPERTY_PLATFORM, filter);
        if (samplingSupported) {
            if (hasValues(samplings)) {
                addFilterRestriction(samplings, DatasetEntity.PROPERTY_SAMPLING_PROFILE + "."
                        + SamplingProfileDatasetEntity.PROPERTY_SAMPLINGS, filter);
            }
            if (hasValues(measuringPrograms)) {
                addFilterRestriction(measuringPrograms, DatasetEntity.PROPERTY_SAMPLING_PROFILE + "."
                        + SamplingProfileDatasetEntity.PROPERTY_MEASURING_PROGRAMS, filter);
            }
        }
        addFilterRestriction(tags, DatasetEntity.PROPERTY_TAGS, filter);

        addFilterRestriction(datasets, filter);

        // TODO refactory/simplify projection
        String projectionProperty = QueryUtils.createAssociation(datasetName, PROPERTY_ID);
        filter.setProjection(Property.forName(projectionProperty));

        String filterProperty = QueryUtils.createAssociation(datasetName, PROPERTY_ID);
        criteria.add(Subqueries.propertyIn(filterProperty, filter));
        return criteria;
    }

    private void addProcedureRestriction(Set<String> procedures, DetachedCriteria filter, Criteria criteria) {
        if (isIncludeHierarchy() && DataModelUtil.isPropertyNameSupported(ProcedureEntity.PROPERTY_PARENTS,
                ProcedureEntity.class, criteria)) {
            addHierarchicalFilterRestriction(procedures, DatasetEntity.PROPERTY_PROCEDURE, filter, PROCEDURE_ALIAS);
        } else {
            addFilterRestriction(procedures, DatasetEntity.PROPERTY_PROCEDURE, filter);
        }
    }

    private void addOfferingRestriction(Set<String> offerings, DetachedCriteria filter, Criteria criteria) {
        if (isIncludeHierarchy() && DataModelUtil.isPropertyNameSupported(OfferingEntity.PROPERTY_PARENTS,
                OfferingEntity.class, criteria)) {
            addHierarchicalFilterRestriction(offerings, DatasetEntity.PROPERTY_OFFERING, filter, OFFERING_ALIAS);
        } else {
            addFilterRestriction(offerings, DatasetEntity.PROPERTY_OFFERING, filter);
        }
    }

    private void addFeatureRestriction(Set<String> features, DetachedCriteria filter) {
        if (isIncludeHierarchy()) {
            addHierarchicalFilterRestriction(features, DatasetEntity.PROPERTY_FEATURE, filter, FEATURE_ALIAS);
        } else {
            addFilterRestriction(features, DatasetEntity.PROPERTY_FEATURE, filter);
        }
    }

    private DetachedCriteria addHierarchicalFilterRestriction(Set<String> values, String entity,
            DetachedCriteria filter, String prefix) {
        if (hasValues(values)) {
            filter.createCriteria(entity, prefix + "e")
                    // join the parents to enable filtering via parent ids
                    .createAlias(prefix + "e.parents", prefix + "p", JoinType.LEFT_OUTER_JOIN)
                    .add(Restrictions.or(createIdCriterion(values, prefix + "e"),
                            Restrictions.in(prefix + "p.id", QueryUtils.parseToIds(values))));
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
                return filter.createCriteria(entity).add(restriction);
            }
        }
        return filter;
    }

    private Criterion createIdCriterion(Set<String> values) {
        return createIdCriterion(values, null);
    }

    private Criterion createIdCriterion(Set<String> values, String alias) {
        return parameters.isMatchDomainIds() ? createDomainIdFilter(values, alias) : createIdFilter(values, alias);
    }

    private Criterion createDomainIdFilter(Set<String> filterValues, String alias) {
        String column = QueryUtils.createAssociation(alias, DescribableEntity.PROPERTY_DOMAIN_ID);
        return filterValues.stream().map(filter -> Restrictions.ilike(column, filter))
                .collect(Restrictions::disjunction, Disjunction::add, (a, b) -> b.conditions().forEach(a::add));
    }

    public Criterion createIdFilter(Set<String> filterValues, String alias) {
        String column = QueryUtils.createAssociation(alias, PROPERTY_ID);
        return Restrictions.in(column, QueryUtils.parseToIds(filterValues));
    }

    private boolean hasValues(Set<String> values) {
        return values != null && !values.isEmpty();
    }

    public Criteria addResultTimeFilter(Criteria criteria) {
        if (parameters.shallClassifyByResultTimes()) {
            criteria.add(parameters.getResultTimes().stream().map(Instant::parse).map(Instant::toDate)
                    .map(x -> Restrictions.eq(DataEntity.PROPERTY_RESULT_TIME, x))
                    .collect(Restrictions::disjunction, Disjunction::add, (a, b) -> b.conditions().forEach(a::add)));
        }
        return criteria;
    }

    public DetachedCriteria addResultTimeFilter(DetachedCriteria criteria) {
        if (parameters.shallClassifyByResultTimes()) {
            criteria.add(parameters.getResultTimes().stream()
                    .map(Instant::parse).map(Instant::toDate)
                    .map(x -> Restrictions.eq(DataEntity.PROPERTY_RESULT_TIME, x))
                    .collect(Restrictions::disjunction, Disjunction::add,
                             (a, b) -> b.conditions().forEach(a::add)));
        }
        return criteria;
    }

    public Criteria addSpatialFilter(Criteria criteria) {
        Criterion filter = createSpatialFilter();
        return filter != null ? criteria.add(filter) : criteria;
    }

    public DetachedCriteria addSpatialFilter(DetachedCriteria criteria) {
        Criterion filter = createSpatialFilter();
        return filter != null ? criteria.add(filter) : criteria;
    }

    public Criterion createSpatialFilter() {
        Geometry envelope = getSpatialFilter();
        if (envelope != null) {
            String geometryMember = DataEntity.PROPERTY_GEOMETRY_ENTITY + ".geometry";
            return SpatialRestrictions.intersects(geometryMember, envelope);

            // TODO intersect with linestring
            // XXX do sampling filter only on generated line strings stored in
            // FOI table,
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

    public DbQuery withoutSelectFilter() {
        return new DbQuery(parameters.removeAllOf(Parameters.SELECT));
    }

    public DbQuery withSubSelectFilter(String selection) {
        return new DbQuery(parameters.withSubSelectFilter(selection));
    }

    public boolean expandWithNextValuesBeyondInterval() {
        return parameters.isExpandWithNextValuesBeyondInterval();
    }

    /**
     * @return the includeHierarchy
     */
    public boolean isIncludeHierarchy() {
        return includeHierarchy;
    }

    /**
     * @param includeHierarchy
     *            the includeHierarchy to set
     * @return this
     */
    public DbQuery setIncludeHierarchy(boolean includeHierarchy) {
        this.includeHierarchy = includeHierarchy;
        return this;
    }

    public Criteria getDatasetSubCriteria(Criteria criteria, String key, String alias) {
        if (!subCriteria.containsKey(key)) {
            subCriteria.put(key, criteria.createCriteria(key, alias));
         }
         return subCriteria.get(key);
    }

    public Set<String> getSelectOriginal() {
        return parameters.getSelectOriginal();
    }

    public boolean isFormatToUnixTime() {
        return formatToUnixTime;
    }

    @SuppressFBWarnings({ "EI_EXPOSE_REP" })
    public NumberFormat getNumberFormat() {
        if (numberFormat == null) {
            this.numberFormat = NumberFormat.getInstance(LocaleHelper.decode(getLocale()));
        }
        return numberFormat;
    }

}
