
package org.n52.series.db.da;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.hibernate.Session;
import org.n52.io.request.IoParameters;
import org.n52.io.response.ParameterOutput;
import org.n52.series.db.DataAccessException;
import org.n52.series.db.beans.DescribableEntity;
import org.n52.series.db.dao.AbstractDao;
import org.n52.series.db.dao.DbQuery;
import org.n52.series.db.dao.SearchableDao;
import org.n52.series.spi.search.SearchResult;
import org.n52.web.exception.ResourceNotFoundException;

public abstract class ParameterRepository<E extends DescribableEntity, O extends ParameterOutput>
        extends SessionAwareRepository
        implements SearchableRepository, OutputAssembler<O> {

    protected abstract O prepareEmptyParameterOutput(E entity);

    protected abstract SearchResult createEmptySearchResult(String id, String label, String baseUrl);

    protected abstract O createExpanded(E instance, DbQuery query, Session session)
            throws DataAccessException;

    protected abstract String createHref(String hrefBase);

    protected abstract AbstractDao<E> createDao(Session session);

    protected abstract SearchableDao<E> createSearchableDao(Session session);

    @Override
    public boolean exists(String id, DbQuery query) throws DataAccessException {
        Session session = getSession();
        try {
            AbstractDao< ? extends DescribableEntity> dao = createDao(session);
            return dao.hasInstance(parseId(id), query);
        } finally {
            returnSession(session);
        }
    }

    @Override
    public List<O> getAllCondensed(DbQuery query) throws DataAccessException {
        Session session = getSession();
        try {
            return getAllCondensed(query, session);
        } finally {
            returnSession(session);
        }
    }

    @Override
    public List<O> getAllCondensed(DbQuery query, Session session) throws DataAccessException {
        List<E> allInstances = getAllInstances(query, session);
        List<O> results = createCondensed(allInstances, query, session);
        return results;
    }

    protected List<O> createCondensed(Iterable<E> allInstances, DbQuery query, Session session) {
        List<O> results = new ArrayList<>();
        for (E entity : allInstances) {
            results.add(createCondensed(entity, query, session));
        }
        return results;
    }

    protected O createCondensed(E entity, DbQuery query, Session session) {
        O result = prepareEmptyParameterOutput(entity);
        result.setId(Long.toString(entity.getPkid()));
        result.setLabel(entity.getLabelFrom(query.getLocale()));
        result.setDomainId(entity.getDomainId());
        if (query.getHrefBase() != null) {
            result.setHrefBase(createHref(query.getHrefBase()));
        }
        return result;
    }

    @Override
    public List<O> getAllExpanded(DbQuery query) throws DataAccessException {
        Session session = getSession();
        try {
            return getAllExpanded(query, session);
        } finally {
            returnSession(session);
        }
    }

    @Override
    public List<O> getAllExpanded(DbQuery query, Session session) throws DataAccessException {
        List<E> allInstances = getAllInstances(query, session);
        return createExpanded(allInstances, query, session);
    }

    protected List<O> createExpanded(Iterable<E> allInstances, DbQuery query, Session session)
            throws DataAccessException {
        List<O> results = new ArrayList<>();
        for (E entity : allInstances) {
            results.add(createExpanded(entity, query, session));
        }
        return results;
    }

    protected List<E> getAllInstances(DbQuery parameters, Session session) throws DataAccessException {
        return createDao(session).getAllInstances(parameters);
    }

    @Override
    public O getInstance(String id, DbQuery query) throws DataAccessException {
        Session session = getSession();
        try {
            return getInstance(id, query, session);
        } finally {
            returnSession(session);
        }
    }

    @Override
    public O getInstance(String id, DbQuery query, Session session) throws DataAccessException {
        AbstractDao<E> dao = createDao(session);
        E entity = getEntity(parseId(id), dao, query);
        return createExpanded(entity, query, session);
    }

    protected E getInstance(Long id, DbQuery query, Session session) throws DataAccessException {
        AbstractDao<E> dao = createDao(session);
        return getEntity(id, dao, query);
    }

    protected E getEntity(Long id, AbstractDao<E> dao, DbQuery query) throws DataAccessException {
        E entity = dao.getInstance(id, query);
        if (entity == null) {
            throw new ResourceNotFoundException("Resource with id '" + id + "' could not be found.");
        }
        return entity;
    }

    @Override
    public Collection<SearchResult> searchFor(IoParameters parameters) {
        Session session = getSession();
        try {
            SearchableDao<E> dao = createSearchableDao(session);
            DbQuery query = getDbQuery(parameters);
            List<E> found = dao.find(query);
            return convertToSearchResults(found, query);
        } finally {
            returnSession(session);
        }
    }

    protected List<SearchResult> convertToSearchResults(List<E> found, DbQuery query) {
        String locale = query.getLocale();
        String hrefBase = createHref(query.getHrefBase());
        List<SearchResult> results = new ArrayList<>();
        for (DescribableEntity searchResult : found) {
            String label = searchResult.getLabelFrom(locale);
            String pkid = Long.toString(searchResult.getPkid());
            results.add(createEmptySearchResult(pkid, label, hrefBase));
        }
        return results;
    }

}
