package org.n52.series.srv;

import org.n52.io.response.ServiceOutput;
import org.n52.series.db.da.OutputAssembler;
import org.n52.series.db.dao.DbQueryFactory;
import org.springframework.stereotype.Component;

@Component
public class ServiceService extends AccessService<ServiceOutput> {

    public ServiceService(OutputAssembler<ServiceOutput> repository, DbQueryFactory queryFactory) {
        super(repository, queryFactory);
    }

}
