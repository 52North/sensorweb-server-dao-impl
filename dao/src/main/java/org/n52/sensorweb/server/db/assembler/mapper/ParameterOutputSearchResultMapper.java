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
package org.n52.sensorweb.server.db.assembler.mapper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
import org.n52.io.crs.CRSUtils;
import org.n52.io.request.IoParameters;
import org.n52.io.response.AbstractOutput;
import org.n52.io.response.CategoryOutput;
import org.n52.io.response.FeatureOutput;
import org.n52.io.response.OfferingOutput;
import org.n52.io.response.ParameterOutput;
import org.n52.io.response.PhenomenonOutput;
import org.n52.io.response.PlatformOutput;
import org.n52.io.response.ProcedureOutput;
import org.n52.io.response.ServiceOutput;
import org.n52.io.response.TagOutput;
import org.n52.io.response.dataset.AbstractValue;
import org.n52.io.response.dataset.DatasetOutput;
import org.n52.io.response.dataset.DatasetParameters;
import org.n52.io.response.sampling.SamplingOutput;
import org.n52.sensorweb.server.db.TimeOutputCreator;
import org.n52.sensorweb.server.db.old.dao.DbQuery;
import org.n52.series.db.beans.AbstractFeatureEntity;
import org.n52.series.db.beans.CategoryEntity;
import org.n52.series.db.beans.DatasetEntity;
import org.n52.series.db.beans.DescribableEntity;
import org.n52.series.db.beans.GeometryEntity;
import org.n52.series.db.beans.OfferingEntity;
import org.n52.series.db.beans.PhenomenonEntity;
import org.n52.series.db.beans.PlatformEntity;
import org.n52.series.db.beans.ProcedureEntity;
import org.n52.series.db.beans.ServiceEntity;
import org.n52.series.db.beans.TagEntity;
import org.n52.series.db.beans.sampling.SamplingEntity;
import org.n52.series.spi.search.SearchResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class ParameterOutputSearchResultMapper<E extends DescribableEntity, O extends ParameterOutput>
        implements OutputMapper<E, O>, TimeOutputCreator {

    private static final Logger LOGGER = LoggerFactory.getLogger(ParameterOutputSearchResultMapper.class);

    protected final DbQuery query;
    protected final OutputMapperFactory outputMapperFactory;

    public ParameterOutputSearchResultMapper(final DbQuery query, final OutputMapperFactory outputMapperFactory) {
        this.query = query == null ? outputMapperFactory.getDbQuery(IoParameters.createDefaults()) : query;
        this.outputMapperFactory = outputMapperFactory;
    }

    @Override
    public O createCondensed(E entity, O output) {
        final IoParameters parameters = query.getParameters();

        final Long id = entity.getId();
        final String label = createLabel(entity);
        final String domainId = entity.getIdentifier();
        final String hrefBase = query.getHrefBase();

        output.setId(Long.toString(id));
        output.setValue(ParameterOutput.LABEL, label, parameters, output::setLabel);
        output.setValue(ParameterOutput.DOMAIN_ID, domainId, parameters, output::setDomainId);
        output.setValue(ParameterOutput.HREF_BASE, hrefBase, parameters, output::setHrefBase);
        return output;
    }

    protected List<O> createCondensed(Collection<E> entities) {
        long start = System.currentTimeMillis();
        if (entities != null) {
            LOGGER.debug("Condensed entities raw: " + entities.size());
            List<O> result = entities.parallelStream().map(entity -> createCondensed(entity, getParameterOuput()))
                    .collect(Collectors.toList());
            LOGGER.debug("Condensed entities processed: " + result.size());
            LOGGER.debug("Processing all condensed instances takes {} ms", System.currentTimeMillis() - start);
            return result;
        }
        return new ArrayList<>();
    }

    @Override
    public O addExpandedValues(E entity, O output) {
        addService(entity, output);
        return output;
    }

    public <E extends DescribableEntity, R extends SearchResult> R createSearchResult(final E entity, final R result) {
        result.setId(Long.toString(entity.getId()));
        result.setLabel(entity.getLabelFrom(query.getLocale()));
        result.setBaseUrl(query.getHrefBase());
        return result;
    }

    protected <E extends DescribableEntity, O extends ParameterOutput> O addService(E entity, O output) {
        if (output instanceof AbstractOutput) {
            final ServiceOutput serviceOutput = outputMapperFactory.getServiceMapper()
                    .createCondensed(outputMapperFactory.getServiceEntity(entity), new ServiceOutput());
            ((AbstractOutput) output).setValue(AbstractOutput.SERVICE, serviceOutput, query.getParameters(),
                    ((AbstractOutput) output)::setService);
        }
        return output;
    }

    protected <E extends DescribableEntity> String createLabel(E entity) {
        return entity.getLabelFrom(query.getLocale());
    }

    public Geometry createGeometry(AbstractFeatureEntity<?> featureEntity) {
        return featureEntity.isSetGeometry() ? getGeometry(featureEntity.getGeometryEntity()) : null;
    }

    public Geometry getGeometry(GeometryEntity geometryEntity) {
        if (geometryEntity == null) {
            return null;
        } else {
            String srid = query.getDatabaseSridCode();
            geometryEntity.setGeometryFactory(createGeometryFactory(srid));
            return geometryEntity.getGeometry();
        }
    }

    private GeometryFactory createGeometryFactory(String srsId) {
        PrecisionModel pm = new PrecisionModel(PrecisionModel.FLOATING);
        return srsId == null ? new GeometryFactory(pm) : new GeometryFactory(pm, CRSUtils.getSrsIdFrom(srsId));
    }

    protected OutputMapperFactory getOutputMapperFactory() {
        return outputMapperFactory;
    }

    protected DatasetOutput<AbstractValue<?>> getDatasetOutput(DatasetEntity datasetEntity, DbQuery query) {
        return getOutputMapperFactory().getDatasetMapper(query).createCondensed(datasetEntity, new DatasetOutput());
    }

    protected FeatureOutput getFeatureOutput(AbstractFeatureEntity<?> entity, DbQuery query) {
        return getOutputMapperFactory().getFeatureMapper(query).createCondensed(entity, new FeatureOutput());
    }

    protected OfferingOutput getOfferingOutput(OfferingEntity entity, DbQuery query) {
        return getOutputMapperFactory().getOfferingMapper(query).createCondensed(entity, new OfferingOutput());
    }

    protected PhenomenonOutput getPhenomenonOutput(PhenomenonEntity entity, DbQuery query) {
        return getOutputMapperFactory().getPhenomenonMapper(query).createCondensed(entity, new PhenomenonOutput());
    }

    protected CategoryOutput getCategoryOutput(CategoryEntity entity, DbQuery query) {
        return getOutputMapperFactory().getCategoryMapper(query).createCondensed(entity, new CategoryOutput());
    }

    protected ProcedureOutput getProcedureOutput(ProcedureEntity entity, DbQuery query) {
        return getOutputMapperFactory().getProcedureMapper(query).createCondensed(entity, new ProcedureOutput());
    }

    protected PlatformOutput getPlatformOutput(PlatformEntity entity, DbQuery query) {
        return getOutputMapperFactory().getPlatformMapper(query).createCondensed(entity, new PlatformOutput());
    }

    protected ServiceOutput getServiceOutput(ServiceEntity entity, DbQuery query) {
        return getOutputMapperFactory().getServiceMapper(query).createCondensed(entity, new ServiceOutput());
    }

    protected TagOutput getTagOutput(TagEntity entity, DbQuery query) {
        return getOutputMapperFactory().getTagMapper(query).createCondensed(entity, new TagOutput());
    }

    protected SamplingOutput getSamplingOutput(SamplingEntity entity, DbQuery query) {
        return getOutputMapperFactory().getSamplingOutputMapper(query).createCondensed(entity, new SamplingOutput());
    }

    protected DatasetParameters createTimeseriesOutput(DatasetEntity dataset, DbQuery parameters) {
        DatasetParameters metadata = new DatasetParameters();
        metadata.setService(getServiceOutput(getOutputMapperFactory().getServiceEntity(dataset), parameters));
        metadata.setOffering(getOfferingOutput(dataset.getOffering(), parameters));
        metadata.setProcedure(getProcedureOutput(dataset.getProcedure(), parameters));
        metadata.setPhenomenon(getPhenomenonOutput(dataset.getPhenomenon(), parameters));
        metadata.setCategory(getCategoryOutput(dataset.getCategory(), parameters));
        metadata.setPlatform(getPlatformOutput(dataset.getPlatform(), parameters));
        return metadata;
    }

}
