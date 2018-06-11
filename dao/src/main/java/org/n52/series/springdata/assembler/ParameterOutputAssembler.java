
package org.n52.series.springdata.assembler;

import java.util.Collection;
import java.util.List;

import org.hibernate.Session;
import org.n52.io.request.IoParameters;
import org.n52.io.response.ParameterOutput;
import org.n52.series.db.DataAccessException;
import org.n52.series.db.beans.DescribableEntity;
import org.n52.series.db.da.OutputAssembler;
import org.n52.series.db.dao.DbQuery;
import org.n52.series.spi.search.SearchResult;
import org.n52.series.springdata.DatasetRepository;
import org.n52.series.springdata.ParameterDataRepository;

public abstract class ParameterOutputAssembler<E extends DescribableEntity, O extends ParameterOutput> implements
        OutputAssembler<O> {

    private final ParameterDataRepository<E> parameterRepository;

    private final DatasetRepository< ? > datasetRepository;

    public ParameterOutputAssembler(ParameterDataRepository<E> parameterRepository,
                                    DatasetRepository< ? > datasetRepository) {
        this.parameterRepository = parameterRepository;
        this.datasetRepository = datasetRepository;
    }

    protected abstract O prepareEmptyOutput();

    protected O createCondensed(E entity, DbQuery query) {
        O result = prepareEmptyOutput();
        IoParameters parameters = query.getParameters();

        Long id = entity.getId();
        String label = entity.getLabelFrom(query.getLocale());
        String domainId = entity.getIdentifier();
        String hrefBase = query.getHrefBase();

        result.setId(Long.toString(id));
        result.setValue(ParameterOutput.LABEL, label, parameters, result::setLabel);
        result.setValue(ParameterOutput.DOMAIN_ID, domainId, parameters, result::setDomainId);
        // result.setValue(ParameterOutput.HREF_BASE, createHref(hrefBase), parameters, result::setHrefBase);
        return result;
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
            ? parameterRepository.existsById(Long.parseLong(id))
            : parameterRepository.existsByIdentifier(id);
    }

}
