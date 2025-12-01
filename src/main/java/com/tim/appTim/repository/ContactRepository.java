package com.tim.appTim.repository;

import com.tim.appTim.entity.ContactPerson;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ContactRepository extends JpaRepository<ContactPerson, Long> {
    List<ContactPerson> findByCompanyId(Long companyId);
}