package org.n52.series.srv;

import org.n52.io.response.PhenomenonOutput;
import org.n52.series.db.da.OutputAssembler;
import org.n52.series.db.dao.DbQueryFactory;

public class PhenomenonService extends AccessService<PhenomenonOutput> {

    public PhenomenonService(OutputAssembler<PhenomenonOutput> repository, DbQueryFactory queryFactory) {
        super(repository, queryFactory);
    }

}
