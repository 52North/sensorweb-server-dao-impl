package org.n52.series.srv;

import org.n52.io.response.PlatformOutput;
import org.n52.series.db.old.dao.DbQueryFactory;
import org.springframework.stereotype.Component;

@Component
public class PlatformService extends AccessService<PlatformOutput> {

    public PlatformService(OutputAssembler<PlatformOutput> repository, DbQueryFactory queryFactory) {
        super(repository, queryFactory);
    }

}
