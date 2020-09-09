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
package org.n52.series.db.assembler.core;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.n52.io.response.FeatureOutput;
import org.n52.io.response.HierarchicalParameterOutput;
import org.n52.io.response.dataset.AbstractValue;
import org.n52.io.response.dataset.DatasetOutput;
import org.n52.io.response.dataset.DatasetParameters;
import org.n52.series.db.assembler.ParameterOutputAssembler;
import org.n52.series.db.assembler.mapper.FeatureOutputMapper;
import org.n52.series.db.assembler.mapper.ParameterOutputSearchResultMapper;
import org.n52.series.db.beans.AbstractFeatureEntity;
import org.n52.series.db.beans.DatasetEntity;
import org.n52.series.db.beans.FormatEntity;
import org.n52.series.db.old.dao.DbQuery;
import org.n52.series.db.query.DatasetQuerySpecifications;
import org.n52.series.db.query.FeatureQuerySpecifications;
import org.n52.series.db.repositories.core.DatasetRepository;
import org.n52.series.db.repositories.core.FeatureRepository;
import org.n52.series.spi.search.FeatureSearchResult;
import org.n52.shetland.ogc.OGCConstants;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

@Component
public class FeatureAssembler
        extends ParameterOutputAssembler<AbstractFeatureEntity, FeatureOutput, FeatureSearchResult> {

    @PersistenceContext
    private EntityManager entityManager;

    @Inject
    private FormatAssembler formatAssembler;

    @Inject
    private DatasetAssembler<?> datasetAssembler;

    public FeatureAssembler(FeatureRepository featureRepository, DatasetRepository datasetRepository) {
        super(featureRepository, datasetRepository);
    }

    @Override
    protected FeatureOutput prepareEmptyOutput() {
        return new FeatureOutput();
    }

    @Override
    protected FeatureSearchResult prepareEmptySearchResult() {
        return new FeatureSearchResult();
    }

    @Override
    protected Specification<AbstractFeatureEntity> createFilterPredicate(DbQuery query) {
        DatasetQuerySpecifications dsFilterSpec = DatasetQuerySpecifications.of(query, entityManager);
        FeatureQuerySpecifications fFilterSpec = FeatureQuerySpecifications.of(query, entityManager);
        return fFilterSpec.selectFrom(dsFilterSpec.matchFilters());
    }

    @Override
    public Specification<AbstractFeatureEntity> createPublicPredicate(final String id, DbQuery query) {
        final DatasetQuerySpecifications dsFilterSpec = DatasetQuerySpecifications.of(query, entityManager);
        final Specification<DatasetEntity> datasetPredicate =
                dsFilterSpec.matchFeatures(id).and(dsFilterSpec.isPublic());
        FeatureQuerySpecifications filterSpec = FeatureQuerySpecifications.of(query, entityManager);
        return filterSpec.selectFrom(dsFilterSpec.toSubquery(datasetPredicate));
    }

    @Override
    protected FeatureOutput createExpanded(AbstractFeatureEntity entity, DbQuery query) {
        return createExpanded(entity, query, false, false, query.getLevel());
    }

    protected FeatureOutput createExpanded(AbstractFeatureEntity entity, DbQuery query, boolean isParent,
            boolean isChild, Integer level) {
        FeatureOutput result = super.createExpanded(entity, query);
        if (!isParent && !isChild && entity.hasParents()) {
            List<FeatureOutput> parents = getMemberList(entity.getParents(), query, level, true, false);
            result.setValue(HierarchicalParameterOutput.PARENTS, parents, query.getParameters(), result::setParents);
        }
        if (level != null && level > 0) {
            if (((!isParent && !isChild) || (!isParent && isChild)) && entity.hasChildren()) {
                List<FeatureOutput> children = getMemberList(entity.getChildren(), query, level - 1, false, true);
                result.setValue(HierarchicalParameterOutput.CHILDREN, children, query.getParameters(),
                        result::setChildren);
            }
        }
        return result;
    }

    private List<FeatureOutput> getMemberList(Set<AbstractFeatureEntity> entities, DbQuery query, Integer level,
            boolean isNotParent, boolean isNotChild) {
        List<FeatureOutput> list = new LinkedList<>();
        for (AbstractFeatureEntity e : entities) {
            list.add(createExpanded(e, query, isNotParent, isNotChild, level));
        }
        return list;
    }

    private Map<String, DatasetParameters> createDatasetParameters(List<?> datasets) {
        Map<String, DatasetParameters> map = new LinkedHashMap<>();
        for (Object object : datasets) {
            DatasetOutput<AbstractValue<?>> value = (DatasetOutput<AbstractValue<?>>) object;
            map.put(value.getId(), value.getDatasetParameters());
        }
        return map;
    }

    @Override
    public AbstractFeatureEntity getOrInsertInstance(AbstractFeatureEntity entity) {
        AbstractFeatureEntity instance = getParameterRepository().getInstance(entity);
        if (instance != null) {
            return instance;
        }
        entity.setFeatureType(formatAssembler.getOrInsertInstance(entity.isSetFeatureType() ? entity.getFeatureType()
                : new FormatEntity().setFormat(OGCConstants.UNKNOWN)));
        return getParameterRepository().saveAndFlush(entity);
    }

    @Override
    protected ParameterOutputSearchResultMapper getMapper(DbQuery query) {
        return new FeatureOutputMapper(query);
    }
}
