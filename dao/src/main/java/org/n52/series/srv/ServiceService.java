package org.n52.series.srv;

import org.n52.io.response.ServiceOutput;
import org.n52.series.db.old.dao.DbQueryFactory;
import org.springframework.stereotype.Component;

@Component
public class ServiceService extends AccessService<ServiceOutput> {

    public ServiceService(OutputAssembler<ServiceOutput> repository, DbQueryFactory queryFactory) {
        super(repository, queryFactory);
    }

}
