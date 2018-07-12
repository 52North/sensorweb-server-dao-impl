package org.n52.series.db.query;

import org.n52.series.db.beans.DatasetEntity;
import org.n52.series.db.beans.QDatasetEntity;
import org.n52.series.db.beans.QProcedureEntity;
import org.n52.series.db.old.dao.DbQuery;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.JPQLQuery;

public class ProcedureQuerySpecifications extends ParameterQuerySpecifications {

	public static ProcedureQuerySpecifications of(DbQuery dbQuery) {
        return new ProcedureQuerySpecifications(dbQuery);
    }

    private ProcedureQuerySpecifications(DbQuery dbQuery) {
        super(dbQuery);
    }

    /**
     * Matches procedures included in a result of a given subquery, i.e.
     *
     * <pre>
     *   where id in (select fk_procedure_id from dataset where &lt;subquery&gt;)
     * </pre>
     *
     * @param subquery
     *        the query
     * @return a boolean expression
     */
    public BooleanExpression selectFrom(JPQLQuery<DatasetEntity> subquery) {
        QDatasetEntity datasetentity = QDatasetEntity.datasetEntity;
        QProcedureEntity procedureentity = QProcedureEntity.procedureEntity;
        return procedureentity.id.in(subquery.select(datasetentity.procedure.id));
    }

    public BooleanExpression matchesPublicProcedure(String id) {
        DatasetQuerySpecifications dsFilterSpec = DatasetQuerySpecifications.of(query);
        BooleanExpression datasetPredicate = dsFilterSpec.matchProcedures(id)
                                                         .and(dsFilterSpec.isPublic());
        return selectFrom(dsFilterSpec.toSubquery(datasetPredicate));
    }
}
