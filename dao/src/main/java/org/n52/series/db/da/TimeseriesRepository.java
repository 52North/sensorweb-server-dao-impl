/*
 * Copyright (C) 2015-2017 52°North Initiative for Geospatial Open Source
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.hibernate.Session;
import org.n52.io.DatasetFactoryException;
import org.n52.io.request.IoParameters;
import org.n52.io.response.dataset.DatasetParameters;
import org.n52.io.response.dataset.StationOutput;
import org.n52.io.response.dataset.TimeseriesMetadataOutput;
import org.n52.io.response.dataset.ValueType;
import org.n52.io.response.dataset.quantity.QuantityReferenceValueOutput;
import org.n52.io.response.dataset.quantity.QuantityValue;
import org.n52.series.db.DataAccessException;
import org.n52.series.db.beans.FeatureEntity;
import org.n52.series.db.beans.OfferingEntity;
import org.n52.series.db.beans.PhenomenonEntity;
import org.n52.series.db.beans.ProcedureEntity;
import org.n52.series.db.beans.QuantityDataEntity;
import org.n52.series.db.beans.QuantityDatasetEntity;
import org.n52.series.db.dao.DatasetDao;
import org.n52.series.db.dao.DbQuery;
import org.n52.series.spi.search.SearchResult;
import org.n52.series.spi.search.TimeseriesSearchResult;
import org.n52.web.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

/**
 * @author <a href="mailto:h.bredel@52north.org">Henning Bredel</a>
 * @deprecated since 2.0.0
 */
@Deprecated
public class TimeseriesRepository extends SessionAwareRepository implements OutputAssembler<TimeseriesMetadataOutput> {

    private static final Logger LOGGER = LoggerFactory.getLogger(TimeseriesRepository.class);

    @Autowired
    @Qualifier(value = "stationRepository")
    private OutputAssembler<StationOutput> stationRepository;

    @Autowired
    private IDataRepositoryFactory factory;

    private DatasetDao<QuantityDatasetEntity> createDao(Session session) {
        return new DatasetDao<>(session, QuantityDatasetEntity.class);
    }

    @Override
    public boolean exists(String id, DbQuery parameters) throws DataAccessException {
        Session session = getSession();
        try {
            DatasetDao<QuantityDatasetEntity> dao = createDao(session);
            return dao.hasInstance(parseId(id), parameters, QuantityDatasetEntity.class);
        } finally {
            returnSession(session);
        }
    }

    @Override
    public Collection<SearchResult> searchFor(IoParameters parameters) {
        Session session = getSession();
        try {
            DatasetDao<QuantityDatasetEntity> seriesDao = createDao(session);
            DbQuery query = dbQueryFactory.createFrom(parameters);
            List<QuantityDatasetEntity> found = seriesDao.find(query);
            return convertToResults(found, query.getLocale());
        } finally {
            returnSession(session);
        }
    }

    private List<SearchResult> convertToResults(List<QuantityDatasetEntity> found, String locale) {
        List<SearchResult> results = new ArrayList<>();
        for (QuantityDatasetEntity searchResult : found) {
            String pkid = searchResult.getPkid()
                                      .toString();
            String phenomenonLabel = searchResult.getPhenomenon()
                                                 .getLabelFrom(locale);
            String procedureLabel = searchResult.getProcedure()
                                                .getLabelFrom(locale);
            String stationLabel = searchResult.getFeature()
                                              .getLabelFrom(locale);
            String offeringLabel = searchResult.getOffering()
                                               .getLabelFrom(locale);
            String label = createTimeseriesLabel(phenomenonLabel, procedureLabel, stationLabel, offeringLabel);
            results.add(new TimeseriesSearchResult(pkid, label));
        }
        return results;
    }

    @Override
    public List<TimeseriesMetadataOutput> getAllCondensed(DbQuery query) throws DataAccessException {
        Session session = getSession();
        try {
            return getAllCondensed(query, session);
        } finally {
            returnSession(session);
        }
    }

    @Override
    public List<TimeseriesMetadataOutput> getAllCondensed(DbQuery query, Session session) throws DataAccessException {
        List<TimeseriesMetadataOutput> results = new ArrayList<>();
        DatasetDao<QuantityDatasetEntity> seriesDao = createDao(session);
        for (QuantityDatasetEntity timeseries : seriesDao.getAllInstances(query)) {
            results.add(createCondensed(timeseries, query, session));
        }
        return results;
    }

    @Override
    public List<TimeseriesMetadataOutput> getAllExpanded(DbQuery query) throws DataAccessException {
        Session session = getSession();
        try {
            return getAllExpanded(query, session);
        } finally {
            returnSession(session);
        }
    }

    @Override
    public List<TimeseriesMetadataOutput> getAllExpanded(DbQuery query, Session session) throws DataAccessException {
        List<TimeseriesMetadataOutput> results = new ArrayList<>();
        DatasetDao<QuantityDatasetEntity> seriesDao = createDao(session);
        for (QuantityDatasetEntity timeseries : seriesDao.getAllInstances(query)) {
            results.add(createExpanded(timeseries, query, session));
        }
        return results;
    }

    @Override
    public TimeseriesMetadataOutput getInstance(String timeseriesId, DbQuery dbQuery) throws DataAccessException {
        Session session = getSession();
        try {
            return getInstance(timeseriesId, dbQuery, session);
        } finally {
            returnSession(session);
        }
    }

    @Override
    public TimeseriesMetadataOutput getInstance(String timeseriesId, DbQuery dbQuery, Session session)
            throws DataAccessException {
        DatasetDao<QuantityDatasetEntity> seriesDao = createDao(session);
        QuantityDatasetEntity result = seriesDao.getInstance(parseId(timeseriesId), dbQuery);
        if (result == null) {
            throw new ResourceNotFoundException("Resource with id '" + timeseriesId + "' could not be found.");
        }
        return createExpanded(result, dbQuery, session);
    }

    protected TimeseriesMetadataOutput createExpanded(QuantityDatasetEntity series, DbQuery query, Session session)
            throws DataAccessException {
        TimeseriesMetadataOutput result = createCondensed(series, query, session);
        IoParameters params = query.getParameters();
        QuantityDataRepository repository = createRepository(ValueType.DEFAULT_VALUE_TYPE);

        List<QuantityReferenceValueOutput> referenceValues = createReferenceValueOutputs(series, query, repository);
        DatasetParameters timeseries = createTimeseriesOutput(series, query.withoutFieldsFilter());
        QuantityValue firstValue = repository.getFirstValue(series, session, query);
        QuantityValue lastValue = repository.getLastValue(series, session, query);

        result.setValue(TimeseriesMetadataOutput.REFERENCE_VALUES, referenceValues, params, result::setReferenceValues);
        result.setValue(TimeseriesMetadataOutput.DATASET_PARAMETERS, timeseries, params, result::setDatasetParameters);
        result.setValue(TimeseriesMetadataOutput.FIRST_VALUE, firstValue, params, result::setFirstValue);
        result.setValue(TimeseriesMetadataOutput.LAST_VALUE, lastValue, params, result::setLastValue);
        return result;
    }

    private QuantityDataRepository createRepository(String valueType) throws DataAccessException {
        if (!ValueType.DEFAULT_VALUE_TYPE.equalsIgnoreCase(valueType)) {
            throw new ResourceNotFoundException("unknown value type: " + valueType);
        }
        try {
            return (QuantityDataRepository) factory.create(ValueType.DEFAULT_VALUE_TYPE);
        } catch (DatasetFactoryException e) {
            throw new DataAccessException(e.getMessage());
        }
    }

    private List<QuantityReferenceValueOutput> createReferenceValueOutputs(QuantityDatasetEntity series,
                                                                           DbQuery query,
                                                                           QuantityDataRepository repository)
            throws DataAccessException {
        List<QuantityReferenceValueOutput> outputs = new ArrayList<>();
        Set<QuantityDatasetEntity> referenceValues = series.getReferenceValues();
        for (QuantityDatasetEntity referenceSeriesEntity : referenceValues) {
            if (referenceSeriesEntity.isPublished()) {
                QuantityReferenceValueOutput refenceValueOutput = new QuantityReferenceValueOutput();
                ProcedureEntity procedure = referenceSeriesEntity.getProcedure();
                refenceValueOutput.setLabel(procedure.getNameI18n(query.getLocale()));
                refenceValueOutput.setReferenceValueId(referenceSeriesEntity.getPkid()
                                                                            .toString());

                QuantityDataEntity lastValue = referenceSeriesEntity.getLastValue();
                refenceValueOutput.setLastValue(repository.createSeriesValueFor(lastValue,
                                                                                referenceSeriesEntity,
                                                                                query));
                outputs.add(refenceValueOutput);
            }
        }
        return outputs;
    }

    private TimeseriesMetadataOutput createCondensed(QuantityDatasetEntity entity, DbQuery query, Session session)
            throws DataAccessException {
        IoParameters parameters = query.getParameters();
        TimeseriesMetadataOutput result = new TimeseriesMetadataOutput(parameters);
        String locale = query.getLocale();
        PhenomenonEntity phenomenon = entity.getPhenomenon();
        String phenomenonLabel = phenomenon.getLabelFrom(locale);
        ProcedureEntity procedure = entity.getProcedure();
        String procedureLabel = procedure.getLabelFrom(locale);
        FeatureEntity feature = entity.getFeature();
        String stationLabel = feature.getLabelFrom(locale);
        OfferingEntity offering = entity.getOffering();
        String offeringLabel = offering.getLabelFrom(locale);

        Long pkid = entity.getPkid();
        String uom = entity.getUnitI18nName(locale);
        String label = createTimeseriesLabel(phenomenonLabel, procedureLabel, stationLabel, offeringLabel);
        StationOutput station = createCondensedStation(entity, query.withoutFieldsFilter(), session);

        result.setId(pkid.toString());
        result.setValue(TimeseriesMetadataOutput.LABEL, label, parameters, result::setLabel);
        result.setValue(TimeseriesMetadataOutput.UOM, uom, parameters, result::setUom);
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

    private StationOutput createCondensedStation(QuantityDatasetEntity entity, DbQuery query, Session session)
            throws DataAccessException {
        FeatureEntity feature = entity.getFeature();
        String featurePkid = Long.toString(feature.getPkid());

        // XXX explicit cast here
        return ((StationRepository) stationRepository).getCondensedInstance(featurePkid, query, session);
    }

    public OutputAssembler<StationOutput> getStationRepository() {
        return stationRepository;
    }

    public void setStationRepository(OutputAssembler<StationOutput> stationRepository) {
        this.stationRepository = stationRepository;
    }

}
