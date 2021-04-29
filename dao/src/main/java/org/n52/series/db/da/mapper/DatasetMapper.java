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
package org.n52.series.db.da.mapper;

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

    public DatasetMapper(MapperFactory mapperFactory) {
        super(mapperFactory);
    }

    @Override
    public DatasetOutput<V> createCondensed(DatasetEntity dataset, DbQuery query) {
        return createCondensed(new DatasetOutput<>(), dataset, query);
    }

    @Override
    public DatasetOutput<V> createCondensed(DatasetOutput<V> result, DatasetEntity dataset, DbQuery query) {
        try {
            IoParameters parameters = query.getParameters();
            addService(result, dataset, query);
            result.setId(dataset.getId().toString());
            if (parameters.isSelected(ParameterOutput.LABEL)) {
                result.setValue(ParameterOutput.LABEL, createDatasetLabel(dataset, query.getLocaleForLabel()),
                        parameters, result::setLabel);
            }
            if (parameters.isSelected(ParameterOutput.DOMAIN_ID)) {
                result.setValue(ParameterOutput.DOMAIN_ID, dataset.getIdentifier(), parameters, result::setDomainId);
            }
            if (parameters.isSelected(ParameterOutput.HREF)) {
                result.setValue(ParameterOutput.HREF, createHref(query.getHrefBase(), dataset), parameters,
                        result::setHref);
            }
            if (parameters.isSelected(DatasetOutput.UOM)) {
                result.setValue(DatasetOutput.UOM, dataset.getUnitI18nName(query.getLocaleForLabel()), parameters,
                        result::setUom);
            }
            if (parameters.isSelected(DatasetOutput.DATASET_TYPE)) {
                result.setValue(DatasetOutput.DATASET_TYPE, dataset.getDatasetType().name(), parameters,
                        result::setDatasetType);
            }
            if (parameters.isSelected(DatasetOutput.OBSERVATION_TYPE)) {
                result.setValue(DatasetOutput.OBSERVATION_TYPE, dataset.getObservationType().name(), parameters,
                        result::setObservationType);
            }
            if (parameters.isSelected(DatasetOutput.VALUE_TYPE)) {
                result.setValue(DatasetOutput.VALUE_TYPE, dataset.getValueType().name(), parameters,
                        result::setValueType);
            }
            if (parameters.isSelected(DatasetOutput.MOBILE)) {
                result.setValue(DatasetOutput.MOBILE, dataset.isMobile(), parameters, result::setMobile);
            }
            if (parameters.isSelected(DatasetOutput.INSITU)) {
                result.setValue(DatasetOutput.INSITU, dataset.isInsitu(), parameters, result::setInsitu);
            }
            if (parameters.isSelected(DatasetOutput.HAS_SAMPLINGS) && dataset.hasSamplingProfile()) {
                result.setValue(DatasetOutput.HAS_SAMPLINGS, dataset.getSamplingProfile().hasSamplings(), parameters,
                        result::setHasSamplings);
            }
            if (parameters.isSelected(DatasetOutput.ORIGIN_TIMEZONE)) {
                result.setValue(DatasetOutput.ORIGIN_TIMEZONE,
                        dataset.isSetOriginTimezone() ? dataset.getOriginTimezone() : "UTC", parameters,
                        result::setOriginTimezone);
            }
            if (parameters.isSelected(DatasetOutput.SMAPLING_TIME_START)) {
                result.setValue(DatasetOutput.SMAPLING_TIME_START,
                        createTimeOutput(dataset.getFirstValueAt(), dataset.getOriginTimezone(), parameters),
                        parameters, result::setSamplingTimeStart);
            }
            if (parameters.isSelected(DatasetOutput.SMAPLING_TIME_END)) {
                result.setValue(DatasetOutput.SMAPLING_TIME_END,
                        createTimeOutput(dataset.getLastValueAt(), dataset.getOriginTimezone(), parameters),
                        parameters, result::setSamplingTimeEnd);
            }
            if (parameters.isSelected(DatasetOutput.FEATURE)) {
                result.setValue(DatasetOutput.FEATURE,
                        getCondensedFeature(dataset.getFeature(), query.withoutSelectFilter()), parameters,
                        result::setFeature);
            }

            return result;
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
        V firstValue = null;
        if (params.isSelected(DatasetOutput.FIRST_VALUE)) {
            firstValue = dataRepository.getFirstValue(dataset, session, query);
            result.setValue(DatasetOutput.FIRST_VALUE, firstValue, params, result::setFirstValue);
        }
        if (params.isSelected(DatasetOutput.LAST_VALUE)) {
            V lastValue = dataset.getFirstValueAt().equals(dataset.getLastValueAt()) && firstValue != null ? firstValue
                    : dataRepository.getLastValue(dataset, session, query);
            lastValue = isReferenceSeries(dataset) && isCongruentValues(firstValue, lastValue)
                    // first == last to have a valid interval
                    ? firstValue
                    : lastValue;
            result.setValue(DatasetOutput.LAST_VALUE, lastValue, params, result::setLastValue);
        }
        if (params.isSelected(DatasetOutput.REFERENCE_VALUES)) {
            List<ReferenceValueOutput<V>> refValues = dataRepository.getReferenceValues(dataset, query, session);
            result.setValue(DatasetOutput.REFERENCE_VALUES, refValues, params, result::setReferenceValues);
        }
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

        if (params.isSelected(ParameterOutput.EXTRAS)) {
            DatasetParameters datasetParams = new DatasetParameters();
            datasetParams.setPhenomenon(getCondensedPhenomenon(dataset.getPhenomenon(), query));
            result.setValue(DatasetOutput.DATASET_PARAMETERS, datasetParams, params, result::setDatasetParameters);
        }
        if (params.isSelected(DatasetOutput.DATASET_PARAMETERS)) {
            DatasetParameters datasetParams = createDatasetParameters(dataset,
                    query.withoutFieldsFilter().withSubSelectFilter(DatasetOutput.DATASET_PARAMETERS), session);
            result.setValue(DatasetOutput.DATASET_PARAMETERS, datasetParams, params, result::setDatasetParameters);
        }
        return result;
    }

    private DatasetParameters createDatasetParameters(DatasetEntity dataset, DbQuery query, Session session)
            throws DataAccessException {
        DatasetParameters metadata = new DatasetParameters();
        ServiceEntity service = getServiceEntity(dataset);
        if (query.getParameters().isSelected(DatasetParameters.SERVICE)) {
            metadata.setService(getCondensedService(service, query.withSubSelectFilter(DatasetParameters.SERVICE)));
        }
        if (query.getParameters().isSelected(DatasetParameters.OFFERING)) {
            metadata.setOffering(getCondensedOffering(dataset.getOffering(),
                    query.withSubSelectFilter(DatasetParameters.OFFERING)));
        }
        if (query.getParameters().isSelected(DatasetParameters.PROCEDURE)) {
            metadata.setProcedure(getCondensedProcedure(dataset.getProcedure(),
                    query.withSubSelectFilter(DatasetParameters.PROCEDURE)));
        }
        if (query.getParameters().isSelected(DatasetParameters.PHENOMENON)) {
            metadata.setPhenomenon(getCondensedPhenomenon(dataset.getPhenomenon(),
                    query.withSubSelectFilter(DatasetParameters.PHENOMENON)));
        }
        if (query.getParameters().isSelected(DatasetParameters.CATEGORY)) {
            metadata.setCategory(getCondensedCategory(dataset.getCategory(),
                    query.withSubSelectFilter(DatasetParameters.CATEGORY)));
        }
        if (query.getParameters().isSelected(DatasetParameters.PLATFORM)) {
            metadata.setPlatform(getCondensedPlatform(dataset.getPlatform(),
                    query.withSubSelectFilter(DatasetParameters.PLATFORM)));
        }
        return metadata;
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
