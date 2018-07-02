
package org.n52.series.springdata.assembler;

import static org.springframework.data.util.StreamUtils.createStreamFromIterator;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.n52.io.response.AbstractOutput;
import org.n52.io.response.ServiceOutput;
import org.n52.series.db.beans.DatasetEntity;
import org.n52.series.db.beans.DescribableEntity;
import org.n52.series.db.beans.ServiceEntity;
import org.n52.series.db.dao.DbQuery;
import org.n52.series.spi.search.SearchResult;
import org.n52.series.springdata.DatasetRepository;
import org.n52.series.springdata.ParameterDataRepository;
import org.n52.series.springdata.query.DatasetQuerySpecifications;
import org.n52.series.springdata.query.OfferingQuerySpecifications;
import org.n52.series.srv.OutputAssembler;
import org.springframework.beans.factory.annotation.Autowired;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.JPQLQuery;

public abstract class ParameterOutputAssembler<E extends DescribableEntity, O extends AbstractOutput> implements
        OutputAssembler<O> {

    private final ParameterDataRepository<E> parameterRepository;

    private final DatasetRepository< ? > datasetRepository;

    // via database or config
    @Autowired(required = false)
    private ServiceEntity serviceEntity;

    public ParameterOutputAssembler(ParameterDataRepository<E> parameterRepository,
                                    DatasetRepository< ? > datasetRepository) {
        this.parameterRepository = parameterRepository;
        this.datasetRepository = datasetRepository;
    }

    protected abstract O prepareEmptyOutput();

    protected O createExpanded(E entity, DbQuery query) {
        ServiceEntity serviceEntity = getServiceEntity(entity);
        O output = ParameterOutputMapper.createCondensed(entity, prepareEmptyOutput(), query);
        ServiceOutput serviceOutput = ParameterOutputMapper.createCondensed(serviceEntity, new ServiceOutput(), query);
        output.setValue(AbstractOutput.SERVICE, serviceOutput, query.getParameters(), output::setService);
        return output;
    }

    @Override
    public List<O> getAllCondensed(DbQuery query) {
        return findAll(query).map(it -> ParameterOutputMapper.createCondensed(it, prepareEmptyOutput(), query))
                             .collect(Collectors.toList());

    }

    @Override
    public List<O> getAllExpanded(DbQuery query) {
        return findAll(query).map(it -> createExpanded(it, query))
                             .collect(Collectors.toList());
    }

    @Override
    public O getInstance(String id, DbQuery query) {
        BooleanExpression publicOffering = createPublicOfferingPredicate(id, query);
        Optional<E> entity = parameterRepository.findOne(publicOffering);
        return entity.map(it -> createExpanded(it, query)).orElse(null);
    }

    @Override
    public Collection<SearchResult> searchFor(DbQuery query) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean exists(String id, DbQuery query) {
        BooleanExpression publicOffering = createPublicOfferingPredicate(id, query);
        return parameterRepository.exists(publicOffering);
    }

    private BooleanExpression createPublicOfferingPredicate(String id, DbQuery query) {
        OfferingQuerySpecifications oFilterSpec = OfferingQuerySpecifications.of(query);
        return oFilterSpec.matchesPublicOffering(id);
    }

    private Stream<E> findAll(DbQuery query) {
        BooleanExpression predicate = createFilterPredicate(query);
        Iterable<E> entities = parameterRepository.findAll(predicate);
        return createStreamFromIterator(entities.iterator());
    }

    private BooleanExpression createFilterPredicate(DbQuery query) {
        DatasetQuerySpecifications dsFilterSpec = DatasetQuerySpecifications.of(query);
        JPQLQuery<DatasetEntity> subQuery = dsFilterSpec.toSubquery(dsFilterSpec.matchFilters());

        OfferingQuerySpecifications oFilterSpec = OfferingQuerySpecifications.of(query);
        return oFilterSpec.selectFrom(subQuery);
    }

    protected ServiceEntity getServiceEntity(DescribableEntity entity) {
        assertServiceAvailable(entity);
        return entity.getService() != null
            ? entity.getService()
            : serviceEntity;
    }

    private void assertServiceAvailable(DescribableEntity entity) throws IllegalStateException {
        if (serviceEntity == null && entity == null) {
            throw new IllegalStateException("No service instance available");
        }
    }

}
