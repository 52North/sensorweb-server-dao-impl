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
package org.n52.series.db.da;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.hibernate.Session;
import org.n52.io.HrefHelper;
import org.n52.io.request.IoParameters;
import org.n52.io.request.Parameters;
import org.n52.io.response.OptionalOutput;
import org.n52.io.response.ParameterOutput;
import org.n52.io.response.dataset.AbstractValue;
import org.n52.io.response.dataset.AggregationOutput;
import org.n52.io.response.dataset.DatasetOutput;
import org.n52.io.response.dataset.DatasetParameters;
import org.n52.io.response.dataset.IndividualObservationOutput;
import org.n52.io.response.dataset.ProfileOutput;
import org.n52.io.response.dataset.ReferenceValueOutput;
import org.n52.io.response.dataset.TimeseriesMetadataOutput;
import org.n52.io.response.dataset.TrajectoryOutput;
import org.n52.series.db.DataAccessException;
import org.n52.series.db.DataRepositoryTypeFactory;
import org.n52.series.db.DatasetTypesMetadata;
import org.n52.series.db.beans.AbstractFeatureEntity;
import org.n52.series.db.beans.DatasetEntity;
import org.n52.series.db.beans.DescribableEntity;
import org.n52.series.db.beans.OfferingEntity;
import org.n52.series.db.beans.PhenomenonEntity;
import org.n52.series.db.beans.ProcedureEntity;
import org.n52.series.db.beans.dataset.ValueType;
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
        for (DatasetEntity series : dao.getAllInstances(query)) {
            if (dataRepositoryFactory.isKnown(series.getObservationType().name(), series.getValueType().name())) {
                results.add(createCondensed(series, query));
            }
        }
        LOGGER.debug("Processing all condensed instances takes {} ms", System.currentTimeMillis() - start);
    }

    private DatasetDao<? extends DatasetEntity> getDatasetDao(Class<? extends DatasetEntity> clazz, Session session) {
        return new DatasetDao<>(session, clazz);
    }

    // private DatasetDao<? extends DatasetEntity> getDatasetDao(String valueType, Session session) {
    // // if (! ("all".equalsIgnoreCase(valueType) ||
    // // dataRepositoryFactory.isKnown(valueType))) {
    // // throw new BadQueryParameterException("invalid type: " + valueType);
    // // }
    // return createDataAccessRepository(DatasetEntity.class, session);
    // }

    private DatasetDao<? extends DatasetEntity> getSeriesDao(String datasetId, DbQuery query, Session session)
            throws DataAccessException {
        // String handleAsFallback = query.getHandleAsValueTypeFallback();
        // final String valueType = ValueType.extractType(datasetId,
        // handleAsFallback);
        Class<? extends DatasetEntity> datasetEntityType = DatasetEntity.class;
        // if (!dataRepositoryFactory.isKnown(datasetEntityType)) {
        // throw new ResourceNotFoundException("unknown type: " + valueType);
        // }
        return createDataAccessRepository(datasetEntityType, session);
    }

    private DatasetDao<? extends DatasetEntity> createDataAccessRepository(
            Class<? extends DatasetEntity> datasetEntityType, Session session) {
        return getDatasetDao(datasetEntityType, session);
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
        for (DatasetEntity dataset : dao.getAllInstances(query)) {
            if (dataRepositoryFactory.isKnown(dataset.getObservationType().name(), dataset.getValueType().name())) {
                try {
                    results.add(createExpanded(dataset, query, session));
                } catch (Exception e) {
                    LOGGER.error("Error while processing dataset {}! Exception: {}", dataset.getId(), e);
                }
            }
        }
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
        DatasetEntity instanceEntity = getInstanceEntity(id, query, session);
        return createExpanded(instanceEntity, query, session);
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
        String locale = query.getLocale();
        String hrefBase = query.getHrefBase();
        List<SearchResult> results = new ArrayList<>();
        for (DescribableEntity searchResult : found) {
            String id = searchResult.getId().toString();
            String label = searchResult.getLabelFrom(locale);
            results.add(new DatasetSearchResult().setId(id).setLabel(label).setBaseUrl(hrefBase));
        }
        return results;
    }

    protected DatasetOutput<V> createCondensed(DatasetEntity dataset, DbQuery query) {
        IoParameters parameters = query.getParameters();
        if (dataset.getService() == null) {
            dataset.setService(getServiceEntity());
        }
        DatasetOutput<V> result = new DatasetOutput();

        Long id = dataset.getId();
        String hrefBase = query.getHrefBase();
        String domainId = dataset.getIdentifier();
        String uom = dataset.getUnitI18nName(query.getLocale());
        String label = createDatasetLabel(dataset, query.getLocale());

        result.setId(id.toString());
        result.setValue(DatasetOutput.UOM, uom, parameters, result::setUom);
        result.setValue(ParameterOutput.LABEL, label, parameters, result::setLabel);
        result.setValue(ParameterOutput.DOMAIN_ID, domainId, parameters, result::setDomainId);
        result.setValue(DatasetOutput.DATASET_TYPE, dataset.getDatasetType().name(), parameters,
                result::setDatasetType);
        result.setValue(DatasetOutput.OBSERVATION_TYPE, dataset.getObservationType().name(), parameters,
                result::setObservationType);
        result.setValue(DatasetOutput.VALUE_TYPE, dataset.getValueType().name(), parameters, result::setValueType);
        result.setValue(DatasetOutput.MOBILE, dataset.isMobile(), parameters, result::setMobile);
        result.setValue(DatasetOutput.INSITU, dataset.isInsitu(), parameters, result::setInsitu);
        if (dataset.hasSamplingProfile()) {
            result.setValue(DatasetOutput.HAS_SAMPLINGS, dataset.getSamplingProfile().hasSamplings(), parameters,
                    result::setHasSamplings);
        }
        result.setValue(ParameterOutput.HREF, createHref(hrefBase, dataset), parameters, result::setHref);
        result.setValue(DatasetOutput.ORIGIN_TIMEZONE,
                dataset.isSetOriginTimezone() ? dataset.getOriginTimezone() : "UTC", parameters,
                result::setOriginTimezone);

        result.setValue(DatasetOutput.SMAPLING_TIME_START,
                createTimeOutput(dataset.getFirstValueAt(), dataset.getOriginTimezone(), parameters), parameters,
                result::setSamplingTimeStart);
        result.setValue(DatasetOutput.SMAPLING_TIME_END,
                createTimeOutput(dataset.getLastValueAt(), dataset.getOriginTimezone(), parameters), parameters,
                result::setSamplingTimeEnd);
        result.setValue(DatasetOutput.FEATURE, getCondensedFeature(dataset.getFeature(), query), parameters,
                result::setFeature);

        return result;
    }

    private String createHref(String hrefBase, DatasetEntity dataset) {
        return HrefHelper.constructHref(hrefBase, getCollectionName(dataset)) + "/" + dataset.getId();
    }

    private String getCollectionName(DatasetEntity dataset) {
        switch (dataset.getDatasetType()) {
            case individualObservation:
                return IndividualObservationOutput.COLLECTION_PATH;
            case trajectory:
                return TrajectoryOutput.COLLECTION_PATH;
            case profile:
                return ProfileOutput.COLLECTION_PATH;
            case timeseries:
                return TimeseriesMetadataOutput.COLLECTION_PATH;
            default:
                return DatasetOutput.COLLECTION_PATH;
        }
    }

    protected DatasetOutput<V> createExpanded(DatasetEntity dataset, DbQuery query, Session session) {
        IoParameters params = query.getParameters();
        DatasetOutput<V> result = createCondensed(dataset, query);

        DatasetParameters datasetParams = createDatasetParameters(dataset, query.withoutFieldsFilter(), session);
        datasetParams.setPlatform(getCondensedPlatform(dataset.getPlatform(), query));
        if (dataset.getService() == null) {
            dataset.setService(getServiceEntity());
        }

        DataRepository<DatasetEntity, ?, V, ?> dataRepository = getDataRepositoryFactory(dataset);
        V firstValue = dataRepository.getFirstValue(dataset, session, query);
        V lastValue = dataset.getFirstValueAt().equals(dataset.getLastValueAt()) ? firstValue
                : dataRepository.getLastValue(dataset, session, query);

        List<ReferenceValueOutput<V>> refValues = dataRepository.getReferenceValues(dataset, query, session);
        lastValue = isReferenceSeries(dataset) && isCongruentValues(firstValue, lastValue)
                // first == last to have a valid interval
                ? firstValue
                : lastValue;

        result.setValue(DatasetOutput.REFERENCE_VALUES, refValues, params, result::setReferenceValues);
        result.setValue(DatasetOutput.DATASET_PARAMETERS, datasetParams, params, result::setDatasetParameters);
        result.setValue(DatasetOutput.FIRST_VALUE, firstValue, params, result::setFirstValue);
        result.setValue(DatasetOutput.LAST_VALUE, lastValue, params, result::setLastValue);

        if (query.getParameters().containsParameter(Parameters.AGGREGATION)
                && dataRepository instanceof AbstractDataRepository) {
            Set<String> aggParams = query.getParameters().getAggregation();
            AggregationOutput<V> aggregation = new AggregationOutput<>();
            addCount(aggregation, aggParams, (AbstractDataRepository<DatasetEntity, ?, V, ?>) dataRepository, dataset,
                    query, session);
            if (checkNumerical(dataset) && dataRepository instanceof AbstractNumericalDataRepository) {
                addAggregation(aggregation, aggParams, (AbstractNumericalDataRepository<?, V, ?>) dataRepository,
                        dataset, query, session);
            }
            if (!aggregation.isEmpty()) {
                result.setValue(DatasetOutput.AGGREGATION, aggregation, params, result::setAggregations);
            }
        }

        return result;
    }

    private void addCount(AggregationOutput<V> aggregation, Set<String> params,
            AbstractDataRepository<DatasetEntity, ?, V, ?> dataRepository, DatasetEntity dataset, DbQuery query,
            Session session) {
        if (params.isEmpty() || params.contains("count")) {
            aggregation.setCount(OptionalOutput.of(dataRepository.getCount(dataset, query, session)));
        }
    }

    private void addAggregation(AggregationOutput<V> aggregation, Set<String> params,
            AbstractNumericalDataRepository<?, V, ?> dataRepository, DatasetEntity dataset, DbQuery query,
            Session session) {
        if (params.isEmpty() || params.contains("max")) {
            aggregation.setMax(OptionalOutput.of(dataRepository.getMax(dataset, query, session)));
        }
        if (params.isEmpty() || params.contains("min")) {
            aggregation.setMin(OptionalOutput.of(dataRepository.getMin(dataset, query, session)));
        }
        if (params.isEmpty() || params.contains("avg")) {
            aggregation.setAvg(OptionalOutput.of(dataRepository.getAverage(dataset, query, session)));
        }
    }

    private boolean checkNumerical(DatasetEntity dataset) {
        return ValueType.quantity.equals(dataset.getValueType()) || ValueType.count.equals(dataset.getValueType());
    }

    private DataRepository<DatasetEntity, ?, V, ?> getDataRepositoryFactory(DatasetEntity dataset) {
        return dataRepositoryFactory.create(dataset.getObservationType().name(), dataset.getValueType().name(),
                DatasetEntity.class);
    }

    private boolean isCongruentValues(AbstractValue<?> firstValue, AbstractValue<?> lastValue) {
        return firstValue.getTimestamp().equals(lastValue.getTimestamp());
    }

    private boolean isReferenceSeries(DatasetEntity series) {
        return series.getProcedure().isReference();
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
        return sb.append(phenomenonLabel).append(", ").append(procedureLabel).append(", ").append(stationLabel)
                .append(", ").append(offeringLabel).toString();
    }

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
