package qilin.android.manifest;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public interface IManifestHandler extends AutoCloseable{

    /**
     * Returns all activities in the Android app
     *
     * @return list with all activities
     */
    List<String> getActivities();


    default Set<String> getEntryPointClasses() {
        return new HashSet<>(getActivities());
    }
}
