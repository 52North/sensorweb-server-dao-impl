package org.n52.series.srv;

import org.n52.io.response.FeatureOutput;
import org.n52.series.db.da.OutputAssembler;
import org.n52.series.db.dao.DbQueryFactory;
import org.springframework.stereotype.Component;

@Component
public class FeatureService extends AccessService<FeatureOutput> {

    public FeatureService(OutputAssembler<FeatureOutput> repository, DbQueryFactory queryFactory) {
        super(repository, queryFactory);
    }

}
