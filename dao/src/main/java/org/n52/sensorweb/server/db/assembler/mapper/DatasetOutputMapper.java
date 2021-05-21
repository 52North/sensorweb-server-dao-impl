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
package org.n52.sensorweb.server.db.assembler.mapper;

import java.util.List;

import org.hibernate.Session;
import org.n52.io.handler.DatasetFactoryException;
import org.n52.io.request.IoParameters;
import org.n52.io.response.ParameterOutput;
import org.n52.io.response.dataset.AbstractValue;
import org.n52.io.response.dataset.DatasetOutput;
import org.n52.io.response.dataset.DatasetParameters;
import org.n52.io.response.dataset.IndividualObservationOutput;
import org.n52.io.response.dataset.ProfileOutput;
import org.n52.io.response.dataset.ReferenceValueOutput;
import org.n52.io.response.dataset.TimeseriesMetadataOutput;
import org.n52.io.response.dataset.TrajectoryOutput;
import org.n52.sensorweb.server.db.ValueAssembler;
import org.n52.sensorweb.server.db.old.dao.DbQuery;
import org.n52.series.db.beans.AbstractFeatureEntity;
import org.n52.series.db.beans.DatasetEntity;
import org.n52.series.db.beans.OfferingEntity;
import org.n52.series.db.beans.PhenomenonEntity;
import org.n52.series.db.beans.ProcedureEntity;
import org.n52.series.db.beans.ServiceEntity;

public class DatasetOutputMapper<V extends AbstractValue<?>>
        extends ParameterOutputSearchResultMapper<DatasetEntity, DatasetOutput<V>> {

    private static final DbQuery DUMMY_QUERY = new DbQuery(null);

    public DatasetOutputMapper(DbQuery query, OutputMapperFactory outputMapperFactory) {
        this(query, outputMapperFactory, false);
    }

    public DatasetOutputMapper(DbQuery query, OutputMapperFactory outputMapperFactory, boolean subMapper) {
        super(query, outputMapperFactory, subMapper);
        initSubSelect(getDbQuery(), DatasetOutput.DATASET_PARAMETERS);
    }

    @Override
    public void addAll(DatasetOutput<V> result, DatasetEntity entity, DbQuery query, IoParameters parameters) {
        super.addAll(result, entity, query, parameters);
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

    public void add(DatasetOutput<V> result, DatasetEntity dataset, DbQuery query, IoParameters params,
            Session session, ValueAssembler<?, V, ?> dataRepository) {
        V firstValue = addFirstValue(result, dataset, query, params, dataRepository);
        addLastValue(result, dataset, query, params, dataRepository, firstValue);
        addReferanceValues(result, dataset, query, params, dataRepository);
        addExtra(result, dataset, query, params);
        addParameters(result, dataset, query, params);
    }

    @Override
    public DatasetOutput<V> addExpandedValues(DatasetEntity entity, DatasetOutput<V> output) {
        super.addExpandedValues(entity, output);
        IoParameters params = getDbQuery().getParameters();
        try {
            ValueAssembler<?, V, ?> dataRepository = getDataRepositoryFactory(entity);
            if (!hasSelect()) {
                addAllExpanded(output, entity, getDbQuery(), params, dataRepository);
            } else {
                addSelectedExpanded(output, entity, getDbQuery(), params, dataRepository);
            }
        } catch (DatasetFactoryException e) {
            getLogger().debug("Error adding expanded values!", e);
        }
        return output;
    }

    private void addAllExpanded(DatasetOutput<V> result, DatasetEntity dataset, DbQuery query, IoParameters params,
            ValueAssembler<?, V, ?> dataRepository) {
        V firstValue = addFirstValue(result, dataset, query, params, dataRepository);
        addLastValue(result, dataset, query, params, dataRepository, firstValue);
        addReferanceValues(result, dataset, query, params, dataRepository);
        addExtra(result, dataset, query, params);
        addParameters(result, dataset, query, params);
    }

    private void addSelectedExpanded(DatasetOutput<V> result, DatasetEntity dataset, DbQuery query,
            IoParameters params, ValueAssembler<?, V, ?> dataRepository) {
        V firstValue = null;
        if (isSelected(DatasetOutput.FIRST_VALUE)) {
            firstValue = addFirstValue(result, dataset, query, params, dataRepository);
        }
        if (isSelected(DatasetOutput.LAST_VALUE)) {
            addLastValue(result, dataset, query, params, dataRepository, firstValue);
        }
        if (isSelected(DatasetOutput.REFERENCE_VALUES)) {
            addReferanceValues(result, dataset, query, params, dataRepository);
        }
        if (isSelected(ParameterOutput.EXTRAS)) {
            addExtra(result, dataset, query, params);
        }
        if (isSelected(DatasetOutput.DATASET_PARAMETERS)) {
            addParameters(result, dataset, query, params);
        }
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
        result.setValue(DatasetOutput.FEATURE, getFeatureOutput(dataset.getFeature()), parameters, result::setFeature);
    }

    private V addFirstValue(DatasetOutput<V> result, DatasetEntity dataset, DbQuery query, IoParameters params,
            ValueAssembler<?, V, ?> dataRepository) {
        V firstValue = dataRepository.getFirstValue(dataset, query);
        result.setValue(DatasetOutput.FIRST_VALUE, firstValue, params, result::setFirstValue);
        return firstValue;
    }

    private void addLastValue(DatasetOutput<V> result, DatasetEntity dataset, DbQuery query, IoParameters params,
            ValueAssembler<?, V, ?> dataRepository, V firstValue) {
        V lastValue = dataset.getFirstValueAt().equals(dataset.getLastValueAt()) && firstValue != null ? firstValue
                : dataRepository.getLastValue(dataset, query);
        lastValue = isReferenceSeries(dataset) && isCongruentValues(firstValue, lastValue)
                // first == last to have a valid interval
                ? firstValue
                : lastValue;
        result.setValue(DatasetOutput.LAST_VALUE, lastValue, params, result::setLastValue);
    }

    private void addReferanceValues(DatasetOutput<V> result, DatasetEntity dataset, DbQuery query, IoParameters params,
            ValueAssembler<?, V, ?> dataRepository) {
        List<ReferenceValueOutput<V>> refValues = dataRepository.getReferenceValues(dataset, query);
        result.setValue(DatasetOutput.REFERENCE_VALUES, refValues, params, result::setReferenceValues);
    }

    private void addExtra(DatasetOutput<V> result, DatasetEntity dataset, DbQuery query, IoParameters params) {
        DatasetParameters datasetParams = new DatasetParameters();
        datasetParams.setPhenomenon(getPhenomenonOutput(dataset.getPhenomenon()));
        result.setValue(DatasetOutput.DATASET_PARAMETERS, datasetParams, params, result::setDatasetParameters);

    }

    private void addParameters(DatasetOutput<V> result, DatasetEntity dataset, DbQuery query, IoParameters params) {
        DatasetParameters datasetParams = createDatasetParameters(dataset, DUMMY_QUERY);
        result.setValue(DatasetOutput.DATASET_PARAMETERS, datasetParams, params, result::setDatasetParameters);

    }

    private DatasetParameters createDatasetParameters(DatasetEntity dataset, DbQuery query) {
        DatasetParameters metadata = new DatasetParameters();
        ServiceEntity service = getOutputMapperFactory().getServiceEntity(dataset);
        if (getSubSelection(DatasetOutput.DATASET_PARAMETERS) == null
                || getSubSelection(DatasetOutput.DATASET_PARAMETERS).isEmpty()) {
            metadata.setService(getServiceOutput(service));
            metadata.setOffering(getOfferingOutput(dataset.getOffering()));
            metadata.setProcedure(getProcedureOutput(dataset.getProcedure()));
            metadata.setPhenomenon(getPhenomenonOutput(dataset.getPhenomenon()));
            metadata.setCategory(getCategoryOutput(dataset.getCategory()));
            metadata.setPlatform(getPlatformOutput(dataset.getPlatform()));
        } else {
            if (isSubSelected(DatasetOutput.DATASET_PARAMETERS, DatasetParameters.SERVICE)) {
                metadata.setService(getServiceOutput(service));
            }
            if (isSubSelected(DatasetOutput.DATASET_PARAMETERS, DatasetParameters.OFFERING)) {
                metadata.setOffering(getOfferingOutput(dataset.getOffering()));
            }
            if (isSubSelected(DatasetOutput.DATASET_PARAMETERS, DatasetParameters.PROCEDURE)) {
                metadata.setProcedure(getProcedureOutput(dataset.getProcedure()));
            }
            if (isSubSelected(DatasetOutput.DATASET_PARAMETERS, DatasetParameters.PHENOMENON)) {
                metadata.setPhenomenon(getPhenomenonOutput(dataset.getPhenomenon()));
            }
            if (isSubSelected(DatasetOutput.DATASET_PARAMETERS, DatasetParameters.CATEGORY)) {
                metadata.setCategory(getCategoryOutput(dataset.getCategory()));
            }
            if (isSubSelected(DatasetOutput.DATASET_PARAMETERS, DatasetParameters.PLATFORM)) {
                metadata.setPlatform(getPlatformOutput(dataset.getPlatform()));
            }
        }
        return metadata;
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

    private ValueAssembler<?, V, ?> getDataRepositoryFactory(DatasetEntity dataset) throws DatasetFactoryException {
        return getOutputMapperFactory().getDataRepositoryFactory().create(dataset.getDatasetType().name(),
                dataset.getObservationType().name(), dataset.getValueType().name(), DatasetEntity.class);
    }

    private boolean isCongruentValues(AbstractValue<?> firstValue, AbstractValue<?> lastValue) {
        return firstValue.getTimestamp().equals(lastValue.getTimestamp());
    }

    private boolean isReferenceSeries(DatasetEntity series) {
        return series.getProcedure().isReference();
    }

    @Override
    public String createLabel(DatasetEntity entity, DbQuery query) {
        return createDatasetLabel(entity, query.getLocaleForLabel());
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

    @Override
    public DatasetOutput<V> getParameterOuput() {
        return new DatasetOutput<>();
    }

}
