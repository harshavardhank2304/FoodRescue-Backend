package com.foodrescue.backend.repository;

import com.foodrescue.backend.entity.VolunteerTask;
import com.foodrescue.backend.enums.TaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VolunteerTaskRepository extends JpaRepository<VolunteerTask, Long> {

    List<VolunteerTask> findByVolunteerId(Long volunteerId);

    List<VolunteerTask> findByVolunteerIdOrVolunteerIsNull(Long volunteerId);

    Optional<VolunteerTask> findByRequestId(Long requestId);

    long countByStatus(TaskStatus status);
}
