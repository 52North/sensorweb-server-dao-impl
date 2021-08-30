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
package org.n52.series.db.da.mapper;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.Session;
import org.locationtech.jts.geom.Geometry;
import org.n52.io.request.IoParameters;
import org.n52.io.response.FeatureOutput;
import org.n52.io.response.HierarchicalParameterOutput;
import org.n52.io.response.dataset.DatasetParameters;
import org.n52.io.response.dataset.StationOutput;
import org.n52.series.db.DataAccessException;
import org.n52.series.db.beans.AbstractFeatureEntity;
import org.n52.series.db.beans.DatasetEntity;
import org.n52.series.db.beans.FeatureEntity;
import org.n52.series.db.dao.DbQuery;

public class FeatureMapper extends AbstractOuputMapper<FeatureOutput, FeatureEntity> {

    public FeatureMapper(MapperFactory mapperFactory, IoParameters params) {
        super(mapperFactory, params, false);
    }

    public FeatureMapper(MapperFactory mapperFactory, IoParameters params, boolean subMapper) {
        super(mapperFactory, params, subMapper);
    }

    @Override
    public FeatureOutput createCondensed(FeatureEntity entity, DbQuery query) {
        try {
            FeatureOutput result = createCondensed(new FeatureOutput(), entity, query);
            if (query.getParameters().isSelected(FeatureOutput.GEOMETRY)) {
                result.setValue(StationOutput.GEOMETRY, createGeometry(entity, query), query.getParameters(),
                        result::setGeometry);
            }
            return result;
        } catch (Exception e) {
            log(entity, e);
        }
        return null;
    }

    public FeatureOutput createCondensed(AbstractFeatureEntity<?> entity, DbQuery query) {
        try {
            FeatureOutput result = condensed(new FeatureOutput(), (FeatureEntity) entity, query);
            if (query.getParameters().isSelected(StationOutput.GEOMETRY)) {
                result.setValue(StationOutput.GEOMETRY, createGeometry(entity, query), query.getParameters(),
                        result::setGeometry);
            }
            return result;
        } catch (Exception e) {
            log(entity, e);
        }
        return null;
    }

    @Override
    public FeatureOutput createExpanded(FeatureEntity entity, DbQuery query, Session session) {
        return createExpanded(entity, query, false, false, query.getLevel(), session);
    }

    public FeatureOutput createExpanded(FeatureEntity entity, DbQuery query, boolean isParent, boolean isChild,
            Integer level, Session session) {
        try {
            FeatureOutput result = createCondensed(entity, query);
            addService(result, entity, query);
            if (!isParent && !isChild) {
                Map<String, DatasetParameters> timeseriesList = createTimeseriesList(entity.getDatasets(),
                        query.withSubSelectFilter(StationOutput.PROPERTIES));
                result.setValue(StationOutput.PROPERTIES, timeseriesList, query.getParameters(), result::setDatasets);
            }
            if (!isParent && !isChild && entity.hasParents()) {
                List<FeatureOutput> parents = getMemberList(entity.getParents(),
                        query.withSubSelectFilter(HierarchicalParameterOutput.PARENTS), level, true, false, session);
                result.setValue(HierarchicalParameterOutput.PARENTS, parents, query.getParameters(),
                        result::setParents);
            }
            if (level != null && level > 0) {
                if ((!isParent && !isChild || !isParent && isChild) && entity.hasChildren()) {
                    List<FeatureOutput> children = getMemberList(entity.getChildren(),
                            query.withSubSelectFilter(HierarchicalParameterOutput.CHILDREN), level - 1, false, true,
                            session);
                    result.setValue(HierarchicalParameterOutput.CHILDREN, children, query.getParameters(),
                            result::setChildren);
                }
            }
            return result;
        } catch (Exception e) {
            getLogger().error("Error while processing {} with id {}! Exception: {}", entity.getClass().getSimpleName(),
                    entity.getId(), e);
        }
        return null;
    }

    private List<FeatureOutput> getMemberList(Set<FeatureEntity> entities, DbQuery query, Integer level,
            boolean isNotParent, boolean isNotChild, Session session) {
        List<FeatureOutput> list = new LinkedList<>();
        for (FeatureEntity e : entities) {
            list.add(createExpanded(e, query, isNotParent, isNotChild, level, session));
        }
        return list;
    }

    private Map<String, DatasetParameters> createTimeseriesList(Collection<DatasetEntity> series, DbQuery parameters)
            throws DataAccessException {
        Map<String, DatasetParameters> timeseriesOutputs = new HashMap<>();
        for (DatasetEntity timeseries : series) {
            if (!timeseries.getProcedure().isReference()) {
                String timeseriesId = Long.toString(timeseries.getId());
                timeseriesOutputs.put(timeseriesId, createTimeseriesOutput(timeseries, parameters));
            }
        }
        return timeseriesOutputs;
    }

    protected Geometry createGeometry(AbstractFeatureEntity<?> featureEntity, DbQuery query) {
        return featureEntity.isSetGeometry() ? getGeometry(featureEntity.getGeometryEntity(), query) : null;
    }

}
