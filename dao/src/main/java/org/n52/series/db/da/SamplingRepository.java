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

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.n52.io.request.IoParameters;
import org.n52.io.response.FeatureOutput;
import org.n52.io.response.OptionalOutput;
import org.n52.io.response.dataset.DatasetOutput;
import org.n52.io.response.sampling.MeasuringProgramOutput;
import org.n52.io.response.sampling.SamplerOutput;
import org.n52.io.response.sampling.SamplingObservationOutput;
import org.n52.io.response.sampling.SamplingOutput;
import org.n52.series.db.DataAccessException;
import org.n52.series.db.DataRepositoryTypeFactory;
import org.n52.series.db.beans.DataEntity;
import org.n52.series.db.beans.DatasetEntity;
import org.n52.series.db.beans.sampling.MeasuringProgramEntity;
import org.n52.series.db.beans.sampling.SamplingEntity;
import org.n52.series.db.dao.AbstractDao;
import org.n52.series.db.dao.DbQuery;
import org.n52.series.db.dao.SamplingDao;
import org.n52.series.db.dao.SearchableDao;
import org.n52.series.spi.search.SamplingSearchResult;
import org.n52.series.spi.search.SearchResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

public class SamplingRepository extends ParameterRepository<SamplingEntity, SamplingOutput>
        implements OutputAssembler<SamplingOutput> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SamplingRepository.class);

    @Autowired
    private DataRepositoryTypeFactory dataRepositoryFactory;

    @Override
    protected SamplingOutput prepareEmptyParameterOutput() {
        return new SamplingOutput();
    }

    @Override
    protected SearchResult createEmptySearchResult(String id, String label, String baseUrl) {
        return new SamplingSearchResult().setId(id).setLabel(label).setBaseUrl(baseUrl);
    }

    @Override
    protected AbstractDao<SamplingEntity> createDao(Session session) {
        return new SamplingDao(session);
    }

    @Override
    protected SearchableDao<SamplingEntity> createSearchableDao(Session session) {
        return new SamplingDao(session);
    }

    @Override
    protected List<SamplingOutput> createCondensed(Collection<SamplingEntity> samplings,
            DbQuery query, Session session) throws DataAccessException {
        long start = System.currentTimeMillis();
        List<SamplingOutput> results = new LinkedList<>();
        for (SamplingEntity sampling : samplings) {
            results.add(createCondensed(sampling, query, session));
        }
        LOGGER.debug("Processing all condensed instances takes {} ms", System.currentTimeMillis() - start);
        return results;
    }

    @Override
    protected SamplingOutput createCondensed(SamplingEntity sampling, DbQuery query, Session session) {
        IoParameters parameters = query.getParameters();
        SamplingOutput result = createCondensed(prepareEmptyParameterOutput(), sampling, query);

        if (parameters.isSelected(SamplingOutput.COMMENT)) {
            result.setValue(SamplingOutput.COMMENT, sampling.isSetDescription() ? sampling.getDescription() : "",
                    parameters, result::setComment);
        }
        if (parameters.isSelected(SamplingOutput.MONITORING_PROGRAM)) {
            result.setValue(SamplingOutput.MONITORING_PROGRAM,
                    getCondensedMeasuringProgram(sampling.getMeasuringProgram(), query), parameters,
                    result::setMeasuringProgram);
        }
        if (parameters.isSelected(SamplingOutput.SAMPLER)) {
            result.setValue(SamplingOutput.SAMPLER, getCondensedSampler(sampling.getSampler(), parameters), parameters,
                    result::setSampler);
        }
        if (parameters.isSelected(SamplingOutput.SAMPLING_METHOD)) {
            result.setValue(SamplingOutput.SAMPLING_METHOD, sampling.getSamplingMethod(), parameters,
                    result::setSamplingMethod);
        }
        if (parameters.isSelected(SamplingOutput.ENVIRONMENTAL_CONDITIONS)) {
            result.setValue(SamplingOutput.ENVIRONMENTAL_CONDITIONS,
                    sampling.isSetEnvironmentalConditions() ? sampling.getEnvironmentalConditions() : "", parameters,
                    result::setEnvironmentalConditions);
        }
        if (parameters.isSelected(SamplingOutput.SAMPLING_TIME_START)) {
            result.setValue(SamplingOutput.SAMPLING_TIME_START,
                    createTimeOutput(sampling.getSamplingTimeStart(), parameters), parameters,
                    result::setSamplingTimeStart);
        }
        if (parameters.isSelected(SamplingOutput.SAMPLING_TIME_END)) {
            result.setValue(SamplingOutput.SAMPLING_TIME_END,
                    createTimeOutput(sampling.getSamplingTimeEnd(), parameters), parameters,
                    result::setSamplingTimeEnd);
        }

        return result;
    }

    @Override
    protected List<SamplingOutput> createExpanded(Collection<SamplingEntity> samplings,
            DbQuery query, Session session) throws DataAccessException {
//        long start = System.currentTimeMillis();
//        List<SamplingOutput> results = new LinkedList<>();
//        for (SamplingEntity sampling : samplings) {
//            results.add(createExpanded(sampling, query, session));
//        }
//        LOGGER.debug("Processing all expanded instances takes {} ms", System.currentTimeMillis() - start);
        return createCondensed(samplings, query, session);
    }

    @Override
    protected SamplingOutput createExpanded(SamplingEntity sampling, DbQuery query, Session session) {
        IoParameters parameters = query.getParameters();
        SamplingOutput result = createCondensed(sampling, query, session);
        if (parameters.isSelected(SamplingOutput.FEATURE)) {
            result.setValue(SamplingOutput.FEATURE, getFeature(sampling, query), parameters, result::setFeature);
        }
        if (parameters.isSelected(SamplingOutput.SAMPLING_OBSERVATIONS)) {
            result.setValue(SamplingOutput.SAMPLING_OBSERVATIONS, getSamplingObservations(sampling, query), parameters,
                    result::setSamplingObservations);
        }
        return result;
    }

    private MeasuringProgramOutput getCondensedMeasuringProgram(MeasuringProgramEntity entity, DbQuery query) {
        return createCondensed(new MeasuringProgramOutput(), entity, query);
    }

    private SamplerOutput getCondensedSampler(String sampler, IoParameters parameters) {
        if (sampler != null) {
            SamplerOutput result = new SamplerOutput();
            result.setValue(SamplerOutput.LABEL, sampler, parameters, result::setLabel);
            return result;
        }
        return null;
    }

    private FeatureOutput getFeature(SamplingEntity sampling, DbQuery query) {
        if (sampling.hasDatasets()) {
            return getCondensedFeature(sampling.getDatasets().iterator().next().getFeature(), query);
        } else if (sampling.hasObservations()) {
            return getCondensedFeature(sampling.getObservations().iterator().next().getDataset().getFeature(), query);
        }
        return null;
    }

    private List<SamplingObservationOutput> getSamplingObservations(SamplingEntity sampling, DbQuery query) {
        SortedSet<DataEntity<?>> observations = new TreeSet<>();
        if (sampling.hasObservations()) {
            for (DataEntity<?> o : sampling.getObservations()) {
                if (!o.hasParent()) {
                    observations.add((DataEntity<?>) Hibernate.unproxy(o));
                }
            }
        }
        return observations.stream().map(o -> getObservation(o, query)).collect(Collectors.toList());
    }

    private SamplingObservationOutput getObservation(DataEntity<?> o, DbQuery query) {
        SamplingObservationOutput result = new SamplingObservationOutput();
        DataRepository factory = getDataRepositoryFactory(o.getDataset());
        result.setValue(factory.assembleDataValue(o, o.getDataset(), query));

        String uom = o.getDataset().getUnitI18nName(query.getLocaleForLabel());
        result.setUom(uom != null && !uom.isEmpty() ? OptionalOutput.of(uom) : null);
        result.setDataset(createCondensed(new DatasetOutput(), o.getDataset(), query));
        result.setCategory(getCondensedCategory(o.getDataset().getCategory(), query));
        result.setOffering(getCondensedOffering(o.getDataset().getOffering(), query));
        result.setPhenomenon(getCondensedPhenomenon(o.getDataset().getPhenomenon(), query));
        result.setPlatform(getCondensedPlatform(o.getDataset().getPlatform(), query));
        result.setProcedure(getCondensedProcedure(o.getDataset().getProcedure(), query));
        return result;
    }

    private DataRepository<DatasetEntity, ?, ?, ?> getDataRepositoryFactory(DatasetEntity dataset) {
        return dataRepositoryFactory.create(dataset.getObservationType().name(), dataset.getValueType().name(),
                DatasetEntity.class);
    }

}
