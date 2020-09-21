/*
 * Copyright (C) 2015-2020 52°North Initiative for Geospatial Open Source
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
package org.n52.series.db.assembler.core;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import org.springframework.transaction.annotation.Transactional;

import org.n52.io.handler.DatasetFactoryException;
import org.n52.io.request.IoParameters;
import org.n52.io.request.Parameters;
import org.n52.io.response.OptionalOutput;
import org.n52.io.response.dataset.AbstractValue;
import org.n52.io.response.dataset.AggregationOutput;
import org.n52.io.response.dataset.DatasetOutput;
import org.n52.io.response.dataset.DatasetParameters;
import org.n52.io.response.dataset.ReferenceValueOutput;
import org.n52.series.db.DataRepositoryTypeFactory;
import org.n52.series.db.DatasetTypesMetadata;
import org.n52.series.db.ValueAssembler;
import org.n52.series.db.assembler.ParameterDatasetOutputAssembler;
import org.n52.series.db.assembler.ParameterOutputAssembler;
import org.n52.series.db.assembler.value.AbstractNumericalValueAssembler;
import org.n52.series.db.assembler.value.AbstractValueAssembler;
import org.n52.series.db.beans.DataEntity;
import org.n52.series.db.beans.DatasetEntity;
import org.n52.series.db.beans.dataset.ValueType;
import org.n52.series.db.old.dao.DbQuery;
import org.n52.series.db.old.dao.DbQueryFactory;
import org.n52.series.db.query.DatasetQuerySpecifications;
import org.n52.series.db.repositories.core.DatasetRepository;
import org.n52.series.spi.search.DatasetSearchResult;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

@Component
@Transactional
public class DatasetAssembler<V extends AbstractValue<?>>
        extends ParameterOutputAssembler<DatasetEntity, DatasetOutput<V>, DatasetSearchResult>
        implements ParameterDatasetOutputAssembler {

    @PersistenceContext
    private EntityManager entityManager;

    private final DataRepositoryTypeFactory dataRepositoryFactory;

    private DbQueryFactory dbQueryFactory;

    public DatasetAssembler(DatasetRepository parameterRepository, DatasetRepository datasetRepository,
            DataRepositoryTypeFactory dataRepositoryFactory, DbQueryFactory dbQueryFactory) {
        super(parameterRepository, datasetRepository);
        this.dataRepositoryFactory = dataRepositoryFactory;
        this.dbQueryFactory = dbQueryFactory;
    }

    @Override
    protected DatasetOutput<V> prepareEmptyOutput() {
        return new DatasetOutput<>();
    }

    @Override
    protected DatasetSearchResult prepareEmptySearchResult() {
        return new DatasetSearchResult();
    }

    @Override
    protected Specification<DatasetEntity> createPublicPredicate(String id, DbQuery query) {
        DatasetQuerySpecifications dsFilterSpec = DatasetQuerySpecifications.of(query, entityManager);
        return dsFilterSpec.matchFilters().and(dsFilterSpec.matchId(id));
    }

    @Override
    protected Specification<DatasetEntity> createFilterPredicate(DbQuery query) {
        DatasetQuerySpecifications dsFilterSpec = DatasetQuerySpecifications.of(query, entityManager);
        return dsFilterSpec.matchFilters();
    }

    @Override
    protected DatasetOutput<V> createExpanded(DatasetEntity entity, DbQuery query) {
        IoParameters params = query.getParameters();
        DatasetOutput<V> result = (DatasetOutput<V>) getDataset(entity, query);

        entity.setService(getServiceEntity(entity));
        DatasetParameters datasetParams = createDatasetParameters(entity, query.withoutFieldsFilter());

        ValueAssembler<?, V, ?> assembler;
        try {
            assembler = (ValueAssembler<?, V, ?>) dataRepositoryFactory.create(entity.getObservationType().name(),
                    entity.getValueType().name(), DatasetEntity.class);
            V firstValue = assembler.getFirstValue(entity, query);
            V lastValue = assembler.getLastValue(entity, query);

            List<ReferenceValueOutput<V>> refValues = assembler.getReferenceValues(entity, query);
            lastValue = isReferenceSeries(entity) && isCongruentValues(firstValue, lastValue)
                    // ensure we have a valid interval
                    ? firstValue
                    : lastValue;

            result.setValue(DatasetOutput.REFERENCE_VALUES, refValues, params, result::setReferenceValues);
            result.setValue(DatasetOutput.DATASET_PARAMETERS, datasetParams, params, result::setDatasetParameters);
            result.setValue(DatasetOutput.FIRST_VALUE, firstValue, params, result::setFirstValue);
            result.setValue(DatasetOutput.LAST_VALUE, lastValue, params, result::setLastValue);
            if (query.getParameters().containsParameter(Parameters.AGGREGATION)
                    && assembler instanceof AbstractValueAssembler) {
                Set<String> aggParams = query.getParameters().getAggregation();
                AggregationOutput<V> aggregation = new AggregationOutput<>();
                addCount(aggregation, aggParams, (AbstractValueAssembler<DataEntity<?>, V, ?>) assembler, entity,
                        query, entityManager);
                if (checkNumerical(entity) && assembler instanceof AbstractNumericalValueAssembler) {
                    addAggregation(aggregation, aggParams,
                            (AbstractNumericalValueAssembler<DataEntity<?>, V, ?>) assembler, entity, query,
                            entityManager);
                }
                if (!aggregation.isEmpty()) {
                    result.setValue(DatasetOutput.AGGREGATION, aggregation, params, result::setAggregations);
                }
            }
        } catch (DatasetFactoryException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return result;
    }

    private void addCount(AggregationOutput<V> aggregation, Set<String> params,
            AbstractValueAssembler<DataEntity<?>, V, ?> dataRepository, DatasetEntity dataset, DbQuery query,
            EntityManager entityManager) {
        if (params.isEmpty() || params.contains("count")) {
            aggregation.setCount(OptionalOutput.of(dataRepository.getCount(dataset, query)));
        }
    }

    private void addAggregation(AggregationOutput<V> aggregation, Set<String> params,
            AbstractNumericalValueAssembler<DataEntity<?>, V, ?> dataRepository, DatasetEntity dataset, DbQuery query,
            EntityManager entityManager) {
        if (params.isEmpty() || params.contains("max")) {
            aggregation.setMax(OptionalOutput.of(dataRepository.getMax(dataset, query)));
        }
        if (params.isEmpty() || params.contains("min")) {
            aggregation.setMin(OptionalOutput.of(dataRepository.getMin(dataset, query)));
        }
        if (params.isEmpty() || params.contains("avg")) {
            aggregation.setAvg(OptionalOutput.of(dataRepository.getAverage(dataset, query)));
        }
    }

    private boolean checkNumerical(DatasetEntity dataset) {
        return ValueType.quantity.equals(dataset.getValueType()) || ValueType.count.equals(dataset.getValueType());
    }

    @Override
    public DatasetEntity getOrInsertInstance(DatasetEntity dataset) {
        IoParameters parameters = IoParameters.createDefaults();
        DatasetQuerySpecifications dsQS = DatasetQuerySpecifications.of(dbQueryFactory.createFrom(parameters), null);
        Specification<DatasetEntity> specification = dsQS.matchCategory(dataset.getCategory().getId().toString());
        if (dataset.getFeature() != null && dataset.getFeature().getId() != null) {
            specification.and(dsQS.matchFeatures(dataset.getFeature().getId().toString()));
        }
        if (dataset.getProcedure() != null && dataset.getProcedure().getId() != null) {
            specification.and(dsQS.matchProcedures(dataset.getProcedure().getId().toString()));
        }
        if (dataset.getOffering() != null && dataset.getOffering().getId() != null) {
            specification.and(dsQS.matchOfferings(dataset.getOffering().getId().toString()));
        }
        if (dataset.getPhenomenon() != null && dataset.getPhenomenon().getId() != null) {
            specification.and(dsQS.matchPhenomena(dataset.getPhenomenon().getId().toString()));
        }
        if (dataset.getPlatform() != null && dataset.getPlatform().getId() != null) {
            specification.and(dsQS.matchPlatforms(dataset.getPlatform().getId().toString()));
        }
        if (dataset.getService() != null && dataset.getService().getId() != null) {
            specification.and(dsQS.matchServices(dataset.getService().getId().toString()));
        }
        Optional<DatasetEntity> instance = getParameterRepository().findOne(specification);
        return !instance.isPresent() ? getParameterRepository().saveAndFlush(dataset)
                : update(instance.get(), dataset);
    }

    private DatasetEntity update(DatasetEntity instance, DatasetEntity dataset) {
        boolean minChanged = false;
        boolean maxChanged = false;
        if (!instance.isSetFirstValueAt() || (instance.isSetFirstValueAt() && dataset.isSetFirstValueAt()
                && instance.getFirstValueAt().after(dataset.getFirstValueAt()))) {
            minChanged = true;
            instance.setFirstValueAt(dataset.getFirstValueAt());
            instance.setFirstObservation(dataset.getFirstObservation());
            instance.setFirstQuantityValue(dataset.getFirstQuantityValue());
        }
        if (!instance.isSetLastValueAt() || (instance.isSetLastValueAt() && dataset.isSetLastValueAt()
                && instance.getLastValueAt().before(dataset.getLastValueAt()))) {
            maxChanged = true;
            instance.setLastValueAt(dataset.getLastValueAt());
            instance.setLastObservation(dataset.getLastObservation());
            instance.setLastQuantityValue(dataset.getLastQuantityValue());
        }
        if (minChanged || maxChanged) {
            return getParameterRepository().saveAndFlush(instance);
        }
        return instance;
    }

    private DatasetParameters createDatasetParameters(DatasetEntity dataset, DbQuery query) {
        DatasetParameters metadata = new DatasetParameters();
        metadata.setService(getService(dataset.getService(), query));
        metadata.setOffering(getOffering(dataset, query));
        metadata.setProcedure(getProcedure(dataset, query));
        metadata.setPhenomenon(getPhenomenon(dataset, query));
        metadata.setCategory(getCategory(dataset, query));
        metadata.setPlatform(getPlatform(dataset, query));
        return metadata;
    }

    private boolean isReferenceSeries(DatasetEntity series) {
        return series.getProcedure().isReference();
    }

    private boolean isCongruentValues(AbstractValue<?> firstValue, AbstractValue<?> lastValue) {
        return ((firstValue == null) && (lastValue == null))
                || ((firstValue != null) && lastValue.getTimestamp().equals(firstValue.getTimestamp()))
                || ((lastValue != null) && firstValue.getTimestamp().equals(lastValue.getTimestamp()));
    }

    public List<DatasetTypesMetadata> getDatasetTypesMetadata(DbQuery dbQuery) {
        return getDatasetRepository().getDatasetTypesMetadata(createFilterPredicate(dbQuery));
    }
}
