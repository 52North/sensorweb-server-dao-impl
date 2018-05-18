package org.n52.series.srv;

import org.n52.io.response.PlatformOutput;
import org.n52.series.db.da.OutputAssembler;
import org.n52.series.db.dao.DbQueryFactory;

public class PlatformService extends AccessService<PlatformOutput> {

    public PlatformService(OutputAssembler<PlatformOutput> repository, DbQueryFactory queryFactory) {
        super(repository, queryFactory);
    }

}
