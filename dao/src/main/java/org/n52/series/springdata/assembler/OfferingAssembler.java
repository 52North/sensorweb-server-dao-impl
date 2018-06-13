
package org.n52.series.springdata.assembler;

import org.n52.io.response.OfferingOutput;
import org.n52.series.db.beans.DatasetEntity;
import org.n52.series.db.beans.OfferingEntity;
import org.n52.series.springdata.DatasetRepository;
import org.n52.series.springdata.OfferingRepository;

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
