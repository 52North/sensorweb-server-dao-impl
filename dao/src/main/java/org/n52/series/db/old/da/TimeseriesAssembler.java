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

package org.n52.series.db.old.da;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.hibernate.Session;
import org.n52.io.request.IoParameters;
import org.n52.io.response.ParameterOutput;
import org.n52.io.response.dataset.DatasetOutput;
import org.n52.io.response.dataset.DatasetParameters;
import org.n52.io.response.dataset.ReferenceValueOutput;
import org.n52.io.response.dataset.StationOutput;
import org.n52.io.response.dataset.TimeseriesMetadataOutput;
import org.n52.io.response.dataset.quantity.QuantityValue;
import org.n52.series.db.ValueAssembler;
import org.n52.series.db.beans.AbstractFeatureEntity;
import org.n52.series.db.beans.DatasetEntity;
import org.n52.series.db.beans.OfferingEntity;
import org.n52.series.db.beans.PhenomenonEntity;
import org.n52.series.db.beans.ProcedureEntity;
import org.n52.series.db.beans.QuantityDataEntity;
import org.n52.series.db.beans.QuantityDatasetEntity;
import org.n52.series.db.old.HibernateSessionStore;
import org.n52.series.db.old.dao.DataDao;
import org.n52.series.db.old.dao.DatasetDao;
import org.n52.series.db.old.dao.DbQuery;
import org.n52.series.db.old.dao.DbQueryFactory;
import org.n52.series.spi.search.SearchResult;
import org.n52.series.spi.search.TimeseriesSearchResult;
import org.n52.series.srv.OutputAssembler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * @author <a href="mailto:h.bredel@52north.org">Henning Bredel</a>
 */
@Component
public class TimeseriesAssembler extends SessionAwareAssembler implements OutputAssembler<TimeseriesMetadataOutput> {

    private static final Logger LOGGER = LoggerFactory.getLogger(TimeseriesAssembler.class);

    private final StationAssembler stationRepository;

    private final PlatformAssembler platformRepository;

    private final ValueAssembler<QuantityDatasetEntity, QuantityDataEntity, QuantityValue, BigDecimal> dataAssembler;

    public TimeseriesAssembler(PlatformAssembler platformAssembler,
                               StationAssembler stationAssembler,
                               ValueAssembler<QuantityDatasetEntity, QuantityDataEntity, QuantityValue, BigDecimal> dataAssembler,
                               HibernateSessionStore sessionStore,
                               DbQueryFactory dbQueryFactory) {
        super(sessionStore, dbQueryFactory);
        this.platformRepository = platformAssembler;
        this.stationRepository = stationAssembler;
        this.dataAssembler = dataAssembler;
    }

    private DatasetDao<QuantityDatasetEntity> createDatasetDao(Session session) {
        return new DatasetDao<>(session, QuantityDatasetEntity.class);
    }

    private DataDao<QuantityDataEntity> createDataDao(Session session) {
        return new DataDao<>(session, QuantityDataEntity.class);
    }

    @Override
    public boolean exists(String id, DbQuery parameters) {
        Session session = getSession();
        try {
            DatasetDao<QuantityDatasetEntity> dao = createDatasetDao(session);
            return dao.hasInstance(parseId(id), parameters);
        } finally {
            returnSession(session);
        }
    }

    @Override
    public Collection<SearchResult> searchFor(DbQuery query) {
        Session session = getSession();
        try {
            DatasetDao<QuantityDatasetEntity> seriesDao = createDatasetDao(session);
            List<QuantityDatasetEntity> found = seriesDao.find(query);
            return convertToResults(found, query.getLocale());
        } finally {
            returnSession(session);
        }
    }

    private List<SearchResult> convertToResults(List<QuantityDatasetEntity> found, String locale) {
        List<SearchResult> results = new ArrayList<>();
        for (DatasetEntity searchResult : found) {
            String id = searchResult.getId()
                                    .toString();
            AbstractFeatureEntity< ? > feature = searchResult.getFeature();
            PhenomenonEntity phenomenon = searchResult.getPhenomenon();
            ProcedureEntity procedure = searchResult.getProcedure();
            OfferingEntity offering = searchResult.getOffering();
            String phenomenonLabel = phenomenon.getLabelFrom(locale);
            String procedureLabel = procedure.getLabelFrom(locale);
            String stationLabel = feature.getLabelFrom(locale);
            String offeringLabel = offering.getLabelFrom(locale);
            String label = createTimeseriesLabel(phenomenonLabel, procedureLabel, stationLabel, offeringLabel);
            results.add(new TimeseriesSearchResult(id, label));
        }
        return results;
    }

    @Override
    public List<TimeseriesMetadataOutput> getAllCondensed(DbQuery query) {
        Session session = getSession();
        try {
            return getAllCondensed(query, session);
        } finally {
            returnSession(session);
        }
    }

    private List<TimeseriesMetadataOutput> getAllCondensed(DbQuery query, Session session) {
        List<TimeseriesMetadataOutput> results = new ArrayList<>();
        DatasetDao<QuantityDatasetEntity> seriesDao = createDatasetDao(session);
        for (QuantityDatasetEntity timeseries : seriesDao.getAllInstances(query)) {
            results.add(createCondensed(timeseries, query, session));
        }
        return results;
    }

    @Override
    public List<TimeseriesMetadataOutput> getAllExpanded(DbQuery query) {
        Session session = getSession();
        try {
            return getAllExpanded(query, session);
        } finally {
            returnSession(session);
        }
    }

    private List<TimeseriesMetadataOutput> getAllExpanded(DbQuery query, Session session) {
        List<TimeseriesMetadataOutput> results = new ArrayList<>();
        DatasetDao<QuantityDatasetEntity> seriesDao = createDatasetDao(session);
        for (QuantityDatasetEntity timeseries : seriesDao.getAllInstances(query)) {
            results.add(createExpanded(timeseries, query, session));
        }
        return results;
    }

    @Override
    public TimeseriesMetadataOutput getInstance(String timeseriesId, DbQuery dbQuery) {
        Session session = getSession();
        try {
            return getInstance(timeseriesId, dbQuery, session);
        } finally {
            returnSession(session);
        }
    }

    private TimeseriesMetadataOutput getInstance(String timeseriesId, DbQuery query, Session session) {
        DatasetDao<QuantityDatasetEntity> seriesDao = createDatasetDao(session);
        QuantityDatasetEntity result = seriesDao.getInstance(parseId(timeseriesId), query);
        if (result == null) {
            LOGGER.debug("Resource with id '" + timeseriesId + "' could not be found.");
            return null;
        }
        result.setPlatform(platformRepository.getPlatformEntity(result, query, session));
        return createExpanded(result, query, session);
    }

    private TimeseriesMetadataOutput createExpanded(QuantityDatasetEntity dataset, DbQuery query, Session session) {
        QuantityValue firstValue = dataAssembler.getFirstValue(dataset, query);
        QuantityValue lastValue = dataAssembler.getLastValue(dataset, query);

        List<ReferenceValueOutput<QuantityValue>> referenceValues = createReferenceValueOutputs(dataset,
                                                                                                session,
                                                                                                query);
        DatasetParameters timeseries = createTimeseriesOutput(dataset, query.withoutFieldsFilter());

        IoParameters parameter = query.getParameters();
        TimeseriesMetadataOutput result = createCondensed(dataset, query, session);
        result.setValue(DatasetOutput.REFERENCE_VALUES, referenceValues, parameter, result::setReferenceValues);
        result.setValue(DatasetOutput.DATASET_PARAMETERS, timeseries, parameter, result::setDatasetParameters);
        result.setValue(DatasetOutput.FIRST_VALUE, firstValue, parameter, result::setFirstValue);
        result.setValue(DatasetOutput.LAST_VALUE, lastValue, parameter, result::setLastValue);
        return result;
    }

    private List<ReferenceValueOutput<QuantityValue>> createReferenceValueOutputs(QuantityDatasetEntity series,
                                                                                  Session session,
                                                                                  DbQuery query) {
        List<ReferenceValueOutput<QuantityValue>> outputs = new ArrayList<>();
        List<QuantityDatasetEntity> referenceValues = series.getReferenceValues();
        DataDao<QuantityDataEntity> dataDao = createDataDao(session);
        for (DatasetEntity referenceSeriesEntity : referenceValues) {
            if (referenceSeriesEntity.isPublished() && (referenceSeriesEntity instanceof QuantityDatasetEntity)) {
                ReferenceValueOutput<QuantityValue> refenceValueOutput = new ReferenceValueOutput<>();
                ProcedureEntity procedure = referenceSeriesEntity.getProcedure();
                refenceValueOutput.setLabel(procedure.getNameI18n(query.getLocale()));
                refenceValueOutput.setReferenceValueId(referenceSeriesEntity.getId()
                                                                            .toString());

                QuantityDataEntity lastValue = dataDao.getDataValueViaTimeend(series, query);
                QuantityDatasetEntity datasetEntity = (QuantityDatasetEntity) referenceSeriesEntity;
                refenceValueOutput.setLastValue(dataAssembler.assembleDataValueWithMetadata(lastValue, datasetEntity, query));
                outputs.add(refenceValueOutput);
            }
        }
        return outputs;
    }

    private TimeseriesMetadataOutput createCondensed(DatasetEntity entity, DbQuery query, Session session) {
        IoParameters parameters = query.getParameters();
        TimeseriesMetadataOutput result = new TimeseriesMetadataOutput(parameters);
        String locale = query.getLocale();
        AbstractFeatureEntity<?> feature = entity.getFeature();
        OfferingEntity offering = entity.getOffering();
        PhenomenonEntity phenomenon = entity.getPhenomenon();
        ProcedureEntity procedure = entity.getProcedure();
        String phenomenonLabel = phenomenon.getLabelFrom(locale);
        String procedureLabel = procedure.getLabelFrom(locale);
        String stationLabel = feature.getLabelFrom(locale);
        String offeringLabel = offering.getLabelFrom(locale);

        Long id = entity.getId();
        String uom = entity.getUnitI18nName(locale);
        String label = createTimeseriesLabel(phenomenonLabel, procedureLabel, stationLabel, offeringLabel);
        StationOutput station = createCondensedStation(entity, query.withoutFieldsFilter(), session);

        result.setId(id.toString());
        result.setValue(ParameterOutput.LABEL, label, parameters, result::setLabel);
        result.setValue(DatasetOutput.UOM, uom, parameters, result::setUom);
        result.setValue(TimeseriesMetadataOutput.STATION, station, parameters, result::setStation);

        return result;
    }

    private String createTimeseriesLabel(String phenomenon, String procedure, String station, String offering) {
        StringBuilder sb = new StringBuilder();
        sb.append(phenomenon)
          .append(" ");
        sb.append(procedure)
          .append(", ");
        sb.append(station)
          .append(", ");
        return sb.append(offering)
                 .toString();
    }

    private StationOutput createCondensedStation(DatasetEntity entity, DbQuery query, Session session) {
        AbstractFeatureEntity<?> feature = entity.getFeature();
        String featurePkid = Long.toString(feature.getId());

        // XXX explicit cast here
        return stationRepository.getCondensedInstance(featurePkid, query, session);
    }

}
