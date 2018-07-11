package org.n52.series.db.assembler;

import org.n52.io.request.IoParameters;
import org.n52.io.response.ParameterOutput;
import org.n52.series.db.beans.DescribableEntity;
import org.n52.series.db.old.dao.DbQuery;
import org.n52.series.db.old.dao.DefaultDbQueryFactory;

public final class ParameterOutputMapper implements OutputMapper {

    private final DbQuery query;

    public ParameterOutputMapper(final DbQuery query) {
        this.query = query == null
                ? new DefaultDbQueryFactory().createDefault()
                : query;
    }

    @Override
    public <E extends DescribableEntity, O extends ParameterOutput> O createCondensed(final E entity, final O output) {
        final IoParameters parameters = query.getParameters();

        final Long id = entity.getId();
        final String label = entity.getLabelFrom(query.getLocale());
        final String domainId = entity.getIdentifier();
        final String hrefBase = query.getHrefBase();

        output.setId(Long.toString(id));
        output.setValue(ParameterOutput.LABEL, label, parameters, output::setLabel);
        output.setValue(ParameterOutput.DOMAIN_ID, domainId, parameters, output::setDomainId);
        if (!parameters.shallBehaveBackwardsCompatible()) {
            output.setValue(ParameterOutput.HREF_BASE, hrefBase, parameters, output::setHrefBase);
        }
        return output;
    }
}
