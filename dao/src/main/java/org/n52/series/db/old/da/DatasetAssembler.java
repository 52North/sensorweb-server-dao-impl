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

package org.n52.series.db.old.da;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.hibernate.Session;
import org.n52.io.HrefHelper;
import org.n52.io.request.FilterResolver;
import org.n52.io.request.IoParameters;
import org.n52.io.request.Parameters;
import org.n52.io.response.ParameterOutput;
import org.n52.io.response.PlatformOutput;
import org.n52.io.response.dataset.AbstractValue;
import org.n52.io.response.dataset.DatasetOutput;
import org.n52.io.response.dataset.DatasetParameters;
import org.n52.io.response.dataset.ReferenceValueOutput;
import org.n52.io.response.dataset.ValueType;
import org.n52.series.db.DataRepositoryTypeFactory;
import org.n52.series.db.ValueAssembler;
import org.n52.series.db.beans.AbstractFeatureEntity;
import org.n52.series.db.beans.DatasetEntity;
import org.n52.series.db.beans.DescribableEntity;
import org.n52.series.db.beans.OfferingEntity;
import org.n52.series.db.beans.PhenomenonEntity;
import org.n52.series.db.beans.ProcedureEntity;
import org.n52.series.db.beans.QuantityDatasetEntity;
import org.n52.series.db.old.HibernateSessionStore;
import org.n52.series.db.old.dao.DatasetDao;
import org.n52.series.db.old.dao.DbQuery;
import org.n52.series.db.old.dao.DbQueryFactory;
import org.n52.series.spi.search.DatasetSearchResult;
import org.n52.series.spi.search.SearchResult;
import org.n52.series.srv.OutputAssembler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author <a href="mailto:h.bredel@52north.org">Henning Bredel</a>
 */
@Component
public class DatasetAssembler<V extends AbstractValue<?>> extends SessionAwareAssembler
        implements
        OutputAssembler<DatasetOutput<V>> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatasetAssembler.class);

    private final DataRepositoryTypeFactory dataRepositoryFactory;

    @Autowired
    private PlatformAssembler platformRepository;

    public DatasetAssembler(DataRepositoryTypeFactory dataRepositoryFactory,
                            // PlatformRepository platformRepository,
                            HibernateSessionStore sessionStore,
                            DbQueryFactory dbQueryFactory) {
        super(sessionStore, dbQueryFactory);
        this.dataRepositoryFactory = dataRepositoryFactory;
        // this.platformRepository = platformRepository;
    }

    @Override
    public boolean exists(String id, DbQuery query) {
        Session session = getSession();
        try {
            String dbId = ValueType.extractId(id);
            String handleAsFallback = query.getHandleAsValueTypeFallback();
            String valueType = ValueType.extractType(id, handleAsFallback);
            if (!dataRepositoryFactory.isKnown(valueType)) {
                return false;
            }
            Class< ? extends DatasetEntity> entityType = dataRepositoryFactory.getDatasetEntityType(valueType);
            DatasetDao<? extends DatasetEntity> dao = new DatasetDao<>(session, entityType);
            IoParameters parameters = query.getParameters();
            return parameters.isMatchDomainIds()
                ? dao.hasInstance(dbId, query)
                : dao.hasInstance(parseId(dbId), query);
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

    private List<DatasetOutput<V>> getAllCondensed(DbQuery query, Session session) {
        List<DatasetOutput<V>> results = new ArrayList<>();
        FilterResolver filterResolver = query.getFilterResolver();
        if (query.getParameters().isMatchDomainIds()) {
            String valueType = query.getHandleAsValueTypeFallback();
            Class< ? extends DatasetEntity> entityType = dataRepositoryFactory.getDatasetEntityType(valueType);
            DatasetDao<? extends DatasetEntity> datasetDao = new DatasetDao<>(session, entityType);
            addCondensedResults(datasetDao, query, results, session);
            return results;
        }

        if (filterResolver.shallIncludeAllDatasetTypes()) {
            addCondensedResults(new DatasetDao<>(session, DatasetEntity.class), query, results, session);
        } else {
            List<String> valueTypes = query.getValueTypes()
                 .stream()
                 .filter(it -> !"all".equalsIgnoreCase(it))
                 .collect(Collectors.toList());
            DatasetDao<? extends DatasetEntity> dao = new DatasetDao<>(session);
            addCondensedResults(dao, query.replaceWith(Parameters.FILTER_VALUE_TYPES, valueTypes), results, session);
        }
        return results;
    }

    private void addCondensedResults(DatasetDao<? extends DatasetEntity> dao, DbQuery query, List<DatasetOutput<V>> results, Session session) {
        for (DatasetEntity series : dao.getAllInstances(query)) {
            if (dataRepositoryFactory.isKnown(series.getValueType())) {
                results.add(createCondensed(series, query, session));
            }
        }
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

    private List<DatasetOutput<V>> getAllExpanded(DbQuery query, Session session) {
        List<DatasetOutput<V>> results = new ArrayList<>();
        FilterResolver filterResolver = query.getFilterResolver();
        if (query.getParameters()
                 .isMatchDomainIds()) {
            String valueType = query.getHandleAsValueTypeFallback();
            Class< ? extends DatasetEntity> entityType = dataRepositoryFactory.getDatasetEntityType(valueType);
            addExpandedResults(new DatasetDao<>(session, entityType), query, results, session);
            return results;
        }

        if (filterResolver.shallIncludeAllDatasetTypes()) {
            addExpandedResults(new DatasetDao<QuantityDatasetEntity>(session), query, results, session);
        } else {
            for (String valueType : query.getValueTypes()) {
                Class< ? extends DatasetEntity> entityType = dataRepositoryFactory.getDatasetEntityType(valueType);
                addExpandedResults(new DatasetDao<>(session, entityType), query, results, session);
            }
        }
        return results;
    }

    private void addExpandedResults(DatasetDao<? extends DatasetEntity> dao, DbQuery query, List<DatasetOutput<V>> results, Session session) {
        for (DatasetEntity series : dao.getAllInstances(query)) {
            if (dataRepositoryFactory.isKnown(series.getValueType())) {
                results.add(createExpanded(series, query, session));
            }
        }
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

    private DatasetOutput<V> getInstance(String id, DbQuery query, Session session) {
        DatasetEntity instanceEntity = getInstanceEntity(id, query, session);
        return createExpanded(instanceEntity, query, session);
    }

    DatasetEntity getInstanceEntity(String id, DbQuery query, Session session) {
        String rawId = ValueType.extractId(id);

        String handleAsFallback = query.getHandleAsValueTypeFallback();
        final String valueType = ValueType.extractType(id, handleAsFallback);
        if (!dataRepositoryFactory.isKnown(valueType)) {
            LOGGER.debug("unknown type: " + valueType);
            return null;
        }

        Class<? extends DatasetEntity> datasetType = dataRepositoryFactory.getDatasetEntityType(valueType);
        DatasetDao<? extends DatasetEntity> dao = new DatasetDao<>(session, datasetType);
        DatasetEntity instance = dao.getInstance(Long.parseLong(rawId), query);
        instance.setPlatform(platformRepository.getPlatformEntity(instance, query, session));
        return instance;
    }

    @Override
    public Collection<SearchResult> searchFor(DbQuery query) {
        Session session = getSession();
        try {
            DatasetDao<DatasetEntity> dao = new DatasetDao<>(session);
            List< ? extends DatasetEntity> found = dao.find(query);
            return convertToSearchResults(found, query);
        } finally {
            returnSession(session);
        }
    }

    public List<SearchResult> convertToSearchResults(List< ? extends DescribableEntity> found, DbQuery query) {
        String locale = query.getLocale();
        String hrefBase = HrefHelper.constructHref(query.getHrefBase(), DatasetOutput.COLLECTION_PATH);
        List<SearchResult> results = new ArrayList<>();
        for (DescribableEntity searchResult : found) {
            String id = searchResult.getId()
                                    .toString();
            String label = searchResult.getLabelFrom(locale);
            results.add(new DatasetSearchResult(id, label, hrefBase));
        }
        return results;
    }

    protected DatasetOutput<V> createCondensed(DatasetEntity dataset, DbQuery query, Session session) {
        IoParameters parameters = query.getParameters();

        String valueType = dataset.getValueType();
        DatasetOutput<V> result = DatasetOutput.create(valueType, parameters);

        Long id = dataset.getId();
        String hrefBase = query.getHrefBase();
        String domainId = dataset.getIdentifier();
        String uom = dataset.getUnitI18nName(query.getLocale());
        String label = createDatasetLabel(dataset, query.getLocale());
        String platformtype = getCondensedPlatform(dataset, query.withoutFieldsFilter(), session).getPlatformType();

        result.setId(id.toString());
        result.setValue(DatasetOutput.UOM, uom, parameters, result::setUom);
        result.setValue(ParameterOutput.LABEL, label, parameters, result::setLabel);
        result.setValue(ParameterOutput.DOMAIN_ID, domainId, parameters, result::setDomainId);
        result.setValue(ParameterOutput.HREF_BASE, hrefBase, parameters, result::setHrefBase);
        result.setValue(DatasetOutput.PLATFORM_TYPE, platformtype, parameters, result::setPlatformType);
        return result;
    }

    protected DatasetOutput<V> createExpanded(DatasetEntity dataset, DbQuery query, Session session) {
        IoParameters params = query.getParameters();
        DatasetOutput<V> result = createCondensed(dataset, query, session);

        DatasetParameters datasetParams = createDatasetParameters(dataset, query.withoutFieldsFilter(), session);
        datasetParams.setPlatform(getCondensedPlatform(dataset, query, session));
        dataset.setService(getServiceEntity(dataset));

        ValueAssembler<DatasetEntity, ?, V, ?> assembler = dataRepositoryFactory.create(dataset.getValueType(), DatasetEntity.class);
        V firstValue = assembler.getFirstValue(dataset, query);
        V lastValue = assembler.getLastValue(dataset, query);

        List<ReferenceValueOutput<V>> refValues = assembler.getReferenceValues(dataset, query);
        lastValue = isReferenceSeries(dataset) && isCongruentValues(firstValue, lastValue)
            // ensure we have a valid interval
            ? firstValue
            : lastValue;

        result.setValue(DatasetOutput.REFERENCE_VALUES, refValues, params, result::setReferenceValues);
        result.setValue(DatasetOutput.DATASET_PARAMETERS, datasetParams, params, result::setDatasetParameters);
        result.setValue(DatasetOutput.FIRST_VALUE, firstValue, params, result::setFirstValue);
        result.setValue(DatasetOutput.LAST_VALUE, lastValue, params, result::setLastValue);

        return result;
    }

    private boolean isCongruentValues(AbstractValue< ? > firstValue, AbstractValue< ? > lastValue) {
        return ( (firstValue == null) && (lastValue == null))
                || ( (firstValue != null) && lastValue.getTimestamp().equals(firstValue.getTimestamp()))
                || ( (lastValue != null) && firstValue.getTimestamp().equals(lastValue.getTimestamp()));
    }

    private boolean isReferenceSeries(DatasetEntity series) {
        return series.getProcedure()
                     .isReference();
    }

    private PlatformOutput getCondensedPlatform(DatasetEntity dataset, DbQuery query, Session session) {
        // platform has to be handled dynamically (see #309)
        return platformRepository.createCondensedPlatform(dataset, query, session);
    }

    private String createDatasetLabel(DatasetEntity dataset, String locale) {
        PhenomenonEntity phenomenon = dataset.getPhenomenon();
        ProcedureEntity procedure = dataset.getProcedure();
        OfferingEntity offering = dataset.getOffering();
        AbstractFeatureEntity<?> feature = dataset.getFeature();

        String procedureLabel = procedure.getLabelFrom(locale);
        String phenomenonLabel = phenomenon.getLabelFrom(locale);
        String offeringLabel = offering.getLabelFrom(locale);
        String stationLabel = feature.getLabelFrom(locale);

        StringBuilder sb = new StringBuilder();
        return sb.append(phenomenonLabel)
                 .append(" ")
                 .append(procedureLabel)
                 .append(", ")
                 .append(stationLabel)
                 .append(", ")
                 .append(offeringLabel)
                 .toString();
    }

}
