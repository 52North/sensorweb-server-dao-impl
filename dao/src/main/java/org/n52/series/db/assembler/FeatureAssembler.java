
package org.n52.series.db.assembler;

import org.n52.io.response.FeatureOutput;
import org.n52.series.db.DatasetRepository;
import org.n52.series.db.FeatureRepository;
import org.n52.series.db.beans.DatasetEntity;
import org.n52.series.db.beans.FeatureEntity;
import org.n52.series.db.old.dao.DbQuery;
import org.n52.series.db.query.DatasetQuerySpecifications;
import org.n52.series.db.query.FeatureQuerySpecifications;
import org.springframework.stereotype.Component;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.JPQLQuery;

@Component
public class FeatureAssembler extends ParameterOutputAssembler<FeatureEntity, FeatureOutput> {

    public FeatureAssembler(FeatureRepository featureRepository,
                            DatasetRepository<DatasetEntity> datasetRepository) {
        super(featureRepository, datasetRepository);
    }

    @Override
    protected FeatureOutput prepareEmptyOutput() {
        return new FeatureOutput();
    }

    BooleanExpression createFilterPredicate(DbQuery query) {
        DatasetQuerySpecifications dsFilterSpec = DatasetQuerySpecifications.of(query);
        JPQLQuery<DatasetEntity> subQuery = dsFilterSpec.toSubquery(dsFilterSpec.matchFilters());

        FeatureQuerySpecifications fFilterSpec = FeatureQuerySpecifications.of(query);
        return fFilterSpec.selectFrom(subQuery);
    }
}
