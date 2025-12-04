package com.tim.appTim.service;

import com.tim.appTim.dto.ContactPersonRequestDTO;
import com.tim.appTim.entity.Company;
import com.tim.appTim.entity.ContactPerson;
import com.tim.appTim.repository.CompanyRepository;
import com.tim.appTim.repository.ContactRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ContactServiceImplTest {

    @Mock
    private ContactRepository contactRepository;

    @Mock
    private CompanyRepository companyRepository;

    @InjectMocks
    private ContactServiceImpl contactService;

    private ContactPersonRequestDTO requestDTO;
    private Company company;
    private ContactPerson contactPerson;

    @BeforeEach
    void setUp() {
        company = new Company();
        company.setId(1L);
        company.setName("Test Company");

        requestDTO = new ContactPersonRequestDTO();
        requestDTO.setCompanyId(1L);
        requestDTO.setName("John Doe");
        requestDTO.setPosition("Manager");
        requestDTO.setEmail("john@example.com");
        requestDTO.setPhone("0123456789");
        requestDTO.setDob(LocalDate.of(1990, 1, 1));

        contactPerson = new ContactPerson();
        contactPerson.setId(1L);
        contactPerson.setName("John Doe");
        contactPerson.setPosition("Manager");
        contactPerson.setEmail("john@example.com");
        contactPerson.setPhone("0123456789");
        contactPerson.setDob(LocalDate.of(1990, 1, 1));
        contactPerson.setCompany(company);
    }

    @Test
    void create_WhenCompanyExists_ShouldCreateContact() {
        // Arrange
        when(companyRepository.findById(1L)).thenReturn(Optional.of(company));
        when(contactRepository.save(any(ContactPerson.class))).thenAnswer(invocation -> {
            ContactPerson saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        // Act
        ContactPerson result = contactService.create(requestDTO);

        // Assert
        assertNotNull(result);
        assertEquals(requestDTO.getName(), result.getName());
        assertEquals(requestDTO.getPosition(), result.getPosition());
        assertEquals(requestDTO.getEmail(), result.getEmail());
        assertEquals(requestDTO.getPhone(), result.getPhone());
        assertEquals(requestDTO.getDob(), result.getDob());
        assertEquals(company, result.getCompany());
        verify(companyRepository).findById(1L);
        verify(contactRepository).save(any(ContactPerson.class));
    }

    @Test
    void create_WhenCompanyNotExists_ShouldThrowException() {
        // Arrange
        when(companyRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            requestDTO.setCompanyId(999L);
            contactService.create(requestDTO);
        });
        assertTrue(exception.getMessage().contains("Công ty không tồn tại"));
        verify(companyRepository).findById(999L);
        verify(contactRepository, never()).save(any(ContactPerson.class));
    }

    @Test
    void create_ShouldMapAllFieldsCorrectly() {
        // Arrange
        when(companyRepository.findById(1L)).thenReturn(Optional.of(company));
        when(contactRepository.save(any(ContactPerson.class))).thenAnswer(invocation -> {
            ContactPerson saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        // Act
        ContactPerson result = contactService.create(requestDTO);

        // Assert
        assertNotNull(result);
        assertEquals(requestDTO.getName(), result.getName());
        assertEquals(requestDTO.getPosition(), result.getPosition());
        assertEquals(requestDTO.getEmail(), result.getEmail());
        assertEquals(requestDTO.getPhone(), result.getPhone());
        assertEquals(requestDTO.getDob(), result.getDob());
        assertNotNull(result.getCompany());
        verify(contactRepository).save(any(ContactPerson.class));
    }

    @Test
    void getContactsByCompany_WhenContactsExist_ShouldReturnList() {
        // Arrange
        List<ContactPerson> contacts = new ArrayList<>();
        contacts.add(contactPerson);
        when(contactRepository.findByCompanyId(1L)).thenReturn(contacts);

        // Act
        List<ContactPerson> result = contactService.getContactsByCompany(1L);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(contactPerson.getName(), result.get(0).getName());
        verify(contactRepository).findByCompanyId(1L);
    }

    @Test
    void getContactsByCompany_WhenNoContacts_ShouldReturnEmptyList() {
        // Arrange
        when(contactRepository.findByCompanyId(1L)).thenReturn(new ArrayList<>());

        // Act
        List<ContactPerson> result = contactService.getContactsByCompany(1L);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(contactRepository).findByCompanyId(1L);
    }

    @Test
    void getContactsByCompany_WithMultipleContacts_ShouldReturnAll() {
        // Arrange
        ContactPerson contact2 = new ContactPerson();
        contact2.setId(2L);
        contact2.setName("Jane Doe");
        contact2.setCompany(company);

        List<ContactPerson> contacts = List.of(contactPerson, contact2);
        when(contactRepository.findByCompanyId(1L)).thenReturn(contacts);

        // Act
        List<ContactPerson> result = contactService.getContactsByCompany(1L);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(contactRepository).findByCompanyId(1L);
    }

    @Test
    void delete_WhenContactExists_ShouldDeleteContact() {
        // Arrange
        when(contactRepository.existsById(1L)).thenReturn(true);
        doNothing().when(contactRepository).deleteById(1L);

        // Act
        contactService.delete(1L);

        // Assert
        verify(contactRepository).existsById(1L);
        verify(contactRepository).deleteById(1L);
    }

    @Test
    void delete_WhenContactNotExists_ShouldThrowException() {
        // Arrange
        when(contactRepository.existsById(999L)).thenReturn(false);

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            contactService.delete(999L);
        });
        assertTrue(exception.getMessage().contains("Người liên hệ không tồn tại"));
        verify(contactRepository).existsById(999L);
        verify(contactRepository, never()).deleteById(anyLong());
    }

    @Test
    void create_WhenFieldsAreNull_ShouldStillCreate() {
        // Arrange
        requestDTO.setName(null);
        requestDTO.setPosition(null);
        requestDTO.setEmail(null);
        requestDTO.setPhone(null);
        requestDTO.setDob(null);
        
        when(companyRepository.findById(1L)).thenReturn(Optional.of(company));
        when(contactRepository.save(any(ContactPerson.class))).thenAnswer(invocation -> {
            ContactPerson saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        // Act
        ContactPerson result = contactService.create(requestDTO);

        // Assert
        assertNotNull(result);
        assertNull(result.getName());
        assertNull(result.getPosition());
        assertNull(result.getEmail());
        assertNull(result.getPhone());
        assertNull(result.getDob());
        assertNotNull(result.getCompany());
        verify(contactRepository).save(any(ContactPerson.class));
    }
}

