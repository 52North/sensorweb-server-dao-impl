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
package org.n52.series.db.assembler.sampling;

import java.util.LinkedList;
import java.util.List;

import org.hibernate.Hibernate;
import org.n52.io.request.IoParameters;
import org.n52.io.response.FeatureOutput;
import org.n52.io.response.sampling.DetectionLimitOutput;
import org.n52.io.response.sampling.SamplingObservationOutput;
import org.n52.io.response.sampling.SamplingOutput;
import org.n52.series.db.assembler.ParameterDatasetOutputAssembler;
import org.n52.series.db.assembler.mapper.ParameterOutputSearchResultMapper;
import org.n52.series.db.assembler.mapper.SamplingOutputMapper;
import org.n52.series.db.beans.DataEntity;
import org.n52.series.db.beans.sampling.SamplingEntity;
import org.n52.series.db.old.dao.DbQuery;
import org.n52.series.db.repositories.core.DatasetRepository;
import org.n52.series.db.repositories.sampling.SamplingRepository;
import org.n52.series.spi.search.SamplingSearchResult;
import org.springframework.data.jpa.domain.Specification;

public class SamplingAssembler
        extends ParameterDatasetOutputAssembler<SamplingEntity, SamplingOutput, SamplingSearchResult> {

    public SamplingAssembler(SamplingRepository parameterRepository, DatasetRepository datasetRepository) {
        super(parameterRepository, datasetRepository);
    }

    @Override
    protected SamplingOutput prepareEmptyOutput() {
        return new SamplingOutput();
    }

    @Override
    protected SamplingSearchResult prepareEmptySearchResult() {
        return new SamplingSearchResult();
    }

    @Override
    protected Specification<SamplingEntity> createPublicPredicate(String id, DbQuery query) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    protected Specification<SamplingEntity> createFilterPredicate(DbQuery query) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    protected SamplingOutput createExpanded(SamplingEntity entity, DbQuery query) {
        IoParameters parameters = query.getParameters();
        SamplingOutput result = super.createExpanded(entity, query);
        result.setValue(SamplingOutput.FEATURE, getFeature(entity, query), parameters, result::setFeature);
        result.setValue(SamplingOutput.SAMPLING_OBSERVATIONS, getSamplingObservations(entity, query),
                parameters, result::setSamplingObservations);
        return result;
    }

    @Override
    protected ParameterOutputSearchResultMapper getMapper(DbQuery query) {
        return new SamplingOutputMapper(query);
    }

    private FeatureOutput getFeature(SamplingEntity entity, DbQuery query) {
        return entity.getObservations() != null ? entity.getObservations().stream().map(o -> {
            ParameterOutputSearchResultMapper defaultMapper = getDefaultMapper(query);
            FeatureOutput output = defaultMapper.createCondensed(o.getDataset().getFeature(), new FeatureOutput());
            output.setValue(FeatureOutput.GEOMETRY, defaultMapper.createGeometry(o.getDataset().getFeature(), query),
                    query.getParameters(), output::setGeometry);
            return output;
        }).findFirst().get() : null;
    }

    private List<SamplingObservationOutput> getSamplingObservations(SamplingEntity sampling, DbQuery query) {
        LinkedList<SamplingObservationOutput> result = new LinkedList<>();
        if (sampling.hasObservations()) {
            for (DataEntity<?> o : sampling.getObservations()) {
                if (o.getSamplingTimeEnd().equals(sampling.getSamplingTimeEnd())) {
                    result.add(getLastObservation((DataEntity<?>) Hibernate.unproxy(o), query));
                }
            }
        }
        return result;
    }

    private SamplingObservationOutput getLastObservation(DataEntity<?> o, DbQuery query) {
        SamplingObservationOutput result = new SamplingObservationOutput();
        // try {
        // ValueAssembler<DataEntity<?>, ?, ?> factory =
        // (ValueAssembler<DataEntity<?>, ?, ?>)
        // getDataRepositoryFactory(o.getDataset());
        // result.setValue(factory.assembleDataValue(o, o.getDataset(), query));
        result.setDetectionLimit(getDetectionLimit(o));
        result.setDataset(getDataset(o.getDataset(), query));
        result.setCategory(getCategory(o.getDataset(), query));
        result.setOffering(getOffering(o.getDataset(), query));
        result.setPhenomenon(getPhenomenon(o.getDataset(), query));
        result.setPlatfrom(getPlatform(o.getDataset(), query));
        result.setProcedure(getProcedure(o.getDataset(), query));
        // } catch (Exception e) {
        // LOGGER.error("error while querying last observations for sampling",
        // e);
        // }
        return result;
    }

    private DetectionLimitOutput getDetectionLimit(DataEntity<?> o) {
        if (o.hasSamplingProfile() && o.getSamplingProfile().hasDetectionLimit()) {
            DetectionLimitOutput result = new DetectionLimitOutput();
            result.setFlag(o.getSamplingProfile().getDetectionLimit().getFlag());
            result.setDetectionLimit(o.getSamplingProfile().getDetectionLimit().getDetectionLimit());
            return result;
        }
        return null;
    }

}
