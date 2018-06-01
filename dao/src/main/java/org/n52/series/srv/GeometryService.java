package org.n52.series.srv;

import org.n52.io.response.GeometryInfo;
import org.n52.series.db.da.OutputAssembler;
import org.n52.series.db.dao.DbQueryFactory;
import org.springframework.stereotype.Component;

@Component
public class GeometryService extends AccessService<GeometryInfo> {

    public GeometryService(OutputAssembler<GeometryInfo> repository, DbQueryFactory queryFactory) {
        super(repository, queryFactory);
    }

}
