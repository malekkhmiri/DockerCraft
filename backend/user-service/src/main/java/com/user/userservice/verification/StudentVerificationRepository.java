package com.user.userservice.verification;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface StudentVerificationRepository extends JpaRepository<StudentVerification, Long> {

    Optional<StudentVerification> findByUserId(Long userId);

    List<StudentVerification> findByStatus(StudentVerification.VerificationStatus status);

    @Query("SELECT sv FROM StudentVerification sv WHERE sv.status = 'AI_APPROVED' OR sv.status = 'ADMIN_APPROVED' AND sv.expiresAt < :date")
    List<StudentVerification> findExpiredVerifications(LocalDate date);

    @Query("SELECT sv FROM StudentVerification sv WHERE (sv.status = 'AI_APPROVED' OR sv.status = 'ADMIN_APPROVED') AND sv.expiresAt BETWEEN :startDate AND :endDate")
    List<StudentVerification> findVerificationsExpiringSoon(@org.springframework.data.repository.query.Param("startDate") LocalDate startDate, @org.springframework.data.repository.query.Param("endDate") LocalDate endDate);

    @Query("SELECT CASE WHEN COUNT(sv) > 0 THEN true ELSE false END FROM StudentVerification sv " +
           "WHERE ((sv.extractedStudentId = :studentId AND sv.extractedStudentId IS NOT NULL AND sv.extractedStudentId <> 'Inconnu') " +
           "       OR (sv.extractedName = :studentName AND sv.extractedName IS NOT NULL AND sv.extractedName <> 'Inconnu')) " +
           "AND sv.user.id <> :userId " +
           "AND sv.status IN ('AI_APPROVED', 'ADMIN_APPROVED')")
    boolean existsByStudentIdOrNameAndOtherUser(
            @org.springframework.data.repository.query.Param("studentId") String studentId, 
            @org.springframework.data.repository.query.Param("studentName") String studentName, 
            @org.springframework.data.repository.query.Param("userId") Long userId);
}
