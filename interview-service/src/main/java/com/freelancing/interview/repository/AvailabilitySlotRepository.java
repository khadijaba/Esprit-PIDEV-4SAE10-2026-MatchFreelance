package com.freelancing.interview.repository;

import com.freelancing.interview.entity.AvailabilitySlot;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Optional;

public interface AvailabilitySlotRepository extends JpaRepository<AvailabilitySlot, Long> {
    Page<AvailabilitySlot> findByFreelancerId(Long freelancerId, Pageable pageable);

    Page<AvailabilitySlot> findByFreelancerIdAndBookedFalse(Long freelancerId, Pageable pageable);

    Page<AvailabilitySlot> findByFreelancerIdAndStartAtGreaterThanEqualAndEndAtLessThanEqual(
            Long freelancerId,
            Instant from,
            Instant to,
            Pageable pageable
    );

    Page<AvailabilitySlot> findByFreelancerIdAndBookedFalseAndStartAtGreaterThanEqualAndEndAtLessThanEqual(
            Long freelancerId,
            Instant from,
            Instant to,
            Pageable pageable
    );

    Optional<AvailabilitySlot> findFirstByFreelancerIdAndBookedFalseAndStartAtLessThanEqualAndEndAtGreaterThanEqual(
            Long freelancerId,
            Instant startAt,
            Instant endAt
    );
}

