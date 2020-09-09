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
package org.n52.series.db.assembler.mapper;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
import org.n52.io.crs.CRSUtils;
import org.n52.io.request.IoParameters;
import org.n52.io.response.ParameterOutput;
import org.n52.series.db.beans.AbstractFeatureEntity;
import org.n52.series.db.beans.DescribableEntity;
import org.n52.series.db.beans.GeometryEntity;
import org.n52.series.db.old.dao.DbQuery;
import org.n52.series.db.old.dao.DefaultDbQueryFactory;
import org.n52.series.spi.search.SearchResult;

public class ParameterOutputSearchResultMapper implements OutputMapper {

    protected final DbQuery query;

    public ParameterOutputSearchResultMapper(final DbQuery query) {
        this.query = query == null ? new DefaultDbQueryFactory().createFrom(IoParameters.createDefaults()) : query;
    }

    @Override
    public <E extends DescribableEntity, O extends ParameterOutput> O createCondensed(E entity, O output) {
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

    public <E extends DescribableEntity, R extends SearchResult> R createSearchResult(final E entity, final R result) {
        result.setId(Long.toString(entity.getId()));
        result.setLabel(entity.getLabelFrom(query.getLocale()));
        result.setBaseUrl(query.getHrefBase());
        return result;
    }

    protected <E extends DescribableEntity> String createLabel(E entity) {
        return entity.getLabelFrom(query.getLocale());
    }

    public Geometry createGeometry(AbstractFeatureEntity<?> featureEntity, DbQuery query) {
        return featureEntity.isSetGeometry() ? getGeometry(featureEntity.getGeometryEntity(), query) : null;
    }

    public Geometry getGeometry(GeometryEntity geometryEntity, DbQuery query) {
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

}
