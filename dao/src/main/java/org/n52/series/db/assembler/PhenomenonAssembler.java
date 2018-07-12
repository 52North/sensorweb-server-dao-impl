
package org.n52.series.db.assembler;

import org.n52.io.response.PhenomenonOutput;
import org.n52.series.db.DatasetRepository;
import org.n52.series.db.PhenomenonRepository;
import org.n52.series.db.beans.DatasetEntity;
import org.n52.series.db.beans.PhenomenonEntity;
import org.n52.series.db.old.dao.DbQuery;
import org.n52.series.db.query.DatasetQuerySpecifications;
import org.n52.series.db.query.PhenomenonQuerySpecifications;
import org.springframework.stereotype.Component;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.JPQLQuery;

@Component
public class PhenomenonAssembler extends ParameterOutputAssembler<PhenomenonEntity, PhenomenonOutput> {

    public PhenomenonAssembler(PhenomenonRepository phenomenonRepository,
                               DatasetRepository<DatasetEntity> datasetRepository) {
        super(phenomenonRepository, datasetRepository);
    }

    @Override
    protected PhenomenonOutput prepareEmptyOutput() {
        return new PhenomenonOutput();
    }

    BooleanExpression createFilterPredicate(DbQuery query) {
        DatasetQuerySpecifications dsFilterSpec = DatasetQuerySpecifications.of(query);
        JPQLQuery<DatasetEntity> subQuery = dsFilterSpec.toSubquery(dsFilterSpec.matchFilters());

        PhenomenonQuerySpecifications pFilterSpec = PhenomenonQuerySpecifications.of(query);
        return pFilterSpec.selectFrom(subQuery);
    }
}