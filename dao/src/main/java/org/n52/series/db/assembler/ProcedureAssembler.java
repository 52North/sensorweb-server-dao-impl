
package org.n52.series.db.assembler;

import org.n52.io.response.ProcedureOutput;
import org.n52.series.db.DatasetRepository;
import org.n52.series.db.ProcedureRepository;
import org.n52.series.db.beans.DatasetEntity;
import org.n52.series.db.beans.ProcedureEntity;
import org.n52.series.db.old.dao.DbQuery;
import org.n52.series.db.query.DatasetQuerySpecifications;
import org.n52.series.db.query.ProcedureQuerySpecifications;
import org.springframework.stereotype.Component;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.JPQLQuery;

@Component
public class ProcedureAssembler extends ParameterOutputAssembler<ProcedureEntity, ProcedureOutput> {

    public ProcedureAssembler(ProcedureRepository procedureRepository,
                             DatasetRepository<DatasetEntity> datasetRepository) {
        super(procedureRepository, datasetRepository);
    }

    @Override
    protected ProcedureOutput prepareEmptyOutput() {
        return new ProcedureOutput();
    }

    BooleanExpression createFilterPredicate(DbQuery query) {
        DatasetQuerySpecifications dsFilterSpec = DatasetQuerySpecifications.of(query);
        JPQLQuery<DatasetEntity> subQuery = dsFilterSpec.toSubquery(dsFilterSpec.matchFilters());

        ProcedureQuerySpecifications pFilterSpec = ProcedureQuerySpecifications.of(query);
        return pFilterSpec.selectFrom(subQuery);
    }
}
