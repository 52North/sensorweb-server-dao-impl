/*
 * Copyright (C) 2015-2023 52°North Spatial Information Research GmbH
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
        this(query, outputMapperFactory, false);
    }

    public SamplingOutputMapper(DbQuery query, OutputMapperFactory outputMapperFactory, boolean subMapper) {
        super(query, outputMapperFactory, subMapper);
    }

    @Override
    public SamplingOutput addExpandedValues(SamplingEntity entity, SamplingOutput output) {
        IoParameters parameters = getDbQuery().getParameters();
        if (parameters.isSelected(SamplingOutput.FEATURE)) {
        output.setValue(SamplingOutput.FEATURE, getFeature(entity), parameters, output::setFeature);
        }
        if (parameters.isSelected(SamplingOutput.SAMPLING_OBSERVATIONS)) {
        output.setValue(SamplingOutput.SAMPLING_OBSERVATIONS, getSamplingObservations(entity), parameters,
                output::setSamplingObservations);
        }
        return output;
    }

    @Override
    public void addAll(SamplingOutput result, SamplingEntity sampling, DbQuery query, IoParameters parameters) {
        super.addAll(result, sampling, query, parameters);
        addComment(result, sampling, query, parameters);
        addMonitoringProgram(result, sampling, query, parameters);
        addSampler(result, sampling, query, parameters);
        addSamplingMethod(result, sampling, query, parameters);
        addEnvironmentalConditions(result, sampling, query, parameters);
        addSamplingTimeStart(result, sampling, query, parameters);
        addSamplingTimeEnd(result, sampling, query, parameters);
    }

    @Override
    public void addSelected(SamplingOutput result, SamplingEntity sampling, DbQuery query, IoParameters parameters) {
        super.addSelected(result, sampling, query, parameters);
        for (String selected : parameters.getSelectOriginal()) {
            switch (selected) {
                case SamplingOutput.COMMENT:
                    addComment(result, sampling, query, parameters);
                    break;
                case SamplingOutput.MONITORING_PROGRAM:
                    addMonitoringProgram(result, sampling, query, parameters);
                    break;
                case SamplingOutput.SAMPLER:
                    addSampler(result, sampling, query, parameters);
                    break;
                case SamplingOutput.SAMPLING_METHOD:
                    addSamplingMethod(result, sampling, query, parameters);
                    break;
                case SamplingOutput.ENVIRONMENTAL_CONDITIONS:
                    addEnvironmentalConditions(result, sampling, query, parameters);
                    break;
                case SamplingOutput.SAMPLING_TIME_START:
                    addSamplingTimeStart(result, sampling, query, parameters);
                    break;
                case SamplingOutput.SAMPLING_TIME_END:
                    addSamplingTimeEnd(result, sampling, query, parameters);
                    break;
                default:
                    break;
            }
        }
    }

    private void addComment(SamplingOutput result, SamplingEntity sampling, DbQuery query, IoParameters parameters) {
        result.setValue(SamplingOutput.COMMENT, sampling.isSetDescription() ? sampling.getDescription() : "",
                parameters, result::setComment);
    }

    private void addMonitoringProgram(SamplingOutput result, SamplingEntity sampling, DbQuery query,
            IoParameters parameters) {
        result.setValue(SamplingOutput.MONITORING_PROGRAM,
                getCondensedMeasuringProgram(sampling.getMeasuringProgram(), query), parameters,
                result::setMeasuringProgram);
    }

    private void addSampler(SamplingOutput result, SamplingEntity sampling, DbQuery query, IoParameters parameters) {
        result.setValue(SamplingOutput.SAMPLER, getCondensedSampler(sampling.getSampler(), parameters), parameters,
                result::setSampler);
    }

    private void addSamplingMethod(SamplingOutput result, SamplingEntity sampling, DbQuery query,
            IoParameters parameters) {
        result.setValue(SamplingOutput.SAMPLING_METHOD, sampling.getSamplingMethod(), parameters,
                result::setSamplingMethod);
    }

    private void addEnvironmentalConditions(SamplingOutput result, SamplingEntity sampling, DbQuery query,
            IoParameters parameters) {
        result.setValue(SamplingOutput.ENVIRONMENTAL_CONDITIONS,
                sampling.isSetEnvironmentalConditions() ? sampling.getEnvironmentalConditions() : "", parameters,
                result::setEnvironmentalConditions);

    }

    private void addSamplingTimeStart(SamplingOutput result, SamplingEntity sampling, DbQuery query,
            IoParameters parameters) {
        result.setValue(SamplingOutput.SAMPLING_TIME_START,
                createTimeOutput(sampling.getSamplingTimeStart(), parameters), parameters,
                result::setSamplingTimeStart);
    }

    private void addSamplingTimeEnd(SamplingOutput result, SamplingEntity sampling, DbQuery query,
            IoParameters parameters) {
        result.setValue(SamplingOutput.SAMPLING_TIME_END, createTimeOutput(sampling.getSamplingTimeEnd(), parameters),
                parameters, result::setSamplingTimeEnd);
    }

    private MeasuringProgramOutput getCondensedMeasuringProgram(MeasuringProgramEntity entity, DbQuery query) {
        return getOutputMapperFactory().getMeasuringProgramMapper(query).createCondensed(entity,
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

    private FeatureOutput getFeature(SamplingEntity entity) {
        return entity.getObservations() != null ? entity.getObservations().stream().map(o -> {
            return getFeatureOutput(o.getDataset().getFeature());
        }).findFirst().get() : null;
    }

    private List<SamplingObservationOutput> getSamplingObservations(SamplingEntity sampling) {
        LinkedList<SamplingObservationOutput> result = new LinkedList<>();
        if (sampling.hasObservations()) {
            for (DataEntity<?> o : sampling.getObservations()) {
                if (o.getSamplingTimeEnd().equals(sampling.getSamplingTimeEnd())) {
                    result.add(getLastObservation((DataEntity<?>) Hibernate.unproxy(o), getDbQuery()));
                }
            }
        }
        return result;
    }

    private SamplingObservationOutput getLastObservation(DataEntity<?> o, DbQuery query) {
        SamplingObservationOutput result = new SamplingObservationOutput();
        result.setDataset(getDatasetOutput(o.getDataset(), query));
        result.setCategory(getCategoryOutput(o.getDataset().getCategory()));
        result.setOffering(getOfferingOutput(o.getDataset().getOffering()));
        result.setPhenomenon(getPhenomenonOutput(o.getDataset().getPhenomenon()));
        result.setPlatform(getPlatformOutput(o.getDataset().getPlatform()));
        result.setProcedure(getProcedureOutput(o.getDataset().getProcedure()));
        return result;
    }

    @Override
    public SamplingOutput getParameterOuput() {
        return new SamplingOutput();
    }
}
