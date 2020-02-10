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
import org.n52.io.response.FeatureOutput;
import org.n52.io.response.ParameterOutput;
import org.n52.io.response.dataset.DatasetOutput;
import org.n52.series.db.TimeOutputCreator;
import org.n52.series.db.beans.AbstractFeatureEntity;
import org.n52.series.db.beans.DatasetEntity;
import org.n52.series.db.beans.DescribableEntity;
import org.n52.series.db.beans.OfferingEntity;
import org.n52.series.db.beans.PhenomenonEntity;
import org.n52.series.db.beans.ProcedureEntity;
import org.n52.series.db.old.dao.DbQuery;

public class DatasetOutputMapper
        extends ParameterOutputSearchResultMapper
        implements TimeOutputCreator {

    public DatasetOutputMapper(DbQuery query) {
        super(query);
    }

    @Override
    public <E extends DescribableEntity, O extends ParameterOutput> O createCondensed(E entity, O output) {
        return condensed((DatasetEntity) entity,
                (DatasetOutput) super.createCondensed(entity, output));
    }

    private <O extends ParameterOutput> O condensed(DatasetEntity entity, DatasetOutput<?> result) {
        IoParameters parameters = query.getParameters();
        result.setValue(DatasetOutput.UOM,  entity.getUnitI18nName(query.getLocale()), parameters, result::setUom);
        result.setValue(DatasetOutput.DATASET_TYPE, entity.getDatasetType().name(), parameters,
                result::setDatasetType);
        result.setValue(DatasetOutput.OBSERVATION_TYPE, entity.getObservationType().name(), parameters,
                result::setObservationType);
        result.setValue(DatasetOutput.VALUE_TYPE, entity.getValueType().name(), parameters, result::setValueType);
        result.setValue(DatasetOutput.MOBILE, entity.isMobile(), parameters, result::setMobile);
        result.setValue(DatasetOutput.INSITU, entity.isInsitu(), parameters, result::setInsitu);
        result.setValue(DatasetOutput.ORIGIN_TIMEZONE,
                entity.isSetOriginTimezone() ? entity.getOriginTimezone() : "UTC", parameters,
                result::setOriginTimezone);
        result.setValue(DatasetOutput.SMAPLING_TIME_START,
                createTimeOutput(entity.getFirstValueAt(), entity.getOriginTimezone(), parameters), parameters,
                result::setSamplingTimeStart);
        result.setValue(DatasetOutput.SMAPLING_TIME_END,
                createTimeOutput(entity.getLastValueAt(), entity.getOriginTimezone(), parameters), parameters,
                result::setSamplingTimeEnd);
        result.setValue(DatasetOutput.FEATURE, getCondensedFeature(entity.getFeature(), query), parameters,
                result::setFeature);
        return (O) result;
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

    private FeatureOutput getCondensedFeature(AbstractFeatureEntity<?> entity, DbQuery query) {
        return new FeatureOutputMapper(query).createCondensed(entity, new FeatureOutput());
    }

}
