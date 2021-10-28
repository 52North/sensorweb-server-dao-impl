/*
 * Copyright (C) 2015-2021 52°North Spatial Information Research GmbH
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
package org.n52.series.db.da;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.hibernate.Session;
import org.n52.io.request.IoParameters;
import org.n52.io.response.dataset.AbstractValue;
import org.n52.io.response.dataset.DatasetOutput;
import org.n52.io.response.dataset.DatasetTypesMetadata;
import org.n52.series.db.DataRepositoryTypeFactory;
import org.n52.series.db.beans.DatasetEntity;
import org.n52.series.db.beans.DescribableEntity;
import org.n52.series.db.da.mapper.DatasetMapper;
import org.n52.series.db.dao.DatasetDao;
import org.n52.series.db.dao.DbQuery;
import org.n52.series.spi.search.DatasetSearchResult;
import org.n52.series.spi.search.SearchResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * TODO: JavaDoc
 *
 * @author <a href="mailto:h.bredel@52north.org">Henning Bredel</a>
 * @param <V>
 *            the datasets type this repository is responsible for.
 */
public class DatasetRepository<V extends AbstractValue<?>> extends SessionAwareRepository
        implements OutputAssembler<DatasetOutput<V>> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatasetRepository.class);

    @Autowired
    private DataRepositoryTypeFactory dataRepositoryFactory;

    @Override
    public boolean exists(String id, DbQuery query) {
        Session session = getSession();
        try {
            // String handleAsFallback = query.getHandleAsValueTypeFallback();
            // String valueType = ValueType.extractType(id, handleAsFallback);
            //
            // if (!dataRepositoryFactory.isKnown(valueType)) {
            // return false;
            // }
            Class<? extends DatasetEntity> datasetEntityType = DatasetEntity.class;
            DatasetDao<? extends DatasetEntity> dao = getDatasetDao(datasetEntityType, session);

            IoParameters parameters = query.getParameters();
            return parameters.isMatchDomainIds() ? dao.hasInstance(id, query, datasetEntityType)
                    : dao.hasInstance(parseId(id), query, datasetEntityType);
        } finally {
            returnSession(session);
        }
    }

    @Override
    public List<DatasetOutput<V>> getAllCondensed(DbQuery query) {
        Session session = getSession();
        try {
            return getAllCondensed(query, session);
        } finally {
            returnSession(session);
        }
    }

    @Override
    public List<DatasetOutput<V>> getAllCondensed(DbQuery query, Session session) {
        List<DatasetOutput<V>> results = new ArrayList<>();
        // FilterResolver filterResolver = query.getFilterResolver();
        if (query.getParameters().isMatchDomainIds()) {
            // String valueType = query.getHandleAsValueTypeFallback();
            Class<? extends DatasetEntity> datasetEntityType = DatasetEntity.class;
            addCondensedResults(getDatasetDao(datasetEntityType, session), query, results, session);
            return results;
        }

        // if (filterResolver.shallIncludeAllDatasetTypes()) {
        addCondensedResults(getDatasetDao(DatasetEntity.class, session), query, results, session);
        // } else {
        // for (String valueType : query.getValueTypes()) {
        // addCondensedResults(getDatasetDao(valueType, session), query, results, session);
        // }
        // }
        return results;
    }

    private void addCondensedResults(DatasetDao<? extends DatasetEntity> dao, DbQuery query,
            List<DatasetOutput<V>> results, Session session) {
        long start = System.currentTimeMillis();
        // if (dao.isTimeseriesSimpleQuantityCount(query.getParameters())) {
        // dao.getAllInstances(query).parallelStream()
        // .filter(dataset -> dataRepositoryFactory.isKnown(dataset.getObservationType().name(),
        // dataset.getValueType().name()))
        // .map(dataset -> createCondensed(dataset, query)).forEach(results::add);
        // } else {
        DatasetMapper<V> datasetMapper = getMapperFactory().getDatasetMapper(query.getParameters());
        for (DatasetEntity dataset : dao.getAllInstances(query)) {
            if (dataRepositoryFactory.isKnown(dataset.getObservationType().name(), dataset.getValueType().name())) {
                results.add(datasetMapper.createCondensed(dataset, query));
            }
        }
        // }
        LOGGER.debug("Processing all condensed instances takes {} ms", System.currentTimeMillis() - start);
    }

    private DatasetDao<? extends DatasetEntity> getDatasetDao(Class<? extends DatasetEntity> clazz, Session session) {
        return new DatasetDao<>(session, clazz);
    }

    @Override
    public List<DatasetOutput<V>> getAllExpanded(DbQuery query) {
        Session session = getSession();
        try {
            return getAllExpanded(query, session);
        } finally {
            returnSession(session);
        }
    }

    @Override
    public List<DatasetOutput<V>> getAllExpanded(DbQuery query, Session session) {
        List<DatasetOutput<V>> results = new ArrayList<>();
        // FilterResolver filterResolver = query.getFilterResolver();
        if (query.getParameters().isMatchDomainIds()) {
            // String valueType = query.getHandleAsValueTypeFallback();
            addExpandedResults(getDatasetDao(DatasetEntity.class, session), query, results, session);
            return results;
        }

        // if (filterResolver.shallIncludeAllDatasetTypes()) {
        addExpandedResults(getDatasetDao(DatasetEntity.class, session), query, results, session);
        // } else {
        // for (String valueType : query.getValueTypes()) {
        // addExpandedResults(getDatasetDao(valueType, session), query, results, session);
        // }
        // }
        return results;
    }

    private void addExpandedResults(DatasetDao<? extends DatasetEntity> dao, DbQuery query,
            List<DatasetOutput<V>> results, Session session) {
        long start = System.currentTimeMillis();
        // if (dao.isTimeseriesSimpleQuantityCount(query.getParameters())) {
        // dao.getAllInstances(query).parallelStream()
        // .filter(dataset -> dataRepositoryFactory.isKnown(dataset.getObservationType().name(),
        // dataset.getValueType().name()))
        // .map(dataset -> createExpanded(dataset, query, session)).forEach(results::add);
        // } else {
        DatasetMapper<V> datasetMapper = getMapperFactory().getDatasetMapper(query.getParameters());
        for (DatasetEntity dataset : dao.getAllInstances(query)) {
            if (dataRepositoryFactory.isKnown(dataset.getObservationType().name(), dataset.getValueType().name())) {
                try {
                    results.add(datasetMapper.createExpanded(dataset, query, session));
                } catch (Exception e) {
                    LOGGER.error("Error while processing dataset {}! Exception: {}", dataset.getId(), e);
                }
            }
        }
        // }
        LOGGER.debug("Processing all expanded instances takes {} ms", System.currentTimeMillis() - start);
    }

    @Override
    public DatasetOutput<V> getInstance(String id, DbQuery query) {
        Session session = getSession();
        try {
            return getInstance(id, query, session);
        } finally {
            returnSession(session);
        }
    }

    @Override
    public DatasetOutput<V> getInstance(String id, DbQuery query, Session session) {
        return (DatasetOutput<V>) getMapperFactory().getDatasetMapper(query.getParameters())
                .createExpanded(getInstanceEntity(id, query, session), query, session);
    }

    DatasetEntity getInstanceEntity(String id, DbQuery query, Session session) {
        DatasetDao<? extends DatasetEntity> dao = getDatasetDao(DatasetEntity.class, session);
        DatasetEntity instance = dao.getInstance(Long.parseLong(id), query);
        return instance;
    }

    @Override
    public Collection<SearchResult> searchFor(IoParameters paramters) {
        Session session = getSession();
        try {
            DatasetDao<? extends DatasetEntity> dao = getDatasetDao(DatasetEntity.class, session);
            DbQuery query = getDbQuery(paramters);
            List<? extends DatasetEntity> found = dao.find(query);
            return convertToSearchResults(found, query);
        } finally {
            returnSession(session);
        }
    }

    public List<SearchResult> convertToSearchResults(List<? extends DescribableEntity> found, DbQuery query) {
        String locale = query.getLocaleForLabel();
        String hrefBase = query.getHrefBase();
        List<SearchResult> results = new ArrayList<>();
        for (DescribableEntity searchResult : found) {
            String id = searchResult.getId().toString();
            String label = searchResult.getLabelFrom(locale);
            results.add(new DatasetSearchResult().setId(id).setLabel(label).setBaseUrl(hrefBase));
        }
        return results;
    }

    // @SuppressWarnings("unchecked")
    // protected DatasetOutput<V> createCondensed(DatasetEntity dataset, DbQuery query) {
    // return (DatasetOutput<V>) getMapperFactory().getDatasetMapper().createCondensed(dataset, query);
    // }
    //
    // @SuppressWarnings("unchecked")
    // protected DatasetOutput<V> createExpanded(DatasetEntity dataset, DbQuery query, Session session) {
    // return (DatasetOutput<V>) getMapperFactory().getDatasetMapper().createExpanded(dataset, query,
    // session);
    // }

    public DataRepositoryTypeFactory getDataRepositoryTypeFactory() {
        return dataRepositoryFactory;
    }

    public void setDataRepositoryTypeFactory(DataRepositoryTypeFactory dataRepositoryTypeFactory) {
        this.dataRepositoryFactory = dataRepositoryTypeFactory;
    }

    public List<DatasetTypesMetadata> getDatasetTypesMetadata(IoParameters parameters) {
        Session session = getSession();
        try {
            return getDatasetDao(DatasetEntity.class, session).getDatasetTypesMetadata(parameters.getDatasets(),
                    getDbQuery(parameters));
        } finally {
            returnSession(session);
        }
    }

}
