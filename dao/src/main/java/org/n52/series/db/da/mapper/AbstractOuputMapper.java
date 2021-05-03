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
package org.n52.series.db.da.mapper;

import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
import org.n52.io.request.Parameters;
import org.n52.io.response.AbstractOutput;
import org.n52.io.response.CategoryOutput;
import org.n52.io.response.FeatureOutput;
import org.n52.io.response.OfferingOutput;
import org.n52.io.response.ParameterOutput;
import org.n52.io.response.PhenomenonOutput;
import org.n52.io.response.PlatformOutput;
import org.n52.io.response.ProcedureOutput;
import org.n52.io.response.ServiceOutput;
import org.n52.io.response.TimeOutput;
import org.n52.io.response.dataset.DatasetParameters;
import org.n52.series.db.DataAccessException;
import org.n52.series.db.beans.AbstractFeatureEntity;
import org.n52.series.db.beans.CategoryEntity;
import org.n52.series.db.beans.DatasetEntity;
import org.n52.series.db.beans.DescribableEntity;
import org.n52.series.db.beans.FeatureEntity;
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

    protected ServiceMapper serviceMapper;
    protected FeatureMapper featureMapper;
    protected OfferingMapper offeringMapper;
    protected ProcedureMapper procedureMapper;
    protected PhenomenonMapper phenomenonMapper;
    protected CategoryMapper categoryMapper;
    protected PlatformMapper platformMapper;

    private CRSUtils crsUtils = CRSUtils.createEpsgForcedXYAxisOrder();
    private MapperFactory mapperFactory;
    private boolean hasSelecetion;
    private Set<String> selection = new LinkedHashSet<>();
    private Map<String, Set<String>> subSelection = new LinkedHashMap<>();
    private GeometryFactory geometryFactory;

    private String hrefBase;

    public AbstractOuputMapper(MapperFactory mapperFactory, IoParameters params) {
        this(mapperFactory, params, false);
    }

    public AbstractOuputMapper(MapperFactory mapperFactory, IoParameters params, boolean subMapper) {
        this.mapperFactory = mapperFactory;
        this.hrefBase = params.getHrefBase();
        if (!subMapper) {
            if (params.containsParameter(Parameters.SELECT)) {
                this.selection.addAll(params.getSelectOriginal());
                this.hasSelecetion = !selection.isEmpty();
            }
        }
    }

    protected void initSubMapper(IoParameters params) {
        this.serviceMapper = getMapperFactory().getServiceMapper(params.withSubSelectFilter("service"), true);
        this.featureMapper = getMapperFactory().getFeatureMapper(params.withSubSelectFilter("feature"), true);
        this.offeringMapper = getMapperFactory().getOfferingMapper(params.withSubSelectFilter("offering"), true);
        this.procedureMapper = getMapperFactory().getProcedureMapper(params.withSubSelectFilter("procedure"), true);
        this.phenomenonMapper = getMapperFactory().getPhenomenonMapper(params.withSubSelectFilter("phenomenon"), true);
        this.categoryMapper = getMapperFactory().getCategoryMapper(params.withSubSelectFilter("category"), true);
        this.platformMapper = getMapperFactory().getPlatformMapper(params.withSubSelectFilter("platform"), true);
    }

    protected void initSubSelect(IoParameters params, String... subs) {
        if (hasSelecetion) {
            for (String sub : subs) {
                if (isSelected(sub)) {
                    subSelection.put(sub, params.withSubSelectFilter(sub).getSelectOriginal());
                }
            }
        }
    }

    @Override
    public Logger getLogger() {
        return LOGGER;
    }

    @Override
    public String getHrefBase() {
        return hrefBase;
    }

    @Override
    public boolean hasSelect() {
        return hasSelecetion;
    }

    @Override
    public Set<String> getSelection() {
        return selection;
    }

    @Override
    public Map<String, Set<String>> getSubSelection() {
        return subSelection;
    }

    protected MapperFactory getMapperFactory() {
        return mapperFactory;
    }

    protected T addService(AbstractOutput result, S entity, DbQuery query) {
        if (query.getParameters().isSelected(AbstractOutput.SERVICE)) {
            ServiceOutput service = getCondensedService(getServiceEntity(entity), query.withoutFieldsFilter());
            result.setValue(AbstractOutput.SERVICE, service, query.getParameters(), result::setService);
        }
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
        return serviceMapper.createCondensed(service, query);
    }

    protected FeatureOutput getCondensedFeature(AbstractFeatureEntity<?> feature, DbQuery query) {
        return featureMapper.createCondensed((FeatureEntity) feature, query);
    }

    protected OfferingOutput getCondensedOffering(OfferingEntity offering, DbQuery query) {
        return offeringMapper.createCondensed(offering, query);
    }

    protected ProcedureOutput getCondensedProcedure(ProcedureEntity procedure, DbQuery query) {
        return procedureMapper.createCondensed(procedure, query);
    }

    protected PhenomenonOutput getCondensedPhenomenon(PhenomenonEntity phenomenon, DbQuery query) {
        return phenomenonMapper.createCondensed(phenomenon, query);
    }

    protected CategoryOutput getCondensedCategory(CategoryEntity category, DbQuery query) {
        return categoryMapper.createCondensed(category, query);
    }

    protected PlatformOutput getCondensedPlatform(PlatformEntity platform, DbQuery query) {
        return platformMapper.createCondensed(platform, query);
    }

    protected <V extends ParameterOutput> V createCondensedMinimal(V result, DescribableEntity entity, DbQuery query) {
        result.setId(Long.toString(entity.getId()));
        result.setValue(ParameterOutput.DOMAIN_ID, entity.getIdentifier(), query.getParameters(), result::setDomainId);
        result.setValue(ParameterOutput.LABEL, entity.getLabelFrom(query.getLocaleForLabel()), query.getParameters(),
                result::setLabel);
        result.setValue(ParameterOutput.HREF_BASE, query.getHrefBase(), query.getParameters(), result::setHrefBase);
        return result;
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
        if (geometryFactory == null) {
            PrecisionModel pm = new PrecisionModel(PrecisionModel.FLOATING);
            this.geometryFactory =
                    srsId == null ? new GeometryFactory(pm) : new GeometryFactory(pm, CRSUtils.getSrsIdFrom(srsId));
        }
        return geometryFactory;
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
