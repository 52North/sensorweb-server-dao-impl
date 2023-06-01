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
package org.n52.series.db.da.mapper;

import org.hibernate.Session;
import org.n52.io.HrefHelper;
import org.n52.io.request.IoParameters;
import org.n52.io.response.ParameterOutput;
import org.n52.io.response.dataset.AbstractValue;
import org.n52.io.response.dataset.DatasetOutput;
import org.n52.io.response.dataset.IndividualObservationOutput;
import org.n52.io.response.dataset.ProfileOutput;
import org.n52.io.response.dataset.TimeseriesMetadataOutput;
import org.n52.io.response.dataset.TrajectoryOutput;
import org.n52.series.db.beans.AbstractFeatureEntity;
import org.n52.series.db.beans.DatasetEntity;
import org.n52.series.db.beans.OfferingEntity;
import org.n52.series.db.beans.PhenomenonEntity;
import org.n52.series.db.beans.ProcedureEntity;
import org.n52.series.db.dao.DbQuery;

public class DatasetMapper extends AbstractOuputMapper<DatasetOutput<AbstractValue<?>>, DatasetEntity> {

    public DatasetMapper(MapperFactory mapperFactory) {
        super(mapperFactory);
    }

    @Override
    public DatasetOutput<AbstractValue<?>> createCondensed(DatasetEntity dataset, DbQuery query) {
        try {
            IoParameters parameters = query.getParameters();
            DatasetOutput<AbstractValue<?>> result = new DatasetOutput();
            addService(result, dataset, query);

            Long id = dataset.getId();
            String hrefBase = query.getHrefBase();
            String domainId = dataset.getIdentifier();
            String uom = dataset.getUnitI18nName(query.getLocale());
            String label = createDatasetLabel(dataset, query.getLocale());

            result.setId(id.toString());
            result.setValue(DatasetOutput.UOM, uom, parameters, result::setUom);
            result.setValue(ParameterOutput.LABEL, label, parameters, result::setLabel);
            result.setValue(ParameterOutput.DOMAIN_ID, domainId, parameters, result::setDomainId);
            result.setValue(DatasetOutput.DATASET_TYPE, dataset.getDatasetType().name(), parameters,
                    result::setDatasetType);
            result.setValue(DatasetOutput.OBSERVATION_TYPE, dataset.getObservationType().name(), parameters,
                    result::setObservationType);
            result.setValue(DatasetOutput.VALUE_TYPE, dataset.getValueType().name(), parameters, result::setValueType);
            result.setValue(DatasetOutput.MOBILE, dataset.isMobile(), parameters, result::setMobile);
            result.setValue(DatasetOutput.INSITU, dataset.isInsitu(), parameters, result::setInsitu);
            if (dataset.hasSamplingProfile()) {
                result.setValue(DatasetOutput.HAS_SAMPLINGS, dataset.getSamplingProfile().hasSamplings(), parameters,
                        result::setHasSamplings);
            }
            result.setValue(ParameterOutput.HREF, createHref(hrefBase, dataset), parameters, result::setHref);
            result.setValue(DatasetOutput.ORIGIN_TIMEZONE,
                    dataset.isSetOriginTimezone() ? dataset.getOriginTimezone() : "UTC", parameters,
                    result::setOriginTimezone);

            result.setValue(DatasetOutput.SMAPLING_TIME_START,
                    createTimeOutput(dataset.getFirstValueAt(), dataset.getOriginTimezone(), parameters), parameters,
                    result::setSamplingTimeStart);
            result.setValue(DatasetOutput.SMAPLING_TIME_END,
                    createTimeOutput(dataset.getLastValueAt(), dataset.getOriginTimezone(), parameters), parameters,
                    result::setSamplingTimeEnd);
            result.setValue(DatasetOutput.FEATURE,
                    getMapperFactory().getFeatureMapper().createCondensed(dataset.getFeature(), query), parameters,
                    result::setFeature);

            return result;
        } catch (Exception e) {
            getLogger().error("Error while processing {} with id {}! Exception: {}",
                    dataset.getClass().getSimpleName(), dataset.getId(), e);
        }
        return null;
    }

    @Override
    public DatasetOutput createExpanded(DatasetEntity entity, DbQuery query, Session session) {
        return null;
    }

    private String createHref(String hrefBase, DatasetEntity dataset) {
        return HrefHelper.constructHref(hrefBase, getCollectionName(dataset)) + "/" + dataset.getId();
    }

    private String getCollectionName(DatasetEntity dataset) {
        switch (dataset.getDatasetType()) {
            case individualObservation:
                return IndividualObservationOutput.COLLECTION_PATH;
            case trajectory:
                return TrajectoryOutput.COLLECTION_PATH;
            case profile:
                return ProfileOutput.COLLECTION_PATH;
            case timeseries:
                return TimeseriesMetadataOutput.COLLECTION_PATH;
            default:
                return DatasetOutput.COLLECTION_PATH;
        }
    }

    private String createDatasetLabel(DatasetEntity dataset, String locale) {
        PhenomenonEntity phenomenon = dataset.getPhenomenon();
        ProcedureEntity procedure = dataset.getProcedure();
        OfferingEntity offering = dataset.getOffering();
        AbstractFeatureEntity<?> feature = dataset.getFeature();

        String procedureLabel = procedure.getLabelFrom(locale);
        String phenomenonLabel = phenomenon.getLabelFrom(locale);
        String offeringLabel = offering.getLabelFrom(locale);
        String stationLabel = feature.getLabelFrom(locale);

        StringBuilder sb = new StringBuilder();
        return sb.append(phenomenonLabel).append(", ").append(procedureLabel).append(", ").append(stationLabel)
                .append(", ").append(offeringLabel).toString();
    }

}
