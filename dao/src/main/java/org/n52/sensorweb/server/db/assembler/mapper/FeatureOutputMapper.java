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

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.n52.io.response.FeatureOutput;
import org.n52.io.response.HierarchicalParameterOutput;
import org.n52.io.response.dataset.DatasetParameters;
import org.n52.sensorweb.server.db.old.dao.DbQuery;
import org.n52.series.db.beans.AbstractFeatureEntity;
import org.n52.series.db.beans.DatasetEntity;
import org.n52.series.db.beans.FeatureEntity;

public class FeatureOutputMapper extends ParameterOutputSearchResultMapper<AbstractFeatureEntity, FeatureOutput> {

    public FeatureOutputMapper(DbQuery query, OutputMapperFactory outputMapperFactory) {
        this(query, outputMapperFactory, false);
    }

    public FeatureOutputMapper(DbQuery query, OutputMapperFactory outputMapperFactory, boolean subMapper) {
        super(query, outputMapperFactory, subMapper);
    }

    @Override
    public FeatureOutput createCondensed(AbstractFeatureEntity entity, FeatureOutput output) {
        FeatureOutput result = super.createCondensed(entity, output);
        if (getDbQuery().getParameters().isSelected(FeatureOutput.GEOMETRY)) {
            result.setValue(FeatureOutput.GEOMETRY, createGeometry(entity), getDbQuery().getParameters(),
                    result::setGeometry);
        }
        return result;
    }

    @Override
    public FeatureOutput addExpandedValues(AbstractFeatureEntity entity, FeatureOutput output) {
        return addExpandedValues(entity, output, false, false, getDbQuery().getLevel());
    }

    protected FeatureOutput addExpandedValues(AbstractFeatureEntity entity, FeatureOutput output, boolean isParent,
            boolean isChild, Integer level) {
        if (!isParent && !isChild && getDbQuery().getParameters().isSelected(FeatureOutput.PROPERTIES)) {
            Map<String, DatasetParameters> timeseriesList = createTimeseriesList(entity.getDatasets());
            output.setValue(FeatureOutput.PROPERTIES, timeseriesList, getDbQuery().getParameters(),
                    output::setDatasets);
        }
        if (entity instanceof FeatureEntity) {
            if (!isParent && !isChild && entity.hasParents()
                    && getDbQuery().getParameters().isSelected(HierarchicalParameterOutput.PARENTS)) {
                List<FeatureOutput> parents = getMemberList(entity.getParents(), level, true, false);
                output.setValue(HierarchicalParameterOutput.PARENTS, parents, getDbQuery().getParameters(),
                        output::setParents);
            }
            if (level != null && level > 0) {
                if (((!isParent && !isChild) || (!isParent && isChild)) && entity.hasChildren()
                        && getDbQuery().getParameters().isSelected(HierarchicalParameterOutput.CHILDREN)) {
                    List<FeatureOutput> children = getMemberList(entity.getChildren(), level - 1, false, true);
                    output.setValue(HierarchicalParameterOutput.CHILDREN, children, getDbQuery().getParameters(),
                            output::setChildren);
                }
            }
        }
        return output;
    }

    protected List<FeatureOutput> getMemberList(Set<AbstractFeatureEntity> entities, Integer level,
            boolean isNotParent, boolean isNotChild) {
        List<FeatureOutput> list = new LinkedList<>();
        for (AbstractFeatureEntity e : entities) {
            list.add(createExpanded(e, getParameterOuput(), isNotParent, isNotChild, level));
        }
        return list;
    }

    private FeatureOutput createExpanded(AbstractFeatureEntity entity, FeatureOutput output, boolean isParent,
            boolean isChild, Integer level) {
        createCondensed(entity, output);
        super.addExpandedValues(entity, output);
        addExpandedValues(entity, output, isParent, isChild, level);
        return output;
    }

    private Map<String, DatasetParameters> createTimeseriesList(Collection<DatasetEntity> series) {
        Map<String, DatasetParameters> timeseriesOutputs = new HashMap<>();
        for (DatasetEntity timeseries : series) {
            if (!timeseries.getProcedure().isReference()) {
                String timeseriesId = Long.toString(timeseries.getId());
                timeseriesOutputs.put(timeseriesId, createTimeseriesOutput(timeseries, getDbQuery()));
            }
        }
        return timeseriesOutputs;
    }

    @Override
    public FeatureOutput getParameterOuput() {
        return new FeatureOutput();
    }
}
