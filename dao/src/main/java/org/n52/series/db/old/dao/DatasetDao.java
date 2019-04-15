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

package org.n52.series.db.old.dao;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hibernate.sql.JoinType;
import org.hibernate.transform.ResultTransformer;
import org.n52.series.db.DatasetTypesMetadata;
import org.n52.series.db.beans.DatasetEntity;
import org.n52.series.db.beans.DescribableEntity;
import org.n52.series.db.beans.FeatureEntity;
import org.n52.series.db.beans.IdEntity;
import org.n52.series.db.beans.ProcedureEntity;
import org.n52.series.db.beans.dataset.DatasetType;
import org.n52.series.db.beans.dataset.ObservationType;
import org.n52.series.db.beans.dataset.ValueType;
import org.n52.series.db.beans.i18n.I18nFeatureEntity;
import org.n52.series.db.beans.i18n.I18nOfferingEntity;
import org.n52.series.db.beans.i18n.I18nPhenomenonEntity;
import org.n52.series.db.beans.i18n.I18nProcedureEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public class DatasetDao<T extends DatasetEntity> extends AbstractDao<T> implements SearchableDao<T> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatasetDao.class);

    private static final String FEATURE_PATH_ALIAS = "dsFeature";

    private static final String PROCEDURE_PATH_ALIAS = "dsProcedure";

    private final Class<T> entityType;

    private final DatasetTypesMetadataTransformer transformer = new DatasetTypesMetadataTransformer();

    @SuppressWarnings("unchecked")
    public DatasetDao(Session session) {
        this(session, (Class<T>) DatasetEntity.class);
    }

    /**
     * Constructs a dao to access datasets of given type.
     *
     * @param session
     *        the session
     * @param clazz
     *        the dataset type
     */
    public DatasetDao(Session session, Class<T> clazz) {
        super(session);
        this.entityType = clazz;
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<T> find(DbQuery query) {
        LOGGER.debug("find entities: {}", query);

        String searchTerm = "%" + query.getSearchTerm() + "%";

        /*
         * Timeseries labels are constructed from labels of related feature and phenomenon. Therefore we have
         * to join tables and search for given pattern on any of the stored labels.
         */
        Criteria criteria = getDefaultCriteria(query);
        // default criteria performs join on procedure table

        criteria.add(Restrictions.or(Restrictions.ilike(DescribableEntity.PROPERTY_NAME, searchTerm),
                                     Restrictions.ilike(DescribableEntity.PROPERTY_NAME, searchTerm),
                                     Restrictions.ilike(DescribableEntity.PROPERTY_NAME, searchTerm),
                                     Restrictions.ilike(DescribableEntity.PROPERTY_NAME, searchTerm)));

        i18n(I18nOfferingEntity.class, criteria, query);
        i18n(I18nPhenomenonEntity.class, criteria, query);
        i18n(I18nProcedureEntity.class, criteria, query);
        i18n(I18nFeatureEntity.class, criteria, query);
        return criteria.list();
    }

    @Override
    @SuppressWarnings("unchecked")
    public T getInstance(Long key, DbQuery query) {
        Criteria criteria = getDefaultCriteria(getDefaultAlias(), false, query);
        return (T) criteria.add(Restrictions.eq(IdEntity.PROPERTY_ID, key))
                           .uniqueResult();
    }

    @Override
    protected T getInstance(String key, DbQuery query, Class<T> clazz) {
        return super.getInstance(key, query, clazz, getDefaultCriteria(null, false, query, clazz));
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<T> getAllInstances(DbQuery query) {
        LOGGER.debug("get all instances: {}", query);
        Criteria criteria = query.addFilters(getDefaultCriteria(query), getDatasetProperty());
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(toSQLString(criteria));
        }
        return criteria.list();
    }

    @SuppressWarnings("unchecked")
    public List<T> getInstancesWith(FeatureEntity feature, DbQuery query) {
        LOGGER.debug("get instance for feature '{}'", feature);
        Criteria criteria = getDefaultCriteria(query);
        String path = QueryUtils.createAssociation(FEATURE_PATH_ALIAS, DescribableEntity.PROPERTY_ID);
        return criteria.add(Restrictions.eq(path, feature.getId()))
                       .list();
    }

    @Override
    protected Class<T> getEntityClass() {
        return entityType;
    }

    @Override
    protected String getDatasetProperty() {
        // self has no property
        return "";
    }

    @Override
    protected String getDefaultAlias() {
        return DatasetEntity.ENTITY_ALIAS;
    }

    @Override
    protected Criteria getDefaultCriteria(String alias, DbQuery query, Class< ? > clazz) {
        // declare explicit alias here
        return getDefaultCriteria(alias, true, query, clazz);
    }

    private Criteria getDefaultCriteria(String alias, boolean ignoreReferenceSeries, DbQuery query) {
        return getDefaultCriteria(alias, ignoreReferenceSeries, query, getEntityClass());
    }

    private Criteria getDefaultCriteria(String alias, boolean ignoreReferenceSeries, DbQuery query, Class< ? > clazz) {
        Criteria criteria = super.getDefaultCriteria(alias, query, clazz);

        if (ignoreReferenceSeries) {
            criteria.createCriteria(DatasetEntity.PROPERTY_PROCEDURE, PROCEDURE_PATH_ALIAS, JoinType.LEFT_OUTER_JOIN)
                    .add(Restrictions.eq(ProcedureEntity.PROPERTY_REFERENCE, Boolean.FALSE));
        }

        query.addOdataFilterForDataset(criteria);

        return criteria;
    }

    @Override
    protected Criteria addDatasetFilters(DbQuery query, Criteria criteria) {
        // on dataset itself there is no explicit join neccessary
        Criteria filter = criteria.add(createPublishedDatasetFilter());
        query.addSpatialFilter(filter.createCriteria(DatasetEntity.PROPERTY_FEATURE,
                                                     FEATURE_PATH_ALIAS,
                                                     JoinType.LEFT_OUTER_JOIN));
        return criteria;
    }

    @SuppressWarnings("unchecked")
    public List<DatasetTypesMetadata> getDatasetTypesMetadata(Collection<String> datasets, DbQuery query) {
        Criteria criteria = getDefaultCriteria(getDefaultAlias(), false, query);
        if (query.isMatchDomainIds()) {
            criteria.add(Restrictions.in(DescribableEntity.PROPERTY_DOMAIN_ID, datasets));
        } else {
            criteria.add(Restrictions.in(DescribableEntity.PROPERTY_ID,
                    datasets.stream().map(d -> Long.parseLong(d)).collect(Collectors.toSet())));
        }
        criteria.setProjection(Projections.projectionList()
                .add(Projections.groupProperty(DatasetEntity.PROPERTY_ID))
                .add(Projections.property(DatasetEntity.PROPERTY_DATASET_TYPE))
                .add(Projections.property(DatasetEntity.PROPERTY_OBSERVATION_TYPE))
                .add(Projections.property(DatasetEntity.PROPERTY_VALUE_TYPE)));
        criteria.setResultTransformer(transformer);
        return criteria.list();
    }

    /**
     * Offering time extrema {@link ResultTransformer}
     *
     * @author <a href="mailto:c.hollmann@52north.org">Carsten Hollmann</a>
     * @since 4.4.0
     *
     */
    private class DatasetTypesMetadataTransformer implements ResultTransformer {
        private static final long serialVersionUID = -373512929481519459L;

        @Override
        public DatasetTypesMetadata transformTuple(Object[] tuple, String[] aliases) {
            DatasetTypesMetadata datasetTypesMetadata = new DatasetTypesMetadata();
            if (tuple != null) {
                datasetTypesMetadata.setId(tuple[0].toString());
                datasetTypesMetadata.setDatasetType(DatasetType.valueOf(tuple[1].toString()));
                datasetTypesMetadata.setObservationType(ObservationType.valueOf(tuple[2].toString()));
                datasetTypesMetadata.setValueType(ValueType.valueOf(tuple[3].toString()));
            }
            return datasetTypesMetadata;
        }

        @Override
        @SuppressWarnings({ "rawtypes"})
        public List transformList(List collection) {
            return collection;
        }
    }

}
