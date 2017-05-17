
package org.n52.series.db.dao;

import java.util.List;

public interface SearchableDao<T> {

    List<T> find(DbQuery query);
}
