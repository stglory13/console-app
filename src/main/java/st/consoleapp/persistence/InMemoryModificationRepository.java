package st.consoleapp.persistence;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class InMemoryModificationRepository implements ModificationRepository {

    private final Map<String, AtomicInteger> modifications = new ConcurrentHashMap<>();

    @Override
    public void saveModification(String userId) {
        modifications
                .computeIfAbsent(userId, k -> new AtomicInteger(0))
                .incrementAndGet();
    }

    /**
     * Snapshot of modifications per user
     */
    @Override
    public Map<String, Integer> countModificationsPerUser() {
        return modifications.entrySet()
                .stream()
                .collect(java.util.stream.Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue().get()
                ));
    }

    @Override
    public void close() {
        // No resources to close in this in-memory implementation
    }

}