package dk.wandywharang.service;

import dk.wandywharang.api.Examiner;
import io.smallrye.mutiny.Uni;

import java.util.List;

public interface ExaminerService {

    /**
     * Finds the Members of any Club whose name matches the query.
     */
    Uni<List<Examiner>> search(String query);
}
