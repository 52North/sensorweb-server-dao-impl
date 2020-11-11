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
package org.n52.series.db.da.mapper;

import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.stream.Collectors;

import org.hibernate.Session;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
import org.n52.io.crs.CRSUtils;
import org.n52.io.request.IoParameters;
import org.n52.io.response.AbstractOutput;
import org.n52.io.response.CategoryOutput;
import org.n52.io.response.OfferingOutput;
import org.n52.io.response.ParameterOutput;
import org.n52.io.response.PhenomenonOutput;
import org.n52.io.response.PlatformOutput;
import org.n52.io.response.ProcedureOutput;
import org.n52.io.response.ServiceOutput;
import org.n52.io.response.TimeOutput;
import org.n52.io.response.dataset.DatasetParameters;
import org.n52.series.db.DataAccessException;
import org.n52.series.db.beans.CategoryEntity;
import org.n52.series.db.beans.DatasetEntity;
import org.n52.series.db.beans.DescribableEntity;
import org.n52.series.db.beans.GeometryEntity;
import org.n52.series.db.beans.OfferingEntity;
import org.n52.series.db.beans.PhenomenonEntity;
import org.n52.series.db.beans.PlatformEntity;
import org.n52.series.db.beans.ProcedureEntity;
import org.n52.series.db.beans.ServiceEntity;
import org.n52.series.db.dao.DbQuery;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.operation.TransformException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractOuputMapper<T extends ParameterOutput, S extends DescribableEntity>
        implements OutputMapper<T, S> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractOuputMapper.class);
    private static final String OFFSET_REGEX = "([+-](?:2[0-3]|[01][0-9]):[0-5][0-9])";
    private CRSUtils crsUtils = CRSUtils.createEpsgForcedXYAxisOrder();

    private MapperFactory mapperFactory;

    public AbstractOuputMapper(MapperFactory mapperFactory) {
        this.mapperFactory = mapperFactory;
    }

    @Override
    public Logger getLogger() {
        return LOGGER;
    }

    protected MapperFactory getMapperFactory() {
        return mapperFactory;
    }

    protected T addService(AbstractOutput result, S entity, DbQuery query) {
        ServiceOutput service = (query.getHrefBase() != null)
                ? getCondensedExtendedService(getServiceEntity(entity), query.withoutFieldsFilter())
                : getCondensedService(getServiceEntity(entity), query.withoutFieldsFilter());
        result.setValue(AbstractOutput.SERVICE, service, query.getParameters(), result::setService);
        return (T) result;
    }

    protected DatasetParameters createTimeseriesOutput(DatasetEntity dataset, DbQuery parameters)
            throws DataAccessException {
        DatasetParameters metadata = new DatasetParameters();
        ServiceEntity service = getServiceEntity(dataset);
        metadata.setService(getCondensedService(service, parameters));
        metadata.setOffering(getCondensedOffering(dataset.getOffering(), parameters));
        metadata.setProcedure(getCondensedProcedure(dataset.getProcedure(), parameters));
        metadata.setPhenomenon(getCondensedPhenomenon(dataset.getPhenomenon(), parameters));
        metadata.setCategory(getCondensedCategory(dataset.getCategory(), parameters));
        metadata.setPlatform(getCondensedPlatform(dataset.getPlatform(), parameters));
        return metadata;
    }

    protected ServiceEntity getServiceEntity(DescribableEntity entity) {
        return getMapperFactory().getServiceEntity(entity);
    }

    protected ServiceOutput getCondensedService(ServiceEntity service, DbQuery query) {
        return getMapperFactory().getServiceMapper().createCondensed(service, query);
    }

    protected ServiceOutput getCondensedExtendedService(ServiceEntity serviceEntity, DbQuery query) {
        return getMapperFactory().getServiceMapper().createCondensed(new ServiceOutput(), serviceEntity, query);
    }

    protected ParameterOutput getCondensedOffering(OfferingEntity offering, DbQuery query) {
        return getMapperFactory().getOfferingMapper().createCondensed(new OfferingOutput(), offering, query);
    }

    protected ParameterOutput getCondensedProcedure(ProcedureEntity procedure, DbQuery query) {
        return getMapperFactory().getProcedureMapper().createCondensed(new ProcedureOutput(), procedure, query);
    }

    protected ParameterOutput getCondensedPhenomenon(PhenomenonEntity phenomenon, DbQuery query) {
        return getMapperFactory().getPhenomenonMapper().createCondensed(new PhenomenonOutput(), phenomenon, query);
    }

    protected ParameterOutput getCondensedCategory(CategoryEntity category, DbQuery query) {
        return getMapperFactory().getCategoryMapper().createCondensed(new CategoryOutput(), category, query);
    }

    protected ParameterOutput getCondensedPlatform(PlatformEntity platform, DbQuery query) {
        return getMapperFactory().getPlatformMapper().createCondensed(new PlatformOutput(), platform, query);
    }

    protected Geometry getGeometry(GeometryEntity geometryEntity, DbQuery query) {
        if (geometryEntity == null) {
            return null;
        } else {
            String srid = query.getDatabaseSridCode();
            geometryEntity.setGeometryFactory(createGeometryFactory(srid));
            try {
                return crsUtils.transformOuterToInner(geometryEntity.getGeometry(), srid);
            } catch (FactoryException | TransformException e) {
                throw new DataAccessException("Error while creating geometry!", e);
            }
        }
    }

    private GeometryFactory createGeometryFactory(String srsId) {
        PrecisionModel pm = new PrecisionModel(PrecisionModel.FLOATING);
        return srsId == null ? new GeometryFactory(pm) : new GeometryFactory(pm, CRSUtils.getSrsIdFrom(srsId));
    }

    protected List<T> createCondensed(Collection<S> entities, DbQuery query, Session session) {
        long start = System.currentTimeMillis();
        if (entities != null) {
            LOGGER.debug("Condensed entities raw: " + entities.size());
            List<T> result = entities.parallelStream().map(entity -> createCondensed(entity, query))
                    .collect(Collectors.toList());
            LOGGER.debug("Condensed entities processed: " + result.size());
            LOGGER.debug("Processing all condensed instances takes {} ms", System.currentTimeMillis() - start);
            return result;
        }
        return new ArrayList<>();
    }

    protected TimeOutput createTimeOutput(Date date, IoParameters parameters) {
        if (date != null) {
            return new TimeOutput(new DateTime(date), parameters.formatToUnixTime());
        }
        return null;
    }

    protected TimeOutput createTimeOutput(Date date, String originTimezone, IoParameters parameters) {
        if (date != null) {
            DateTimeZone zone = getOriginTimeZone(originTimezone);
            return new TimeOutput(new DateTime(date).withZone(zone), parameters.formatToUnixTime());
        }
        return null;
    }

    protected DateTimeZone getOriginTimeZone(String originTimezone) {
        if (originTimezone != null && !originTimezone.isEmpty()) {
            if (originTimezone.matches(OFFSET_REGEX)) {
                return DateTimeZone.forTimeZone(TimeZone.getTimeZone(ZoneOffset.of(originTimezone).normalized()));
            } else {
                return DateTimeZone.forID(originTimezone.trim());
            }
        }
        return DateTimeZone.UTC;
    }

}
