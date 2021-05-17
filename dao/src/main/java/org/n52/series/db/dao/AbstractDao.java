/*
 * Copyright (C) 2015-2021 52°North Initiative for Geospatial Open Source
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

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.geolatte.geom.GeometryType;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Conjunction;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.ProjectionList;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hibernate.criterion.SimpleExpression;
import org.hibernate.criterion.Subqueries;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.CriteriaImpl;
import org.hibernate.loader.criteria.CriteriaJoinWalker;
import org.hibernate.loader.criteria.CriteriaQueryTranslator;
import org.hibernate.persister.entity.OuterJoinLoadable;
import org.hibernate.transform.RootEntityResultTransformer;
import org.n52.io.request.FilterResolver;
import org.n52.io.request.IoParameters;
import org.n52.io.request.Parameters;
import org.n52.series.db.DataAccessException;
import org.n52.series.db.DataModelUtil;
import org.n52.series.db.beans.DatasetEntity;
import org.n52.series.db.beans.DescribableEntity;
import org.n52.series.db.beans.dataset.DatasetType;
import org.n52.series.db.beans.dataset.ObservationType;
import org.n52.series.db.beans.dataset.ValueType;
import org.n52.series.db.beans.i18n.I18nEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractDao<T> implements GenericDao<T, Long> {

    protected static final String TRANSLATIONS_ALIAS = "translations";
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractDao.class);

    protected final Session session;

    public AbstractDao(Session session) {
        if (session == null) {
            throw new NullPointerException("Cannot operate on a null session.");
        }
        this.session = session;
    }

    protected abstract Class<T> getEntityClass();

    protected abstract String getDatasetProperty();

    protected String getDefaultAlias() {
        return getDatasetProperty();
    }

    public boolean hasInstance(String id, DbQuery query) throws DataAccessException {
        return getInstance(id, query) != null;
    }

    public boolean hasInstance(String id, DbQuery query, Class<?> clazz) throws DataAccessException {
        return getInstance(id, query) != null;
    }

    @Override
    public boolean hasInstance(Long id, DbQuery query) {
        return hasInstance(id, query, getEntityClass());
    }

    public boolean hasInstance(Long id, DbQuery query, Class<?> clazz) {
        return getInstance(id, query) != null;
    }

    public T getInstance(String key, DbQuery query) throws DataAccessException {
        return getInstance(key, query, getEntityClass());
    }

    @Override
    public T getInstance(Long key, DbQuery query) throws DataAccessException {
        LOGGER.debug("get instance '{}': {}", key, query);
        return getInstance(Long.toString(key), query, getEntityClass());
    }

    protected T getInstance(String key, DbQuery query, Class<T> clazz) {
        LOGGER.debug("get instance for '{}'. {}", key, query);
        Criteria criteria = getDefaultCriteria(query, clazz);
        return getInstance(key, query, clazz, criteria);
    }

    protected T getInstance(String key, DbQuery query, Class<T> clazz, Criteria criteria) {
        Criteria instanceCriteria =
                query.isMatchDomainIds() ? criteria.add(Restrictions.eq(DescribableEntity.PROPERTY_DOMAIN_ID, key))
                        : criteria.add(Restrictions.eq(DescribableEntity.PROPERTY_ID, Long.parseLong(key)));
        addFetchModes(instanceCriteria, query, true);
        return clazz.cast(instanceCriteria.uniqueResult());
    }

    @Override
    public Long getCount(DbQuery query) throws DataAccessException {
        if (!DataModelUtil.isEntitySupported(getEntityClass(), session)) {
            return 0L;
        }
        Criteria criteria = getDefaultCriteria(query).setProjection(Projections.rowCount());
        Object result = criteria.uniqueResult();
        if (result == null) {
            String sql = DataModelUtil.getSqlString(criteria);
            LOGGER.error("Please review query: {}", sql);
            return 0L;
        }
        return (Long) result;
    }

    protected <I extends I18nEntity> Criteria i18n(Class<I> clazz, Criteria criteria, DbQuery query) {
        return i18n(clazz, criteria, query, null);
    }

    protected <I extends I18nEntity> Criteria i18n(Class<I> clazz, Criteria criteria, DbQuery query, String path) {
        return !query.isDefaultLocal() && hasTranslation(query, clazz) ? query.addLocaleTo(criteria, clazz, path)
                : criteria;
    }

    private <I extends I18nEntity> boolean hasTranslation(DbQuery parameters, Class<I> clazz) {
        Criteria i18nCriteria = session.createCriteria(clazz);
        return parameters.checkTranslationForLocale(i18nCriteria);
    }

    public Criteria getDefaultCriteria(DbQuery query) {
        return getDefaultCriteria((String) null, query);
    }

    public Criteria getDefaultCriteria(String alias, DbQuery query) {
        return getDefaultCriteria(alias, query, getEntityClass());
    }

    private Criteria getDefaultCriteria(DbQuery query, Class<?> clazz) {
        return getDefaultCriteria((String) null, query, clazz);
    }

    protected Criteria getDefaultCriteria(String alias, DbQuery query, Class<?> clazz) {
        String nonNullAlias = alias != null ? alias : getDefaultAlias();
        Criteria criteria = session.createCriteria(clazz, nonNullAlias);
        criteria.setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY);
        addDatasetFilters(query, criteria);
        addMobileInsituFilter(getDatasetProperty(), criteria, query);
        addDatasetTypesFilter(getDatasetProperty(), criteria, query);
        // addGeometryTypeFilter(query, criteria);
        return criteria;
    }

    protected Criteria addDatasetFilters(DbQuery query, Criteria criteria) {
        DetachedCriteria filter = createDatasetSubqueryViaExplicitJoin(query);
        return criteria.add(Subqueries.propertyIn(DescribableEntity.PROPERTY_ID, filter));
    }

    private DetachedCriteria createDatasetSubqueryViaExplicitJoin(DbQuery query) {
        DetachedCriteria subquery = DetachedCriteria.forClass(DatasetEntity.class).add(createPublishedDatasetFilter());
        if (query.getLastValueMatches() != null) {
            subquery.add(createLastValuesFilter(query));
        }
        if (getDatasetProperty().equalsIgnoreCase(DatasetEntity.PROPERTY_FEATURE)) {
            DetachedCriteria featureCriteria = addSpatialFilter(query, subquery);
            return featureCriteria.setProjection(Projections.property(DescribableEntity.PROPERTY_ID));
        } else {
            addSpatialFilter(query, subquery);
            return subquery.createCriteria(getDatasetProperty())
                    .setProjection(Projections.property(DescribableEntity.PROPERTY_ID));

        }
    }

    protected final Criterion createLastValuesFilter(DbQuery query) {
        return Restrictions.between(DatasetEntity.PROPERTY_LAST_VALUE_AT,
                query.getLastValueMatches().getStart().toDate(), query.getLastValueMatches().getEnd().toDate());
    }

    protected final Conjunction createPublishedDatasetFilter() {
        return Restrictions.and(Restrictions.eq(DatasetEntity.PROPERTY_PUBLISHED, true),
                Restrictions.eq(DatasetEntity.PROPERTY_DELETED, false),
                Restrictions.isNotNull(DatasetEntity.PROPERTY_FIRST_VALUE_AT),
                Restrictions.isNotNull(DatasetEntity.PROPERTY_LAST_VALUE_AT));
    }

    /**
     * @param query
     *            the query instance
     * @param criteria
     *            the current detached criteria
     * @return the detached criteria for chaining
     */
    protected DetachedCriteria addSpatialFilter(DbQuery query, DetachedCriteria criteria) {
        return query.addSpatialFilter(criteria.createCriteria(DatasetEntity.PROPERTY_FEATURE));
        // return query.addSpatialFilter(criteria);
    }

    protected Criteria addSpatialFilter(DbQuery query, Criteria criteria) {
        return query.addSpatialFilter(
                query.getDatasetSubCriteria(criteria, DatasetEntity.PROPERTY_FEATURE, query.FEATURE_ALIAS));
        // return query.addSpatialFilter(criteria);
    }

    protected Criteria addDatasetTypesFilter(String parameter, Criteria criteria, DbQuery query) {
        IoParameters parameters = query.getParameters();
        Set<String> datasetTypes = parameters.getDatasetTypes();
        Set<String> observationsTypes = parameters.getObservationTypes();
        Set<String> valueTypes = parameters.getValueTypes();
        if (!datasetTypes.isEmpty() || !observationsTypes.isEmpty() || !valueTypes.isEmpty()) {
            FilterResolver filterResolver = parameters.getFilterResolver();
            if (parameters.shallBehaveBackwardsCompatible() || !filterResolver.shallIncludeAllDatasetTypes()) {
                Criterion containsDatasetType = !datasetTypes.isEmpty()
                        ? Restrictions.in(DatasetEntity.PROPERTY_DATASET_TYPE, DatasetType.convert(datasetTypes))
                        : null;
                Criterion containsObservationType =
                        !observationsTypes.isEmpty() ? Restrictions.in(DatasetEntity.PROPERTY_OBSERVATION_TYPE,
                                ObservationType.convert(observationsTypes)) : null;
                Criterion containsValueType = !valueTypes.isEmpty()
                        ? Restrictions.in(DatasetEntity.PROPERTY_VALUE_TYPE, ValueType.convert(valueTypes))
                        : null;
                if (parameter == null || parameter.isEmpty()) {
                    // series table itself
                    if (containsDatasetType != null) {
                        criteria.add(containsDatasetType);
                    }
                    if (containsObservationType != null) {
                        criteria.add(containsObservationType);
                    }
                    if (containsValueType != null) {
                        criteria.add(containsValueType);
                    }
                } else {
                    ProjectionList onPkids = matchPropertyPkids(DatasetEntity.ENTITY_ALIAS, parameter);
                    DetachedCriteria c = DetachedCriteria.forClass(DatasetEntity.class, DatasetEntity.ENTITY_ALIAS);
                    if (containsDatasetType != null) {
                        c.add(containsDatasetType);
                    }
                    if (containsObservationType != null) {
                        c.add(containsObservationType);
                    }
                    if (containsValueType != null) {
                        c.add(containsValueType);
                    }
                    c.setProjection(onPkids);
                    criteria.add(matchPropertyPkids(parameter, c));
                }
            }
        }
        return criteria;
    }

    protected Criteria addMobileInsituFilter(String parameter, Criteria criteria, DbQuery query) {
        IoParameters parameters = query.getParameters();
        FilterResolver filterResolver = parameters.getFilterResolver();
        if (!filterResolver.shallIncludeAllDatasets()) {
            SimpleExpression mobileExpression = createMobileExpression(filterResolver);
            SimpleExpression insituExpression = createInsituExpression(filterResolver);
            if (parameter == null) {
                // apply filter directly on table table
                if (mobileExpression != null) {
                    criteria.add(mobileExpression);
                }
                if (insituExpression != null) {
                    criteria.add(insituExpression);
                }
            } else {
                // apply filter on dataset table
                DetachedCriteria c = DetachedCriteria.forClass(DatasetEntity.class);
                if (mobileExpression != null) {
                    c.add(mobileExpression);
                }
                if (insituExpression != null) {
                    c.add(insituExpression);
                }

                QueryUtils.setFilterProjectionOn(parameter, c);
                criteria.add(Subqueries.propertyIn(DescribableEntity.PROPERTY_ID, c));
            }
        }
        return criteria;
    }

    private SimpleExpression createMobileExpression(FilterResolver filterResolver) {
        if (filterResolver.hasMobileFilter()) {
            boolean mobile = filterResolver.isMobileFilter();
            return Restrictions.eq(DatasetEntity.PROPERTY_MOBILE, mobile);
        }
        return null;
    }

    private SimpleExpression createInsituExpression(FilterResolver filterResolver) {
        if (filterResolver.hasInsituFilter()) {
            boolean insitu = filterResolver.isInsituFilter();
            return Restrictions.eq(DatasetEntity.PROPERTY_INSITU, insitu);
        }
        return null;
    }

    private ProjectionList matchPropertyPkids(String alias, String property) {
        String member = QueryUtils.createAssociation(alias, property);
        String association = QueryUtils.createAssociation(member, DescribableEntity.PROPERTY_ID);
        return Projections.projectionList().add(Projections.property(association));
    }

    private Criterion matchPropertyPkids(String property, DetachedCriteria c) {
        return Subqueries.propertyIn(QueryUtils.createAssociation(property, DescribableEntity.PROPERTY_ID), c);
    }

    // protected Criteria addGeometryTypeFilter(DbQuery query, Criteria criteria) {
    // query.getParameters()
    // .getGeometryTypes()
    // .stream()
    // .filter(geometryType -> !geometryType.isEmpty())
    // .map(this::getGeometryType)
    // .filter(Objects::nonNull)
    // .map(type -> SpatialRestrictions.geometryType(DataEntity.PROPERTY_GEOMETRY_ENTITY, type))
    // .forEach(criteria::add);
    // return criteria;
    // }

    private GeometryType getGeometryType(String geometryType) {
        return Arrays.stream(GeometryType.values()).filter(type -> type.name().equalsIgnoreCase(geometryType))
                .findAny().orElse(null);
    }

    protected DbQuery checkLevelParameterForHierarchyQuery(DbQuery query) {
        IoParameters params = null;
        if (query.getLevel() != null) {
            if (query.getParameters().containsParameter(Parameters.FEATURES) && !(this instanceof FeatureDao)) {
                Collection<Long> ids = new FeatureDao(session).getChildrenIds(query);
                if (ids != null && !ids.isEmpty()) {
                    params = query.getParameters().extendWith(Parameters.FEATURES, toStringList(ids));
                }
            } else if (query.getParameters().containsParameter(Parameters.PROCEDURES)
                    && !(this instanceof ProcedureDao)) {
                Collection<Long> ids = new ProcedureDao(session).getChildrenIds(query);
                if (ids != null && !ids.isEmpty()) {
                    params = query.getParameters().extendWith(Parameters.PROCEDURES, toStringList(ids));
                }
            }
        }
        return params != null ? new DbQuery(params) : query;
    }

    protected List<String> toStringList(Collection<Long> set) {
        return set.stream().map(s -> s.toString()).collect(Collectors.toList());
    }

    private Criteria addFetchModes(Criteria criteria, DbQuery q) {
        return addFetchModes(criteria, q, q.isExpanded());
    }

    protected Criteria addFetchModes(Criteria criteria, DbQuery q, boolean instance) {
        return criteria;
    }

    protected String getFetchPath(String... values) {
        return String.join(".", values);
    }

    /**
     * Translate the {@link Criteria criteria} to SQL.
     *
     * @param criteria
     *            the criteria
     *
     * @return the SQL
     */
    public static String toSQLString(Criteria criteria) {
        if (!(criteria instanceof CriteriaImpl)) {
            return criteria.toString();
        }
        CriteriaImpl criteriaImpl = (CriteriaImpl) criteria;
        SharedSessionContractImplementor session = criteriaImpl.getSession();
        SessionFactoryImplementor factory = session.getFactory();
        String entityOrClassName = criteriaImpl.getEntityOrClassName();
        CriteriaQueryTranslator translator = new CriteriaQueryTranslator(factory, criteriaImpl, entityOrClassName,
                CriteriaQueryTranslator.ROOT_SQL_ALIAS);
        String[] implementors = factory.getImplementors(entityOrClassName);
        OuterJoinLoadable outerJoinLoadable = (OuterJoinLoadable) factory.getEntityPersister(implementors[0]);
        LoadQueryInfluencers loadQueryInfluencers = session.getLoadQueryInfluencers();
        CriteriaJoinWalker walker = new CriteriaJoinWalker(outerJoinLoadable, translator, factory, criteriaImpl,
                entityOrClassName, loadQueryInfluencers);
        return walker.getSQLString();
    }

    /**
     * Currently used in SOS cache operations.
     *
     * @param query
     *            Query parameters
     *
     * @return the result
     *
     * @deprecated Onlxy for SOS cache which might be deleted in the future
     */
    @SuppressWarnings("unchecked")
    @Deprecated
    public Collection<T> get(DbQuery query) {
        query.setIncludeHierarchy(false);
        Criteria c = session.createCriteria(getEntityClass(), getDefaultAlias())
                .setResultTransformer(RootEntityResultTransformer.INSTANCE);
        DetachedCriteria subquery = DetachedCriteria.forClass(getEntityClass());
        subquery.add(Restrictions.eq(DatasetEntity.PROPERTY_DELETED, false));
        query.addFilters(c, getDatasetProperty(), session);
        addFetchModes(c, query);
        return c.list();
    }
}
