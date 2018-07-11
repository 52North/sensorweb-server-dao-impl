package org.n52.series.srv;

import org.n52.io.response.ProcedureOutput;
import org.n52.series.db.old.dao.DbQueryFactory;
import org.springframework.stereotype.Component;

@Component
public class ProcedureService extends AccessService<ProcedureOutput> {

    public ProcedureService(OutputAssembler<ProcedureOutput> repository, DbQueryFactory queryFactory) {
        super(repository, queryFactory);
    }

}
