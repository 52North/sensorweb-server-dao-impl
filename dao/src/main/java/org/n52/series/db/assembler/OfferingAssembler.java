
package org.n52.series.db.assembler;

import org.n52.io.response.OfferingOutput;
import org.n52.series.db.DatasetRepository;
import org.n52.series.db.OfferingRepository;
import org.n52.series.db.beans.DatasetEntity;
import org.n52.series.db.beans.OfferingEntity;
import org.springframework.stereotype.Component;

@Component
public class OfferingAssembler extends ParameterOutputAssembler<OfferingEntity, OfferingOutput> {

    public OfferingAssembler(OfferingRepository offeringRepository,
                             DatasetRepository<DatasetEntity> datasetRepository) {
        super(offeringRepository, datasetRepository);
    }

    @Override
    protected OfferingOutput prepareEmptyOutput() {
        return new OfferingOutput();
    }

}
