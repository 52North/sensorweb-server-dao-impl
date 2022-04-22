/*
 * Copyright (C) 2015-2022 52°North Spatial Information Research GmbH
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

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.hibernate.Session;
import org.n52.io.request.IoParameters;
import org.n52.io.response.ServiceOutput;
import org.n52.series.db.DataAccessException;
import org.n52.series.db.beans.ServiceEntity;
import org.n52.series.db.dao.AbstractDao;
import org.n52.series.db.dao.DbQuery;
import org.n52.series.db.dao.SearchableDao;
import org.n52.series.db.dao.ServiceDao;
import org.n52.series.spi.search.SearchResult;
import org.n52.series.spi.search.ServiceSearchResult;
import org.n52.web.exception.ResourceNotFoundException;

public class ServiceRepository extends ParameterRepository<ServiceEntity, ServiceOutput> {

    @Override
    protected ServiceOutput prepareEmptyParameterOutput() {
        return new ServiceOutput();
    }

    @Override
    protected SearchResult createEmptySearchResult(String id, String label, String baseUrl) {
        return new ServiceSearchResult().setId(id).setLabel(label).setBaseUrl(baseUrl);
    }

    @Override
    protected ServiceDao createDao(Session session) {
        return new ServiceDao(session);
    }

    @Override
    protected SearchableDao<ServiceEntity> createSearchableDao(Session session) {
        return new ServiceDao(session);
    }

    @Override
    public boolean exists(String id, DbQuery parameters) throws DataAccessException {
        Session session = getSession();
        try {
            Long rawId = parseId(id);
            ServiceDao dao = createDao(session);
            return isConfiguredServiceInstance(rawId) || dao.hasInstance(rawId, parameters);
        } finally {
            returnSession(session);
        }
    }

    private boolean isConfiguredServiceInstance(Long id) {
        return getServiceEntity() != null && getServiceEntity().getId().equals(id);
    }

    @Override
    public Collection<SearchResult> searchFor(IoParameters parameters) {
        /*
         * final ServiceSearchResult result = new ServiceSearchResult(serviceInfo.getServiceId(),
         * serviceInfo.getServiceDescription()); String queryString =
         * DbQuery.createFrom(parameters).getSearchTerm(); return
         * serviceInfo.getServiceDescription().contains(queryString) ?
         * Collections.<SearchResult>singletonList(result)ServiceRepository :
         * Collections.<SearchResult>emptyList(); Session session = getSession(); try { ServiceDao serviceDao
         * = createDao(session); DbQuery query = getDbQuery(parameters); List<ServiceEntity> found =
         * serviceDao.find(query); return convertToSearchResults(found, query); } finally {
         * returnSession(session); }
         */
        // TODO implement search
        throw new UnsupportedOperationException("not supported");
    }

    @Override
    protected List<ServiceEntity> getAllInstances(DbQuery parameters, Session session) throws DataAccessException {
        return getServiceEntity() != null ? Collections.singletonList(getServiceEntity())
                : createDao(session).getAllInstances(parameters);
    }

    @Override
    protected ServiceEntity getEntity(Long id, AbstractDao<ServiceEntity> dao, DbQuery query)
            throws DataAccessException {
        ServiceEntity result = !isConfiguredServiceInstance(id) ? dao.getInstance(id, query) : getServiceEntity();
        if (result == null) {
            throw new ResourceNotFoundException("Resource with id '" + id + "' could not be found.");
        }
        return result;
    }

    @Override
    protected ServiceOutput createCondensed(ServiceEntity entity, DbQuery query, Session session) {
        return getMapperFactory().getServiceMapper(query.getParameters()).createCondensed(entity, query);
    }

    @Override
    protected ServiceOutput createExpanded(ServiceEntity entity, DbQuery query, Session session) {
        return getMapperFactory().getServiceMapper(query.getParameters()).createExpanded(entity, query, session);
    }


}
