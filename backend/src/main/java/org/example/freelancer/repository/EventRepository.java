package org.example.freelancer.repository;

import org.example.freelancer.entity.Event;
import org.example.freelancer.entity.EventStatus;
import org.example.freelancer.entity.EventType;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface EventRepository extends JpaRepository<Event, Long> {

    List<Event> findByStatus(EventStatus status);

    List<Event> findByType(EventType type);

    List<Event> findByStatusOrderByStartDateAsc(EventStatus status);

    List<Event> findByCreatedById(Long createdById);

    List<Event> findByStatusAndType(EventStatus status, EventType type);
}
