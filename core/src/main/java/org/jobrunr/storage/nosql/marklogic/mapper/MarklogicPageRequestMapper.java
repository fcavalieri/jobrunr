package org.jobrunr.storage.nosql.marklogic.mapper;

import org.jobrunr.storage.PageRequest;
import org.jobrunr.storage.StorageProviderUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MarklogicPageRequestMapper {

    private static final Set<String> allowedSortColumns = new HashSet<>();

    static {
        allowedSortColumns.add(StorageProviderUtils.Jobs.FIELD_SCHEDULED_AT);
        allowedSortColumns.add(StorageProviderUtils.Jobs.FIELD_CREATED_AT);
        allowedSortColumns.add(StorageProviderUtils.Jobs.FIELD_UPDATED_AT);
    }

    public List<Map<String, Object>> map(PageRequest pageRequest) {
        final List<Map<String, Object>> result = new ArrayList<>();
        final String[] sortOns = pageRequest.getOrder().split(",");
        for (String sortOn : sortOns) {
            final String[] sortAndOrder = sortOn.split(":");
            if (!allowedSortColumns.contains(sortAndOrder[0])) continue;
            String sortField = sortAndOrder[0];
            PageRequest.Order order = PageRequest.Order.ASC;
            if (sortAndOrder.length > 1) {
                order = PageRequest.Order.valueOf(sortAndOrder[1].toUpperCase());
            }
            Map<String, Object> marklogicOrder = new HashMap<>();
            marklogicOrder.put("type", "long");
            marklogicOrder.put("direction", order == PageRequest.Order.ASC ? "ascending" : "descending");
            marklogicOrder.put("json-property", sortField);
            result.add(marklogicOrder);
        }
        return result;
    }

}
