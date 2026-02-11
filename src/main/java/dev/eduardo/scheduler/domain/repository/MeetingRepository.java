package dev.eduardo.scheduler.domain.repository;

import dev.eduardo.scheduler.domain.entities.Meeting;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface MeetingRepository extends CrudRepository<Meeting, UUID> {

}
