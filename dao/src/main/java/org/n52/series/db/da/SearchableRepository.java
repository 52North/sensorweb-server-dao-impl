package org.n52.series.db.da;

import java.util.Collection;

import org.n52.io.request.IoParameters;
import org.n52.series.spi.search.SearchResult;

public interface SearchableRepository {

    Collection<SearchResult> searchFor(IoParameters paramters);
}
