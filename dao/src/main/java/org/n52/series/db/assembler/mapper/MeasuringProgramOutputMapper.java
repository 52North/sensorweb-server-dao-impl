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

import org.locationtech.jts.geom.Geometry;
import org.n52.io.request.IoParameters;
import org.n52.io.response.ParameterOutput;
import org.n52.io.response.TimeOutput;
import org.n52.io.response.sampling.MeasuringProgramOutput;
import org.n52.io.response.sampling.ProducerOutput;
import org.n52.sensorweb.server.db.old.dao.DbQuery;
import org.n52.series.db.TimeOutputCreator;
import org.n52.series.db.beans.DatasetEntity;
import org.n52.series.db.beans.DescribableEntity;
import org.n52.series.db.beans.sampling.MeasuringProgramEntity;

public class MeasuringProgramOutputMapper extends ParameterOutputSearchResultMapper implements TimeOutputCreator {

    public MeasuringProgramOutputMapper(DbQuery query) {
        super(query);
    }

    @Override
    public <E extends DescribableEntity, O extends ParameterOutput> O createCondensed(E entity, O output) {
        return condensed((MeasuringProgramEntity) entity,
                (MeasuringProgramOutput) super.createCondensed(entity, output));
    }

    private <O extends ParameterOutput> O condensed(MeasuringProgramEntity entity, MeasuringProgramOutput output) {
        MeasuringProgramOutput result = super.createCondensed(entity, output);
        IoParameters parameters = query.getParameters();
        result.setValue(MeasuringProgramOutput.ORDER_ID, entity.getIdentifier(), parameters, result::setOrderId);
        result.setValue(MeasuringProgramOutput.MEASURING_PROGRAM_TIME_START,
                createTimeOutput(entity.getMeasuringTimeStart(), parameters), parameters,
                result::setMeasuringProgramTimeStart);
        result.setValue(MeasuringProgramOutput.MEASURING_PROGRAM_TIME_END, getMeasuringtimeEnd(entity, parameters),
                parameters, result::setMeasuringProgramTimeEnd);
        result.setValue(MeasuringProgramOutput.PRODUCER, getCondensedProducer(entity.getProducer(), parameters),
                parameters, result::setProducer);
        result.setValue(MeasuringProgramOutput.OBSERVED_AREA, getObservedArea(entity, query), parameters,
                result::setObservedArea);
        return (O) result;
    }

    private TimeOutput getMeasuringtimeEnd(MeasuringProgramEntity measuringProgram, IoParameters parameters) {
        if (measuringProgram.isSetMeasuringTimeEnd()) {
            return createTimeOutput(measuringProgram.getMeasuringTimeStart(), parameters);
        }
        return null;
    }

    private ProducerOutput getCondensedProducer(String producer, IoParameters parameters) {
        if (producer != null) {
            ProducerOutput result = new ProducerOutput();
            result.setValue(ProducerOutput.LABEL, producer, parameters, result::setLabel);
            return result;
        }
        return null;
    }

    private Geometry getObservedArea(MeasuringProgramEntity measuringProgram, DbQuery query) {
        Geometry observedArea = null;
        if (measuringProgram.hasDatasets()) {
            for (DatasetEntity dataset : measuringProgram.getDatasets()) {
                if (dataset.isSetFeature() && dataset.getFeature().isSetGeometry()) {
                    Geometry featureGeometry = createGeometry(dataset.getFeature(), query);
                    if (observedArea == null) {
                        observedArea = featureGeometry;
                    } else {
                        observedArea.getEnvelopeInternal().expandToInclude(featureGeometry.getEnvelopeInternal());
                    }
                }
            }
        }
        return observedArea;
    }
}
