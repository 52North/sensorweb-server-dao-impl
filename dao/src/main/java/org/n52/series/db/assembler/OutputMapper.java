package org.n52.series.db.assembler;

import org.n52.io.response.ParameterOutput;
import org.n52.series.db.beans.DescribableEntity;

public interface OutputMapper {

    <E extends DescribableEntity, O extends ParameterOutput> O createCondensed(E entity, O output);

}
