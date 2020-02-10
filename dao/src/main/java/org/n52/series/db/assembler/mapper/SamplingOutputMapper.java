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
package org.n52.series.db.assembler.mapper;

import org.n52.io.request.IoParameters;
import org.n52.io.response.ParameterOutput;
import org.n52.io.response.sampling.MeasuringProgramOutput;
import org.n52.io.response.sampling.SamplerOutput;
import org.n52.io.response.sampling.SamplingOutput;
import org.n52.series.db.TimeOutputCreator;
import org.n52.series.db.beans.DescribableEntity;
import org.n52.series.db.beans.sampling.MeasuringProgramEntity;
import org.n52.series.db.beans.sampling.SamplingEntity;
import org.n52.series.db.old.dao.DbQuery;

public class SamplingOutputMapper extends ParameterOutputSearchResultMapper implements TimeOutputCreator {

    public SamplingOutputMapper(DbQuery query) {
        super(query);
    }

    @Override
    public <E extends DescribableEntity, O extends ParameterOutput> O createCondensed(E entity, O output) {
        return condensed((SamplingEntity) entity, (SamplingOutput) super.createCondensed(entity, output));
    }

    private <O extends ParameterOutput> O condensed(SamplingEntity entity, SamplingOutput output) {
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
        return (O) result;
    }

    private MeasuringProgramOutput getCondensedMeasuringProgram(MeasuringProgramEntity entity, DbQuery query) {
        return new MeasuringProgramOutputMapper(query).createCondensed(entity, new MeasuringProgramOutput());
    }

    private SamplerOutput getCondensedSampler(String sampler, IoParameters parameters) {
        if (sampler != null) {
            SamplerOutput result = new SamplerOutput();
            result.setValue(SamplerOutput.LABEL, sampler, parameters, result::setLabel);
            return result;
        }
        return null;
    }

}
