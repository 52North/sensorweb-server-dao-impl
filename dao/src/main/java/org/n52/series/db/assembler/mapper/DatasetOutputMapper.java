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
import org.n52.io.response.dataset.AbstractValue;
import org.n52.io.response.dataset.DatasetOutput;
import org.n52.sensorweb.server.db.old.dao.DbQuery;
import org.n52.series.db.beans.AbstractFeatureEntity;
import org.n52.series.db.beans.DatasetEntity;
import org.n52.series.db.beans.DescribableEntity;
import org.n52.series.db.beans.OfferingEntity;
import org.n52.series.db.beans.PhenomenonEntity;
import org.n52.series.db.beans.ProcedureEntity;

public class DatasetOutputMapper<V extends AbstractValue<?>>
        extends ParameterOutputSearchResultMapper<DatasetEntity, DatasetOutput<V>> {

    public DatasetOutputMapper(DbQuery query, OutputMapperFactory outputMapperFactory) {
        super(query, outputMapperFactory);
    }

    @Override
    public DatasetOutput<V> createCondensed(DatasetEntity entity, DatasetOutput<V> output) {
        return condensed(entity, output);
    }

    private DatasetOutput<V> condensed(DatasetEntity entity, DatasetOutput<V> output) {
        super.createCondensed(entity, output);
        IoParameters parameters = query.getParameters();

        output.setValue(DatasetOutput.UOM, entity.getUnitI18nName(query.getLocale()), parameters, output::setUom);
        output.setValue(DatasetOutput.DATASET_TYPE, entity.getDatasetType().name(), parameters,
                output::setDatasetType);
        output.setValue(DatasetOutput.OBSERVATION_TYPE, entity.getObservationType().name(), parameters,
                output::setObservationType);
        output.setValue(DatasetOutput.VALUE_TYPE, entity.getValueType().name(), parameters, output::setValueType);
        output.setValue(DatasetOutput.MOBILE, entity.isMobile(), parameters, output::setMobile);
        output.setValue(DatasetOutput.INSITU, entity.isInsitu(), parameters, output::setInsitu);
        output.setValue(DatasetOutput.ORIGIN_TIMEZONE,
                entity.isSetOriginTimezone() ? entity.getOriginTimezone() : "UTC", parameters,
                output::setOriginTimezone);
        output.setValue(DatasetOutput.SMAPLING_TIME_START,
                createTimeOutput(entity.getFirstValueAt(), entity.getOriginTimezone(), parameters), parameters,
                output::setSamplingTimeStart);
        output.setValue(DatasetOutput.SMAPLING_TIME_END,
                createTimeOutput(entity.getLastValueAt(), entity.getOriginTimezone(), parameters), parameters,
                output::setSamplingTimeEnd);
        output.setValue(DatasetOutput.FEATURE, getFeatureOutput(entity.getFeature(), query), parameters,
                output::setFeature);
        return output;
    }

    @Override
    protected <E extends DescribableEntity> String createLabel(E entity) {
        return createLabel((DatasetEntity) entity);
    }

    private String createLabel(DatasetEntity entity) {
        String label = super.createLabel(entity);
        if (label != null && !label.isEmpty()) {
            PhenomenonEntity phenomenon = entity.getPhenomenon();
            ProcedureEntity procedure = entity.getProcedure();
            OfferingEntity offering = entity.getOffering();
            AbstractFeatureEntity<?> feature = entity.getFeature();
            String locale = query.getLocale();
            String procedureLabel = procedure.getLabelFrom(locale);
            String phenomenonLabel = phenomenon.getLabelFrom(locale);
            String offeringLabel = offering.getLabelFrom(locale);
            String stationLabel = feature.getLabelFrom(locale);

            StringBuilder sb = new StringBuilder();
            return sb.append(phenomenonLabel).append(" ").append(procedureLabel).append(", ").append(stationLabel)
                    .append(", ").append(offeringLabel).toString();
        }
        return label;
    }

    @Override
    public DatasetOutput<V> getParameterOuput() {
        return new DatasetOutput<>();
    }

}
