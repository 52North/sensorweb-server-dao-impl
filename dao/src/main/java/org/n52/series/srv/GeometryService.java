package org.n52.series.srv;

import org.n52.io.response.GeometryOutput;
import org.n52.series.db.old.dao.DbQueryFactory;
import org.springframework.stereotype.Component;

@Component
public class GeometryService extends AccessService<GeometryOutput> {

    public GeometryService(OutputAssembler<GeometryOutput> repository, DbQueryFactory queryFactory) {
        super(repository, queryFactory);
    }

}
