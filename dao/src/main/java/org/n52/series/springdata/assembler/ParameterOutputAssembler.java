package org.n52.series.springdata.assembler;

import java.util.Collection;
import java.util.List;

import org.hibernate.Session;
import org.n52.io.request.IoParameters;
import org.n52.series.db.DataAccessException;
import org.n52.series.db.beans.DescribableEntity;
import org.n52.series.db.da.OutputAssembler;
import org.n52.series.db.dao.DbQuery;
import org.n52.series.spi.search.SearchResult;
import org.n52.series.springdata.ParameterDataRepository;

public class ParameterOutputAssembler<E extends DescribableEntity, O> implements OutputAssembler<O> {

    private final ParameterDataRepository<E> repository;

    public ParameterOutputAssembler(ParameterDataRepository<E> repository) {
        this.repository = repository;
    }

    @Override
    public List<O> getAllCondensed(DbQuery parameters) throws DataAccessException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<O> getAllCondensed(DbQuery parameters, Session session) throws DataAccessException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<O> getAllExpanded(DbQuery parameters) throws DataAccessException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<O> getAllExpanded(DbQuery parameters, Session session) throws DataAccessException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public O getInstance(String id, DbQuery parameters) throws DataAccessException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public O getInstance(String id, DbQuery parameters, Session session) throws DataAccessException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection<SearchResult> searchFor(IoParameters parameters) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean exists(String id, DbQuery query) throws DataAccessException {
        return query.isMatchDomainIds()
                ? repository.existsById(Long.parseLong(id))
                : repository.existsByIdentifier(id);
    }

}
