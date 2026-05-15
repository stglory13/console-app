package st.consoleapp.persistence;

import java.util.Map;

public interface ModificationRepository extends AutoCloseable {

    void saveModification(String userId);

    Map<String, Integer> countModificationsPerUser();

    @Override
    void close();
}