package org.n52.series.springdata.assembler;

import org.n52.io.request.IoParameters;
import org.n52.io.response.ParameterOutput;
import org.n52.series.db.beans.DescribableEntity;
import org.n52.series.db.dao.DbQuery;

public final class ParameterOutputMapper {
    
    public static <E extends DescribableEntity, O extends ParameterOutput> O createCondensed(E entity, O result, DbQuery query) {
        IoParameters parameters = query.getParameters();

        Long id = entity.getId();
        String label = entity.getLabelFrom(query.getLocale());
        String domainId = entity.getIdentifier();
        String hrefBase = query.getHrefBase();

        result.setId(Long.toString(id));
        result.setValue(ParameterOutput.LABEL, label, parameters, result::setLabel);
        result.setValue(ParameterOutput.DOMAIN_ID, domainId, parameters, result::setDomainId);
        if (!parameters.shallBehaveBackwardsCompatible()) {
            result.setValue(ParameterOutput.HREF_BASE, hrefBase, parameters, result::setHrefBase);
        }
        return result;
    }
}
