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

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.n52.io.handler.DatasetFactoryException;
import org.n52.io.request.IoParameters;
import org.n52.io.request.Parameters;
import org.n52.io.response.dataset.AbstractValue;
import org.n52.io.response.dataset.DatasetOutput;
import org.n52.io.response.dataset.DatasetParameters;
import org.n52.io.response.dataset.DatasetTypesMetadata;
import org.n52.io.response.dataset.ReferenceValueOutput;
import org.n52.sensorweb.server.db.old.dao.DbQuery;
import org.n52.sensorweb.server.db.old.dao.DbQueryFactory;
import org.n52.sensorweb.server.db.query.DatasetQuerySpecifications;
import org.n52.sensorweb.server.db.repositories.core.DatasetRepository;
import org.n52.series.db.DataRepositoryTypeFactory;
import org.n52.series.db.ValueAssembler;
import org.n52.series.db.assembler.ParameterOutputAssembler;
import org.n52.series.db.assembler.mapper.ParameterOutputSearchResultMapper;
import org.n52.series.db.beans.DatasetEntity;
import org.n52.series.spi.search.DatasetSearchResult;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.util.StreamUtils;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class DatasetAssembler<V extends AbstractValue<?>>
        extends ParameterOutputAssembler<DatasetEntity, DatasetOutput<V>, DatasetSearchResult> {

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
    public List<DatasetOutput<V>> getAllExpanded(final DbQuery query) {
        return findAll(query).map(entity -> createExpanded(entity, query)).collect(Collectors.toList());
    }

    @Override
    public DatasetOutput<V> getInstance(final String id, final DbQuery query) {
        final Specification<DatasetEntity> publicEntity = createPublicPredicate(id, query);
        final Optional<DatasetEntity> entity = getParameterRepository().findOne(publicEntity);
        return entity.map(e -> createExpanded(e, query)).orElse(null);
    }

    protected DatasetOutput<V> createExpanded(DatasetEntity entity, DbQuery query) {
        IoParameters params = query.getParameters();
        ParameterOutputSearchResultMapper<DatasetEntity, DatasetOutput<V>> mapper = getMapper(query);
        DatasetOutput<V> result = mapper.createCondensed(entity);
        mapper.addExpandedValues(entity, result);
        DatasetParameters datasetParams = createDatasetParameters(entity, query.withoutFieldsFilter(), mapper);

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
        } catch (DatasetFactoryException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return result;
    }

    @Override
    public DatasetEntity getOrInsertInstance(DatasetEntity dataset) {
        IoParameters parameters = IoParameters.createDefaults();
        DatasetQuerySpecifications dsQS = DatasetQuerySpecifications.of(dbQueryFactory.createFrom(parameters), null);
        List<Specification<DatasetEntity>> specifications = new LinkedList<>();
        if (dataset.getCategory() != null && dataset.getCategory().getId() != null) {
            specifications.add(dsQS.matchFeatures(dataset.getCategory().getId().toString()));
        }
        if (dataset.getFeature() != null && dataset.getFeature().getId() != null) {
            specifications.add(dsQS.matchFeatures(dataset.getFeature().getId().toString()));
        }
        if (dataset.getProcedure() != null && dataset.getProcedure().getId() != null) {
            specifications.add(dsQS.matchProcedures(dataset.getProcedure().getId().toString()));
        }
        if (dataset.getOffering() != null && dataset.getOffering().getId() != null) {
            specifications.add(dsQS.matchOfferings(dataset.getOffering().getId().toString()));
        }
        if (dataset.getPhenomenon() != null && dataset.getPhenomenon().getId() != null) {
            specifications.add(dsQS.matchPhenomena(dataset.getPhenomenon().getId().toString()));
        }
        if (dataset.getPlatform() != null && dataset.getPlatform().getId() != null) {
            specifications.add(dsQS.matchPlatforms(dataset.getPlatform().getId().toString()));
        }
        if (dataset.getService() != null && dataset.getService().getId() != null) {
            specifications.add(dsQS.matchServices(dataset.getService().getId().toString()));
        }
        Specification<DatasetEntity> specification = null;
        for (Specification<DatasetEntity> spec : specifications) {
            if (specification != null && spec != null) {
                specification = specification.and(spec);
            } else {
                specification = spec;
            }
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

    private DatasetParameters createDatasetParameters(DatasetEntity dataset, DbQuery query,
            ParameterOutputSearchResultMapper<DatasetEntity, DatasetOutput<V>> mapper) {
        DatasetParameters metadata = new DatasetParameters();
        metadata.setService(getOutputMapperFactory().getServiceMapper(query).createCondensed(dataset.getService()));
        metadata.setOffering(getOutputMapperFactory().getOfferingMapper(query).createCondensed(dataset.getOffering()));
        metadata.setProcedure(
                getOutputMapperFactory().getProcedureMapper(query).createCondensed(dataset.getProcedure()));
        metadata.setPhenomenon(
                getOutputMapperFactory().getPhenomenonMapper(query).createCondensed(dataset.getPhenomenon()));
        metadata.setCategory(getOutputMapperFactory().getCategoryMapper(query).createCondensed(dataset.getCategory()));
        metadata.setPlatform(getOutputMapperFactory().getPlatformMapper(query).createCondensed(dataset.getPlatform()));
        if (dataset.hasTagss()) {
            metadata.setTags(dataset.getTags().parallelStream()
                    .map(t -> getOutputMapperFactory().getTagMapper(query).createCondensed(t))
                    .collect(Collectors.toList()));
        }
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

    @Override
    public Stream<DatasetEntity> findAll(DbQuery query) {
        if (query.getParameters().containsParameter(Parameters.DATASETS)) {
            DatasetQuerySpecifications dsFilterSpec = DatasetQuerySpecifications.of(query, entityManager);
            Specification<DatasetEntity> predicate =
                    dsFilterSpec.matchFilters().and(dsFilterSpec.matchIds(query.getParameters().getDatasets()));
            final Iterable<DatasetEntity> entities = getParameterRepository().findAll(predicate);
            return StreamUtils.createStreamFromIterator(entities.iterator());
        }
        return super.findAll(query);
    }

    @Override
    protected ParameterOutputSearchResultMapper<DatasetEntity, DatasetOutput<V>> getMapper(DbQuery query) {
        return getOutputMapperFactory().getDatasetMapper(query);
    }

    public List<DatasetTypesMetadata> getDatasetTypesMetadata(DbQuery dbQuery) {
        // Set<String> datasets = dbQuery.getParameters().getDatasets();

        return Collections.emptyList();
    }

    // @SuppressWarnings("unchecked")
    // public List<DatasetTypesMetadata> getDatasetTypesMetadata(Collection<String> datasets, DbQuery query) {
    // Criteria criteria = getDefaultCriteria(getDefaultAlias(), false, query);
    // if (query.isMatchDomainIds()) {
    // criteria.add(Restrictions.in(DescribableEntity.PROPERTY_DOMAIN_ID, datasets));
    // } else {
    // criteria.add(Restrictions.in(DescribableEntity.PROPERTY_ID,
    // datasets.stream().map(d -> Long.parseLong(d)).collect(Collectors.toSet())));
    // }
    // criteria.setProjection(Projections.projectionList()
    // .add(Projections.groupProperty(DatasetEntity.PROPERTY_ID))
    // .add(Projections.property(DatasetEntity.PROPERTY_DATASET_TYPE))
    // .add(Projections.property(DatasetEntity.PROPERTY_OBSERVATION_TYPE))
    // .add(Projections.property(DatasetEntity.PROPERTY_VALUE_TYPE)));
    // criteria.setResultTransformer(transformer);
    // return criteria.list();
    // }
    //
    // /**
    // * Offering time extrema {@link ResultTransformer}
    // *
    // * @author <a href="mailto:c.hollmann@52north.org">Carsten Hollmann</a>
    // * @since 4.4.0
    // *
    // */
    // private class DatasetTypesMetadataTransformer implements ResultTransformer {
    // private static final long serialVersionUID = -373512929481519459L;
    //
    // @Override
    // public DatasetTypesMetadata transformTuple(Object[] tuple, String[] aliases) {
    // DatasetTypesMetadata datasetTypesMetadata = new DatasetTypesMetadata();
    // if (tuple != null) {
    // datasetTypesMetadata.setId(tuple[0].toString());
    // datasetTypesMetadata.setDatasetType(tuple[1].toString());
    // datasetTypesMetadata.setObservationType(tuple[2].toString());
    // datasetTypesMetadata.setValueType(tuple[3].toString());
    // }
    // return datasetTypesMetadata;
    // }
    //
    // @Override
    // @SuppressWarnings({ "rawtypes"})
    // public List transformList(List collection) {
    // return collection;
    // }
    // }

}
