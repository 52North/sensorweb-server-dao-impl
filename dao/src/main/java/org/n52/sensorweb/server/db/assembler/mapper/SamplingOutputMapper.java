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

import java.util.LinkedList;
import java.util.List;

import org.hibernate.Hibernate;
import org.n52.io.request.IoParameters;
import org.n52.io.response.FeatureOutput;
import org.n52.io.response.sampling.MeasuringProgramOutput;
import org.n52.io.response.sampling.SamplerOutput;
import org.n52.io.response.sampling.SamplingObservationOutput;
import org.n52.io.response.sampling.SamplingOutput;
import org.n52.sensorweb.server.db.TimeOutputCreator;
import org.n52.sensorweb.server.db.old.dao.DbQuery;
import org.n52.series.db.beans.DataEntity;
import org.n52.series.db.beans.sampling.MeasuringProgramEntity;
import org.n52.series.db.beans.sampling.SamplingEntity;

public class SamplingOutputMapper extends ParameterOutputSearchResultMapper<SamplingEntity, SamplingOutput>
        implements TimeOutputCreator {

    public SamplingOutputMapper(DbQuery query, OutputMapperFactory outputMapperFactory) {
        super(query, outputMapperFactory);
    }

    @Override
    public SamplingOutput createCondensed(SamplingEntity entity, SamplingOutput output) {
        return condensed(entity, super.createCondensed(entity, output));
    }

    private SamplingOutput condensed(SamplingEntity entity, SamplingOutput output) {
        SamplingOutput result = super.createCondensed(entity, output);
        IoParameters parameters = query.getParameters();
        result.setValue(SamplingOutput.COMMENT, entity.isSetDescription() ? entity.getDescription() : "", parameters,
                result::setComment);
        result.setValue(SamplingOutput.MONITORING_PROGRAM,
                getCondensedMeasuringProgram(entity.getMeasuringProgram(), query), parameters,
                result::setMeasuringProgram);
        result.setValue(SamplingOutput.SAMPLER, getCondensedSampler(entity.getSampler(), parameters), parameters,
                result::setSampler);
        result.setValue(SamplingOutput.SAMPLING_METHOD, entity.getSamplingMethod(), parameters,
                result::setSamplingMethod);
        result.setValue(SamplingOutput.ENVIRONMENTAL_CONDITIONS,
                entity.isSetEnvironmentalConditions() ? entity.getEnvironmentalConditions() : "", parameters,
                result::setEnvironmentalConditions);
        result.setValue(SamplingOutput.SAMPLING_TIME_START,
                createTimeOutput(entity.getSamplingTimeStart(), parameters), parameters, result::setSamplingTimeStart);
        result.setValue(SamplingOutput.SAMPLING_TIME_END, createTimeOutput(entity.getSamplingTimeEnd(), parameters),
                parameters, result::setSamplingTimeEnd);
        return result;
    }

    @Override
    public SamplingOutput addExpandedValues(SamplingEntity entity, SamplingOutput output) {
        IoParameters parameters = query.getParameters();
        output.setValue(SamplingOutput.FEATURE, getFeature(entity, query), parameters, output::setFeature);
        output.setValue(SamplingOutput.SAMPLING_OBSERVATIONS, getSamplingObservations(entity, query), parameters,
                output::setSamplingObservations);
        return output;
    }

    private MeasuringProgramOutput getCondensedMeasuringProgram(MeasuringProgramEntity entity, DbQuery query) {
        return getOutputMapperFactory().getMeasuringProgramOutputMapper(query).createCondensed(entity,
                new MeasuringProgramOutput());
    }

    private SamplerOutput getCondensedSampler(String sampler, IoParameters parameters) {
        if (sampler != null) {
            SamplerOutput result = new SamplerOutput();
            result.setValue(SamplerOutput.LABEL, sampler, parameters, result::setLabel);
            return result;
        }
        return null;
    }

    private FeatureOutput getFeature(SamplingEntity entity, DbQuery query) {
        return entity.getObservations() != null ? entity.getObservations().stream().map(o -> {
            return getFeatureOutput(o.getDataset().getFeature(), query);
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
        result.setDataset(getDatasetOutput(o.getDataset(), query));
        result.setCategory(getCategoryOutput(o.getDataset().getCategory(), query));
        result.setOffering(getOfferingOutput(o.getDataset().getOffering(), query));
        result.setPhenomenon(getPhenomenonOutput(o.getDataset().getPhenomenon(), query));
        result.setPlatform(getPlatformOutput(o.getDataset().getPlatform(), query));
        result.setProcedure(getProcedureOutput(o.getDataset().getProcedure(), query));
        return result;
    }

    @Override
    public SamplingOutput getParameterOuput() {
        return new SamplingOutput();
    }
}
