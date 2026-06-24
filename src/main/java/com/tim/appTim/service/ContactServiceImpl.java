package com.tim.appTim.service;

import com.tim.appTim.dto.request.ContactPersonRequestDTO;
import com.tim.appTim.entity.Company;
import com.tim.appTim.entity.ContactPerson;
import com.tim.appTim.repository.CompanyRepository;
import com.tim.appTim.repository.ContactRepository;
import com.tim.appTim.service.ContactService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ContactServiceImpl implements ContactService {

    private final ContactRepository contactRepository;
    private final CompanyRepository companyRepository;

    @Override
    @Transactional
    public ContactPerson create(ContactPersonRequestDTO request) {
        Company company = companyRepository.findById(request.getCompanyId())
                .orElseThrow(() -> new RuntimeException("Công ty không tồn tại với ID: " + request.getCompanyId()));

        ContactPerson contact = new ContactPerson();
        contact.setName(request.getName());
        contact.setPosition(request.getPosition());
        contact.setEmail(request.getEmail());
        contact.setPhone(request.getPhone());
        contact.setDob(request.getDob());

        contact.setCompany(company);

        return contactRepository.save(contact);
    }

    @Override
    public List<ContactPerson> getContactsByCompany(Long companyId) {
        return contactRepository.findByCompanyId(companyId);
    }

    @Override
    public void delete(Long id) {
        if (!contactRepository.existsById(id)) {
            throw new RuntimeException("Người liên hệ không tồn tại");
        }
        contactRepository.deleteById(id);
    }
}
