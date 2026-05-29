// PreRegisteredMemberRepository.java
package com.example.LibraryManagement.repository;
import com.example.LibraryManagement.entity.PreRegisteredMember;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface PreRegisteredMemberRepository extends JpaRepository<PreRegisteredMember, Long> {
    Optional<PreRegisteredMember> findByEmail(String email);
}