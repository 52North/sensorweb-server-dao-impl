
package org.n52.series.springdata.assembler;

import static org.springframework.data.util.StreamUtils.createStreamFromIterator;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.hibernate.Session;
import org.n52.io.request.IoParameters;
import org.n52.io.response.ParameterOutput;
import org.n52.series.db.DataAccessException;
import org.n52.series.db.beans.DatasetEntity;
import org.n52.series.db.beans.DescribableEntity;
import org.n52.series.db.da.OutputAssembler;
import org.n52.series.db.dao.DbQuery;
import org.n52.series.spi.search.SearchResult;
import org.n52.series.springdata.DatasetRepository;
import org.n52.series.springdata.ParameterDataRepository;
import org.n52.series.springdata.query.DatasetQuerySpecifications;
import org.n52.series.springdata.query.OfferingQuerySpecifications;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.JPQLQuery;

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
    public List<O> getAllCondensed(DbQuery query) throws DataAccessException {
        DatasetQuerySpecifications dsFilterSpec = DatasetQuerySpecifications.of(query);
        JPQLQuery<DatasetEntity> filteredDatasets = dsFilterSpec.filteredDatasets();

        OfferingQuerySpecifications oFilterSpec = OfferingQuerySpecifications.of(query);
        BooleanExpression predicate = oFilterSpec.subSelectFrom(filteredDatasets);

        Iterable<E> findAll = parameterRepository.findAll(predicate);
        Stream<E> foundEntities = createStreamFromIterator(findAll.iterator());
        return foundEntities.map(it -> createCondensed(it, query))
                            .collect(Collectors.toList());
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
            ? parameterRepository.existsByIdentifier(id)
            : parameterRepository.existsById(Long.parseLong(id));
    }

}
