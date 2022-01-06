/*
 * Copyright (C) 2015-2022 52°North Spatial Information Research GmbH
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
package org.n52.series.db.da.mapper;

import java.util.List;
import java.util.Set;

import org.hibernate.Session;
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
import org.n52.series.db.beans.AbstractFeatureEntity;
import org.n52.series.db.beans.DatasetEntity;
import org.n52.series.db.beans.OfferingEntity;
import org.n52.series.db.beans.PhenomenonEntity;
import org.n52.series.db.beans.ProcedureEntity;
import org.n52.series.db.beans.ServiceEntity;
import org.n52.series.db.beans.dataset.ValueType;
import org.n52.series.db.da.AbstractDataRepository;
import org.n52.series.db.da.AbstractNumericalDataRepository;
import org.n52.series.db.da.DataRepository;
import org.n52.series.db.dao.DbQuery;

public class DatasetMapper<V extends AbstractValue<?>> extends AbstractOuputMapper<DatasetOutput<V>, DatasetEntity> {

    private static final DbQuery DUMMY_QUERY = new DbQuery(null);

    public DatasetMapper(MapperFactory mapperFactory, IoParameters params) {
        super(mapperFactory, params, false);
    }

    public DatasetMapper(MapperFactory mapperFactory, IoParameters params, boolean subMapper) {
        super(mapperFactory, params, subMapper);
        initSubSelect(params, DatasetOutput.DATASET_PARAMETERS);
    }

    @Override
    public DatasetOutput<V> createCondensed(DatasetEntity dataset, DbQuery query) {
        return createCondensed(new DatasetOutput<>(), dataset, query);
    }

    @Override
    public DatasetOutput<V> createCondensed(DatasetOutput<V> result, DatasetEntity dataset, DbQuery query) {
        try {
            return super.createCondensed(result, dataset, query);
        } catch (Exception e) {
            getLogger().error("Error while processing {} with id {}! Exception: {}",
                    dataset.getClass().getSimpleName(), dataset.getId(), e);
        }
        return null;
    }

    @Override
    public DatasetOutput<V> createExpanded(DatasetEntity dataset, DbQuery query, Session session) {
        IoParameters params = query.getParameters();
        DatasetOutput<V> result = createCondensed(dataset, query);
        DataRepository<DatasetEntity, ?, V, ?> dataRepository = getDataRepositoryFactory(dataset);
        if (!hasSelect()) {
            addAllExpanded(result, dataset, query, params, session, dataRepository);
        } else {
            addSelectedExpanded(result, dataset, query, params, session, dataRepository);
        }
        if (query.getParameters().containsParameter(Parameters.AGGREGATION)
                && dataRepository instanceof AbstractDataRepository) {
            addAggregationOutput(result, dataset, query, params, session, dataRepository);
        }
        return result;
    }

    @Override
    public void addAll(DatasetOutput<V> result, DatasetEntity entity, DbQuery query, IoParameters parameters) {
        addLabel(result, entity, query, parameters);
        addDomainId(result, entity, query, parameters);
        addHref(result, entity, query, parameters);
        addUom(result, entity, query, parameters);
        addDatasetType(result, entity, query, parameters);
        addObservationType(result, entity, query, parameters);
        addValueType(result, entity, query, parameters);
        addMobile(result, entity, query, parameters);
        addInsitu(result, entity, query, parameters);
        addHasSamplings(result, entity, query, parameters);
        addOriginTimeZone(result, entity, query, parameters);
        addSamplingTimeStart(result, entity, query, parameters);
        addSamplingTimeEnd(result, entity, query, parameters);
        addFeature(result, entity, query, parameters);
    }

    @Override
    public void addSelected(DatasetOutput<V> result, DatasetEntity entity, DbQuery query, IoParameters parameters) {
        super.addSelected(result, entity, query, parameters);
        for (String selected : parameters.getSelectOriginal()) {
            switch (selected) {
                case DatasetOutput.UOM:
                    addUom(result, entity, query, parameters);
                    break;
                case DatasetOutput.DATASET_TYPE:
                    addDatasetType(result, entity, query, parameters);
                    break;
                case DatasetOutput.OBSERVATION_TYPE:
                    addObservationType(result, entity, query, parameters);
                    break;
                case DatasetOutput.VALUE_TYPE:
                    addValueType(result, entity, query, parameters);
                    break;
                case DatasetOutput.MOBILE:
                    addMobile(result, entity, query, parameters);
                    break;
                case DatasetOutput.INSITU:
                    addInsitu(result, entity, query, parameters);
                    break;
                case DatasetOutput.HAS_SAMPLINGS:
                    addHasSamplings(result, entity, query, parameters);
                    break;
                case DatasetOutput.ORIGIN_TIMEZONE:
                    addOriginTimeZone(result, entity, query, parameters);
                    break;
                case DatasetOutput.SMAPLING_TIME_START:
                    addSamplingTimeStart(result, entity, query, parameters);
                    break;
                case DatasetOutput.SMAPLING_TIME_END:
                    addSamplingTimeEnd(result, entity, query, parameters);
                    break;
                case DatasetOutput.FEATURE:
                    addFeature(result, entity, query, parameters);
                    break;
                default:
                    break;
            }
        }
    }

    private void addAllExpanded(DatasetOutput<V> result, DatasetEntity dataset, DbQuery query, IoParameters params,
            Session session, DataRepository<DatasetEntity, ?, V, ?> dataRepository) {
        V firstValue = addFirstValue(result, dataset, query, params, session, dataRepository);
        addLastValue(result, dataset, query, params, session, dataRepository, firstValue);
        addReferanceValues(result, dataset, query, params, session, dataRepository);
        addExtra(result, dataset, query, params);
        addParameters(result, dataset, query, params, session);
    }

    private void addSelectedExpanded(DatasetOutput<V> result, DatasetEntity dataset, DbQuery query,
            IoParameters params, Session session, DataRepository<DatasetEntity, ?, V, ?> dataRepository) {
        V firstValue = null;
        if (isSelected(DatasetOutput.FIRST_VALUE)) {
            firstValue = addFirstValue(result, dataset, query, params, session, dataRepository);
        }
        if (isSelected(DatasetOutput.LAST_VALUE)) {
            addLastValue(result, dataset, query, params, session, dataRepository, firstValue);
        }
        if (isSelected(DatasetOutput.REFERENCE_VALUES)) {
            addReferanceValues(result, dataset, query, params, session, dataRepository);
        }
        if (isSelected(ParameterOutput.EXTRAS)) {
            addExtra(result, dataset, query, params);
        }
        if (isSelected(DatasetOutput.DATASET_PARAMETERS)) {
            addParameters(result, dataset, query, params, session);
        }
    }

    @Override
    public void addLabel(DatasetOutput<V> result, DatasetEntity dataset, DbQuery query, IoParameters parameters) {
        result.setValue(ParameterOutput.LABEL, createDatasetLabel(dataset, query.getLocaleForLabel()), parameters,
                result::setLabel);
    }

    private void addUom(DatasetOutput<V> result, DatasetEntity dataset, DbQuery query, IoParameters parameters) {
        result.setValue(DatasetOutput.UOM, dataset.getUnitI18nName(query.getLocaleForLabel()), parameters,
                result::setUom);
    }

    private void addDatasetType(DatasetOutput<V> result, DatasetEntity dataset, DbQuery query,
            IoParameters parameters) {
        result.setValue(DatasetOutput.DATASET_TYPE, dataset.getDatasetType().name(), parameters,
                result::setDatasetType);
    }

    private void addObservationType(DatasetOutput<V> result, DatasetEntity dataset, DbQuery query,
            IoParameters parameters) {
        result.setValue(DatasetOutput.OBSERVATION_TYPE, dataset.getObservationType().name(), parameters,
                result::setObservationType);
    }

    private void addValueType(DatasetOutput<V> result, DatasetEntity dataset, DbQuery query, IoParameters parameters) {
        result.setValue(DatasetOutput.VALUE_TYPE, dataset.getValueType().name(), parameters, result::setValueType);
    }

    private void addMobile(DatasetOutput<V> result, DatasetEntity dataset, DbQuery query, IoParameters parameters) {
        result.setValue(DatasetOutput.MOBILE, dataset.isMobile(), parameters, result::setMobile);
    }

    private void addInsitu(DatasetOutput<V> result, DatasetEntity dataset, DbQuery query, IoParameters parameters) {
        result.setValue(DatasetOutput.INSITU, dataset.isInsitu(), parameters, result::setInsitu);
    }

    private void addHasSamplings(DatasetOutput<V> result, DatasetEntity dataset, DbQuery query,
            IoParameters parameters) {
        if (dataset.hasSamplingProfile()) {
            result.setValue(DatasetOutput.HAS_SAMPLINGS, dataset.getSamplingProfile().hasSamplings(), parameters,
                    result::setHasSamplings);
        }
    }

    private void addOriginTimeZone(DatasetOutput<V> result, DatasetEntity dataset, DbQuery query,
            IoParameters parameters) {
        result.setValue(DatasetOutput.ORIGIN_TIMEZONE,
                dataset.isSetOriginTimezone() ? dataset.getOriginTimezone() : "UTC", parameters,
                result::setOriginTimezone);
    }

    private void addSamplingTimeStart(DatasetOutput<V> result, DatasetEntity dataset, DbQuery query,
            IoParameters parameters) {
        result.setValue(DatasetOutput.SMAPLING_TIME_START,
                createTimeOutput(dataset.getFirstValueAt(), dataset.getOriginTimezone(), parameters), parameters,
                result::setSamplingTimeStart);

    }

    private void addSamplingTimeEnd(DatasetOutput<V> result, DatasetEntity dataset, DbQuery query,
            IoParameters parameters) {
        result.setValue(DatasetOutput.SMAPLING_TIME_END,
                createTimeOutput(dataset.getLastValueAt(), dataset.getOriginTimezone(), parameters), parameters,
                result::setSamplingTimeEnd);
    }

    private void addFeature(DatasetOutput<V> result, DatasetEntity dataset, DbQuery query, IoParameters parameters) {
        result.setValue(DatasetOutput.FEATURE, getCondensedFeature(dataset.getFeature(), DUMMY_QUERY),
                parameters, result::setFeature);
    }

    private V addFirstValue(DatasetOutput<V> result, DatasetEntity dataset, DbQuery query, IoParameters params,
            Session session, DataRepository<DatasetEntity, ?, V, ?> dataRepository) {
        V firstValue = dataRepository.getFirstValue(dataset, session, query);
        result.setValue(DatasetOutput.FIRST_VALUE, firstValue, params, result::setFirstValue);
        return firstValue;
    }

    private void addLastValue(DatasetOutput<V> result, DatasetEntity dataset, DbQuery query, IoParameters params,
            Session session, DataRepository<DatasetEntity, ?, V, ?> dataRepository, V firstValue) {
        V lastValue = dataset.getFirstValueAt().equals(dataset.getLastValueAt()) && firstValue != null ? firstValue
                : dataRepository.getLastValue(dataset, session, query);
        lastValue = isReferenceSeries(dataset) && isCongruentValues(firstValue, lastValue)
                // first == last to have a valid interval
                ? firstValue
                : lastValue;
        result.setValue(DatasetOutput.LAST_VALUE, lastValue, params, result::setLastValue);
    }

    private void addAggregationOutput(DatasetOutput<V> result, DatasetEntity dataset, DbQuery query,
            IoParameters params, Session session, DataRepository<DatasetEntity, ?, V, ?> dataRepository) {
        Set<String> aggParams = query.getParameters().getAggregation();
        AggregationOutput<V> aggregation = new AggregationOutput<>();
        addCount(aggregation, aggParams, (AbstractDataRepository<DatasetEntity, ?, V, ?>) dataRepository, dataset,
                query, session);
        if (checkNumerical(dataset) && dataRepository instanceof AbstractNumericalDataRepository) {
            addAggregation(aggregation, aggParams, (AbstractNumericalDataRepository<?, V, ?>) dataRepository, dataset,
                    query, session);
        }
        if (!aggregation.isEmpty()) {
            result.setValue(DatasetOutput.AGGREGATION, aggregation, params, result::setAggregations);
        }
    }

    private void addReferanceValues(DatasetOutput<V> result, DatasetEntity dataset, DbQuery query, IoParameters params,
            Session session, DataRepository<DatasetEntity, ?, V, ?> dataRepository) {
        List<ReferenceValueOutput<V>> refValues = dataRepository.getReferenceValues(dataset, query, session);
        result.setValue(DatasetOutput.REFERENCE_VALUES, refValues, params, result::setReferenceValues);
    }

    private void addExtra(DatasetOutput<V> result, DatasetEntity dataset, DbQuery query, IoParameters params) {
        DatasetParameters datasetParams = new DatasetParameters();
        datasetParams.setPhenomenon(getCondensedPhenomenon(dataset.getPhenomenon(), query));
        result.setValue(DatasetOutput.DATASET_PARAMETERS, datasetParams, params, result::setDatasetParameters);

    }

    private void addParameters(DatasetOutput<V> result, DatasetEntity dataset, DbQuery query, IoParameters params,
            Session session) {
        DatasetParameters datasetParams = createDatasetParameters(dataset,
                DUMMY_QUERY, session);
        result.setValue(DatasetOutput.DATASET_PARAMETERS, datasetParams, params, result::setDatasetParameters);

    }

    private DatasetParameters createDatasetParameters(DatasetEntity dataset, DbQuery query, Session session)
            throws DataAccessException {
        DatasetParameters metadata = new DatasetParameters();
        ServiceEntity service = getServiceEntity(dataset);
        if (getSubSelection(DatasetOutput.DATASET_PARAMETERS) == null
                || getSubSelection(DatasetOutput.DATASET_PARAMETERS).isEmpty()) {
            metadata.setService(getCondensedService(service, query));
            metadata.setOffering(getCondensedOffering(dataset.getOffering(), query));
            metadata.setProcedure(getCondensedProcedure(dataset.getProcedure(), query));
            metadata.setPhenomenon(getCondensedPhenomenon(dataset.getPhenomenon(), query));
            metadata.setCategory(getCondensedCategory(dataset.getCategory(), query));
            metadata.setPlatform(getCondensedPlatform(dataset.getPlatform(), query));
        } else {
            if (isSubSelected(DatasetOutput.DATASET_PARAMETERS, DatasetParameters.SERVICE)) {
                metadata.setService(getCondensedService(service, query));
            }
            if (isSubSelected(DatasetOutput.DATASET_PARAMETERS, DatasetParameters.OFFERING)) {
                metadata.setOffering(getCondensedOffering(dataset.getOffering(), query));
            }
            if (isSubSelected(DatasetOutput.DATASET_PARAMETERS, DatasetParameters.PROCEDURE)) {
                metadata.setProcedure(getCondensedProcedure(dataset.getProcedure(), query));
            }
            if (isSubSelected(DatasetOutput.DATASET_PARAMETERS, DatasetParameters.PHENOMENON)) {
                metadata.setPhenomenon(getCondensedPhenomenon(dataset.getPhenomenon(), query));
            }
            if (isSubSelected(DatasetOutput.DATASET_PARAMETERS, DatasetParameters.CATEGORY)) {
                metadata.setCategory(getCondensedCategory(dataset.getCategory(), query));
            }
            if (isSubSelected(DatasetOutput.DATASET_PARAMETERS, DatasetParameters.PLATFORM)) {
                metadata.setPlatform(getCondensedPlatform(dataset.getPlatform(), query));
            }
        }
        return metadata;
    }

    @Override
    public String getCollectionName(DatasetOutput<V> result, DatasetEntity dataset) {
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

    private String createDatasetLabel(DatasetEntity dataset, String locale) {
        if (dataset.isSetName()) {
            return dataset.getLabelFrom(locale);
        }
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

    private DataRepository<DatasetEntity, ?, V, ?> getDataRepositoryFactory(DatasetEntity dataset) {
        return getMapperFactory().getDataRepositoryFactory().create(dataset.getObservationType().name(),
                dataset.getValueType().name(), DatasetEntity.class);
    }

    private boolean isCongruentValues(AbstractValue<?> firstValue, AbstractValue<?> lastValue) {
        return firstValue.getTimestamp().equals(lastValue.getTimestamp());
    }

    private boolean isReferenceSeries(DatasetEntity series) {
        return series.getProcedure().isReference();
    }

    private boolean checkNumerical(DatasetEntity dataset) {
        return ValueType.quantity.equals(dataset.getValueType()) || ValueType.count.equals(dataset.getValueType());
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

}
