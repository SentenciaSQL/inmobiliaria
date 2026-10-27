package rd.alzada.reservations;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

public interface ReservationRepository extends JpaRepository<Reservation, UUID> {

  Optional<Reservation> findByIdAndTenantId(UUID id, UUID tenantId);

  Optional<Reservation> findByUnitIdAndStatus(UUID unitId, Reservation.Status status);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select reservation from Reservation reservation where reservation.id = :id and reservation.tenantId = :tenantId")
  Optional<Reservation> lock(@Param("id") UUID id, @Param("tenantId") UUID tenantId);

  @Query("""
      select reservation from Reservation reservation
      where reservation.tenantId = :tenantId
        and (:status is null or reservation.status = :status)
        and (:unitId is null or reservation.unitId = :unitId)
      """)
  Page<Reservation> search(
      @Param("tenantId") UUID tenantId,
      @Param("status") Reservation.Status status,
      @Param("unitId") UUID unitId,
      Pageable pageable);

  @Query("select reservation from Reservation reservation where reservation.status = rd.alzada.reservations.Reservation.Status.ACTIVE and reservation.expiresAt < :now")
  List<Reservation> findExpired(@Param("now") Instant now);

  @Query("""
      select reservation from Reservation reservation
      where reservation.status = rd.alzada.reservations.Reservation.Status.ACTIVE
        and reservation.expiryNoticeSentAt is null
        and reservation.expiresAt > :now
        and reservation.expiresAt <= :limit
      """)
  List<Reservation> findExpiring(@Param("now") Instant now, @Param("limit") Instant limit);

  @Query("""
      select count(reservation) from Reservation reservation
      where reservation.tenantId = :tenantId
        and reservation.status = rd.alzada.reservations.Reservation.Status.ACTIVE
        and reservation.expiresAt > :now
        and reservation.expiresAt <= :limit
      """)
  long countExpiring(@Param("tenantId") UUID tenantId, @Param("now") Instant now, @Param("limit") Instant limit);
}
