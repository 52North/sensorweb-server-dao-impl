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
package org.n52.series.db.da;

import java.util.LinkedList;
import java.util.List;

import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.n52.io.request.IoParameters;
import org.n52.io.response.dataset.DatasetOutput;
import org.n52.io.response.sampling.DetectionLimitOutput;
import org.n52.io.response.sampling.MeasuringProgramOutput;
import org.n52.io.response.sampling.SamplerOutput;
import org.n52.io.response.sampling.SamplingObservationOutput;
import org.n52.io.response.sampling.SamplingOutput;
import org.n52.series.db.DataRepositoryTypeFactory;
import org.n52.series.db.beans.DataEntity;
import org.n52.series.db.beans.DatasetEntity;
import org.n52.series.db.beans.sampling.MeasuringProgramEntity;
import org.n52.series.db.beans.sampling.SamplingEntity;
import org.n52.series.db.dao.AbstractDao;
import org.n52.series.db.dao.DbQuery;
import org.n52.series.db.dao.SamplingDao;
import org.n52.series.db.dao.SearchableDao;
import org.n52.series.spi.search.FeatureSearchResult;
import org.n52.series.spi.search.SearchResult;
import org.springframework.beans.factory.annotation.Autowired;

public class SamplingRepository extends ParameterRepository<SamplingEntity, SamplingOutput>
        implements OutputAssembler<SamplingOutput> {

    @Autowired
    private DataRepositoryTypeFactory dataRepositoryFactory;

    @Override
    protected SamplingOutput prepareEmptyParameterOutput() {
        return new SamplingOutput();
    }

    @Override
    protected SearchResult createEmptySearchResult(String id, String label, String baseUrl) {
        return new FeatureSearchResult(id, label, baseUrl);
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
    protected SamplingOutput createCondensed(SamplingEntity sampling, DbQuery query, Session session) {
        IoParameters parameters = query.getParameters();
        SamplingOutput result = createCondensed(prepareEmptyParameterOutput(), sampling, query);

        result.setValue(SamplingOutput.DOMAIN_ID, sampling.getIdentifier(), parameters, result::setDomainId);
        result.setValue(SamplingOutput.COMMENT, sampling.isSetDescription() ? sampling.getDescription() : "",
                parameters, result::setComment);
        result.setValue(SamplingOutput.MONITORING_PROGRAM,
                getCondensedMeasuringProgram(sampling.getMeasuringProgram(), query), parameters,
                result::setMeasuringProgram);
        result.setValue(SamplingOutput.SAMPLER, getCondensedSampler(sampling.getSampler(), parameters), parameters,
                result::setSampler);
        result.setValue(SamplingOutput.SAMPLING_METHOD, sampling.getSamplingMethod(), parameters,
                result::setSamplingMehtod);
        result.setValue(SamplingOutput.ENVIRONMENTAL_CONDITIONS,
                sampling.isSetEnvironmentalConditions() ? sampling.getEnvironmentalConditions() : "", parameters,
                result::setEnvironmentalConditions);
        result.setValue(SamplingOutput.SAMPLING_TIME_START, sampling.getSamplingTimeStart().getTime(), parameters,
                result::setSamplingTimeStart);
        result.setValue(SamplingOutput.SAMPLING_TIME_END, sampling.getSamplingTimeEnd().getTime(), parameters,
                result::setSamplingTimeEnd);

        return result;
    }

    @Override
    protected SamplingOutput createExpanded(SamplingEntity sampling, DbQuery query, Session session) {
        IoParameters parameters = query.getParameters();
        SamplingOutput result = createCondensed(sampling, query, session);
        result.setValue(SamplingOutput.LAST_SAMPLING_OBSERVATIONS, getLastSamplingObservations(sampling, query),
                parameters, result::setLastSamplingObservations);
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

    private List<SamplingObservationOutput> getLastSamplingObservations(SamplingEntity sampling, DbQuery query) {
        LinkedList<SamplingObservationOutput> result = new LinkedList<>();
        if (sampling.hasObservations()) {
            for (DataEntity<?> o : sampling.getObservations()) {
                if (o.getSamplingTimeEnd().equals(sampling.getSamplingTimeEnd())) {
                    result.add(getLastObservation((DataEntity<?>) Hibernate.unproxy(o), query));
                }
            }
        }
        return result;
//
//        return sampling.hasObservations()
//                ? sampling.getObservations().stream()
//                        .filter(o -> o.getSamplingTimeEnd().equals(sampling.getSamplingTimeEnd()))
//                        .map(o -> getLastObservation(o, query)).collect(Collectors.toList())
//                : new LinkedList<>();
        // TODO get last observation for datasets whose obs do not have a sampling id
    }

    private SamplingObservationOutput getLastObservation(DataEntity<?> o, DbQuery query) {
        SamplingObservationOutput result = new SamplingObservationOutput();
        DataRepository factory = getDataRepositoryFactory(o.getDataset());
        result.setValue(factory.assembleDataValue(o, o.getDataset(), query));
        result.setDetectionLimit(getDetectionLimit(o));
        result.setDataset(createCondensed(DatasetOutput.create(query.getParameters()), o.getDataset(), query));

        result.setCategory(getCondensedCategory(o.getDataset().getCategory(), query));
        result.setOffering(getCondensedOffering(o.getDataset().getOffering(), query));
        result.setPhenomenon(getCondensedPhenomenon(o.getDataset().getPhenomenon(), query));
        result.setPlatfrom(getCondensedPlatform(o.getDataset().getPlatform(), query));
        result.setProcedure(getCondensedProcedure(o.getDataset().getProcedure(), query));
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

    private DataRepository<DatasetEntity, ?, ?, ?> getDataRepositoryFactory(DatasetEntity dataset) {
        return dataRepositoryFactory.create(dataset.getObservationType().name(), dataset.getValueType().name(),
                DatasetEntity.class);
    }

}
