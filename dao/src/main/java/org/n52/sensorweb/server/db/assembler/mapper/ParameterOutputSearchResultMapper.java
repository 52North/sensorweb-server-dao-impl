/*
 * Copyright (C) 2015-2021 52°North Spatial Information Research GmbH
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
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

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
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.operation.TransformException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class ParameterOutputSearchResultMapper<E extends DescribableEntity, O extends ParameterOutput>
        implements OutputMapper<E, O>, TimeOutputCreator {

    private static final Logger LOGGER = LoggerFactory.getLogger(ParameterOutputSearchResultMapper.class);
    private static final String EPSG_PREFIX = "EPSG:";

    protected ServiceOutputMapper serviceMapper;
    protected FeatureOutputMapper featureMapper;
    protected OfferingOutputMapper offeringMapper;
    protected ProcedureOutputMapper procedureMapper;
    protected PhenomenonOutputMapper phenomenonMapper;
    protected CategoryOutputMapper categoryMapper;
    protected PlatformOutputMapper platformMapper;

    private final DbQuery query;
    private final OutputMapperFactory outputMapperFactory;
    private final CRSUtils crsUtils = CRSUtils.createEpsgForcedXYAxisOrder();
    private boolean hasSelecetion;
    private Set<String> selection = new LinkedHashSet<>();
    private Map<String, Set<String>> subSelection = new LinkedHashMap<>();
    private GeometryFactory geometryFactory;
    private String hrefBase;

    public ParameterOutputSearchResultMapper(DbQuery query, OutputMapperFactory outputMapperFactory,
            boolean subMapper) {
        Objects.requireNonNull(outputMapperFactory);
        this.query = query == null ? outputMapperFactory.getDbQuery(IoParameters.createDefaults()) : query;
        this.outputMapperFactory = outputMapperFactory;
        this.hrefBase = getDbQuery() != null && getDbQuery().getParameters() != null
                ? getDbQuery().getParameters().getHrefBase()
                : "";
        if (!subMapper) {
            if (getDbQuery().getParameters().containsParameter(Parameters.SELECT)) {
                this.selection.addAll(getDbQuery().getParameters().getSelectOriginal());
                this.hasSelecetion = !selection.isEmpty();
            }
            initSubMapper(getDbQuery());
        }
    }

    protected void initSubMapper(DbQuery query) {
        this.serviceMapper = outputMapperFactory.getServiceMapper(query.withSubSelectFilter("service"), true);
        this.featureMapper = outputMapperFactory.getFeatureMapper(query.withSubSelectFilter("feature"), true);
        this.offeringMapper = outputMapperFactory.getOfferingMapper(query.withSubSelectFilter("offering"), true);
        this.procedureMapper = outputMapperFactory.getProcedureMapper(query.withSubSelectFilter("procedure"), true);
        this.phenomenonMapper = outputMapperFactory.getPhenomenonMapper(query.withSubSelectFilter("phenomenon"), true);
        this.categoryMapper = outputMapperFactory.getCategoryMapper(query.withSubSelectFilter("category"), true);
        this.platformMapper = outputMapperFactory.getPlatformMapper(query.withSubSelectFilter("platform"), true);
    }

    protected void initSubSelect(DbQuery query, String... subs) {
        if (hasSelecetion) {
            for (String sub : subs) {
                if (isSelected(sub)) {
                    subSelection.put(sub, query.withSubSelectFilter(sub).getSelectOriginal());
                }
            }
        }
    }

    @Override
    public O createCondensed(E entity, O output) {
        IoParameters parameters = getDbQuery().getParameters();
        String id = Long.toString(entity.getId());
        output.setId(id);
        if (!hasSelect()) {
            addAll(output, entity, getDbQuery(), parameters);
        } else {
            addSelected(output, entity, getDbQuery(), parameters);
        }
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

    public <R extends SearchResult> R createSearchResult(final E entity, final R result) {
        result.setId(Long.toString(entity.getId()));
        result.setLabel(entity.getLabelFrom(getDbQuery().getLocaleForLabel()));
        result.setBaseUrl(getDbQuery().getHrefBase());
        return result;
    }

    protected O addService(E entity, O output) {
        if (output instanceof AbstractOutput && getDbQuery().getParameters().isSelected(AbstractOutput.SERVICE)) {
            ServiceOutput serviceOutput = outputMapperFactory.getServiceMapper(getDbQuery())
                    .createCondensed(outputMapperFactory.getServiceEntity(entity), new ServiceOutput());
            ((AbstractOutput) output).setValue(AbstractOutput.SERVICE, serviceOutput, getDbQuery().getParameters(),
                    ((AbstractOutput) output)::setService);
        }
        return output;
    }

    public Geometry createGeometry(AbstractFeatureEntity<?> featureEntity) {
        return featureEntity.isSetGeometry() ? getGeometry(featureEntity.getGeometryEntity()) : null;
    }

    public Geometry getGeometry(GeometryEntity geometryEntity) {
        if (geometryEntity != null) {
            String srid = getDbQuery().getDatabaseSridCode();
            if (geometryEntity.isSetGeometry() && geometryEntity.getGeometry().getSRID() > 0) {
                srid = EPSG_PREFIX.concat(Integer.toString(geometryEntity.getGeometry().getSRID()));
            }
            geometryEntity.setGeometryFactory(createGeometryFactory(srid));
            try {
                return crsUtils.transformOuterToInner(geometryEntity.getGeometry(), srid);
            } catch (FactoryException | TransformException e) {
                // TODO
            }
        }
        return null;
    }

    private GeometryFactory createGeometryFactory(String srsId) {
        if (geometryFactory == null) {
            PrecisionModel pm = new PrecisionModel(PrecisionModel.FLOATING);
            this.geometryFactory =
                    srsId == null ? new GeometryFactory(pm) : new GeometryFactory(pm, CRSUtils.getSrsIdFrom(srsId));
        }
        return geometryFactory;
    }

    protected OutputMapperFactory getOutputMapperFactory() {
        return outputMapperFactory;
    }

    protected DbQuery getDbQuery() {
        return query;
    }

    protected <V extends ParameterOutput> V createCondensedMinimal(V result, DescribableEntity entity, DbQuery query) {
        result.setId(Long.toString(entity.getId()));
        result.setValue(ParameterOutput.DOMAIN_ID, entity.getIdentifier(), query.getParameters(), result::setDomainId);
        result.setValue(ParameterOutput.LABEL, entity.getLabelFrom(query.getLocaleForLabel()), query.getParameters(),
                result::setLabel);
        result.setValue(ParameterOutput.HREF_BASE, query.getHrefBase(), query.getParameters(), result::setHrefBase);
        return result;
    }

    protected DatasetOutput<AbstractValue<?>> getDatasetOutput(DatasetEntity datasetEntity, DbQuery query) {
        return createCondensedMinimal(new DatasetOutput<>(), datasetEntity, query);
    }

    protected FeatureOutput getFeatureOutput(AbstractFeatureEntity<?> entity) {
        return featureMapper.createCondensed(entity);
    }

    protected OfferingOutput getOfferingOutput(OfferingEntity entity) {
        return offeringMapper.createCondensed(entity);
    }

    protected PhenomenonOutput getPhenomenonOutput(PhenomenonEntity entity) {
        return phenomenonMapper.createCondensed(entity);
    }

    protected CategoryOutput getCategoryOutput(CategoryEntity entity) {
        return categoryMapper.createCondensed(entity);
    }

    protected ProcedureOutput getProcedureOutput(ProcedureEntity entity) {
        return procedureMapper.createCondensed(entity);
    }

    protected PlatformOutput getPlatformOutput(PlatformEntity entity) {
        return platformMapper.createCondensed(entity);
    }

    protected ServiceOutput getServiceOutput(ServiceEntity entity) {
        return serviceMapper.createCondensed(entity);
    }

    protected TagOutput getTagOutput(TagEntity entity, DbQuery query) {
        return getOutputMapperFactory().getTagMapper(query).createCondensed(entity, new TagOutput());
    }

    protected SamplingOutput getSamplingOutput(SamplingEntity entity, DbQuery query) {
        return getOutputMapperFactory().getSamplingMapper(query).createCondensed(entity, new SamplingOutput());
    }

    protected DatasetParameters createTimeseriesOutput(DatasetEntity dataset, DbQuery parameters) {
        DatasetParameters metadata = new DatasetParameters();
        metadata.setService(getServiceOutput(getOutputMapperFactory().getServiceEntity(dataset)));
        metadata.setOffering(getOfferingOutput(dataset.getOffering()));
        metadata.setProcedure(getProcedureOutput(dataset.getProcedure()));
        metadata.setPhenomenon(getPhenomenonOutput(dataset.getPhenomenon()));
        metadata.setCategory(getCategoryOutput(dataset.getCategory()));
        metadata.setPlatform(getPlatformOutput(dataset.getPlatform()));
        return metadata;
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

}
