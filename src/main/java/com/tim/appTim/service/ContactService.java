package com.tim.appTim.service;

import com.tim.appTim.dto.request.ContactPersonRequestDTO;
import com.tim.appTim.entity.ContactPerson;

import java.util.List;

public interface ContactService {
    ContactPerson create(ContactPersonRequestDTO request);

    List<ContactPerson> getContactsByCompany(Long companyId);

    void delete(Long id);
}
