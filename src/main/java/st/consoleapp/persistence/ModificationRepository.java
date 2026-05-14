package st.consoleapp.persistence;

import java.util.Map;

public interface ModificationRepository {

    void saveModification(String userId);

    Map<String, Integer> countModificationsPerUser();

}