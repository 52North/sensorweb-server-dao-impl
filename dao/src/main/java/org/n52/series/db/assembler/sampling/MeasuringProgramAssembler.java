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
package org.n52.series.db.assembler.sampling;

import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.transaction.annotation.Transactional;

import org.n52.io.request.IoParameters;
import org.n52.io.response.CategoryOutput;
import org.n52.io.response.FeatureOutput;
import org.n52.io.response.PhenomenonOutput;
import org.n52.io.response.dataset.DatasetOutput;
import org.n52.io.response.sampling.MeasuringProgramOutput;
import org.n52.io.response.sampling.SamplingOutput;
import org.n52.series.db.assembler.ParameterDatasetOutputAssembler;
import org.n52.series.db.assembler.ParameterOutputAssembler;
import org.n52.series.db.assembler.mapper.MeasuringProgramOutputMapper;
import org.n52.series.db.assembler.mapper.ParameterOutputSearchResultMapper;
import org.n52.series.db.beans.sampling.MeasuringProgramEntity;
import org.n52.series.db.old.dao.DbQuery;
import org.n52.series.db.repositories.core.DatasetRepository;
import org.n52.series.db.repositories.sampling.MeasuringProgramRepository;
import org.n52.series.spi.search.MeasuringProgramSearchResult;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

@Component
@Profile("sampling")
@Transactional
public class MeasuringProgramAssembler
        extends ParameterOutputAssembler<MeasuringProgramEntity, MeasuringProgramOutput, MeasuringProgramSearchResult>
        implements ParameterDatasetOutputAssembler {

    public MeasuringProgramAssembler(MeasuringProgramRepository parameterRepository,
            DatasetRepository datasetRepository) {
        super(parameterRepository, datasetRepository);
    }

    @Override
    protected MeasuringProgramOutput prepareEmptyOutput() {
        return new MeasuringProgramOutput();
    }

    @Override
    protected MeasuringProgramSearchResult prepareEmptySearchResult() {
        return new MeasuringProgramSearchResult();
    }

    @Override
    protected Specification<MeasuringProgramEntity> createPublicPredicate(String id, DbQuery query) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    protected Specification<MeasuringProgramEntity> createFilterPredicate(DbQuery query) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    protected MeasuringProgramOutput createExpanded(MeasuringProgramEntity entity, DbQuery query) {
        IoParameters parameters = query.getParameters();
        MeasuringProgramOutput result = super.createExpanded(entity, query);
        result.setValue(MeasuringProgramOutput.DATASETS, getDatasets(entity, query), parameters, result::setDatasets);
        result.setValue(MeasuringProgramOutput.SAMPLINGS, getSamplings(entity, query), parameters,
                result::setSamplings);
        result.setValue(MeasuringProgramOutput.FEATURES, getFeatures(entity, query), parameters, result::setFeatures);
        result.setValue(MeasuringProgramOutput.PHENOMENA, getPhenomena(entity, query), parameters,
                result::setPhenomena);
        result.setValue(MeasuringProgramOutput.CATEGORIES, getCategories(entity, query), parameters,
                result::setCategories);
        return result;
    }

    @Override
    protected ParameterOutputSearchResultMapper getMapper(DbQuery query) {
        return new MeasuringProgramOutputMapper(query);
    }

    private List<DatasetOutput<?>> getDatasets(MeasuringProgramEntity measuringProgram, DbQuery query) {
        return measuringProgram.getDatasets() != null
                ? measuringProgram.getDatasets().stream().map(d -> getDataset(d, query)).collect(Collectors.toList())
                : new LinkedList<>();
    }

    private List<SamplingOutput> getSamplings(MeasuringProgramEntity measuringProgram, DbQuery query) {
        return measuringProgram.getSamplings() != null ? measuringProgram.getSamplings().stream()
                .map(s -> getDefaultMapper(query).createCondensed(s, new SamplingOutput()))
                .collect(Collectors.toList()) : new LinkedList<>();
    }

    private List<FeatureOutput> getFeatures(MeasuringProgramEntity measuringProgram, DbQuery query) {
        return measuringProgram.getDatasets() != null
                ? measuringProgram.getDatasets().stream().map(d -> getFeature(d, query)).collect(Collectors.toList())
                : new LinkedList<>();
    }

    private List<PhenomenonOutput> getPhenomena(MeasuringProgramEntity measuringProgram, DbQuery query) {
        return measuringProgram.getDatasets() != null ? measuringProgram.getDatasets().stream()
                .map(d -> getPhenomenon(d, query)).collect(Collectors.toList()) : new LinkedList<>();
    }

    private List<CategoryOutput> getCategories(MeasuringProgramEntity measuringProgram, DbQuery query) {
        return measuringProgram.getDatasets() != null
                ? measuringProgram.getDatasets().stream().map(d -> getCategory(d, query)).collect(Collectors.toList())
                : new LinkedList<>();
    }

}
