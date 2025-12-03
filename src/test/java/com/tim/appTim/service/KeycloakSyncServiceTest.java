package com.tim.appTim.service;

import com.tim.appTim.controller.KeycloakController;
import com.tim.appTim.entity.Role;
import com.tim.appTim.entity.User;
import com.tim.appTim.repository.RoleRepository;
import com.tim.appTim.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.representations.idm.UserRepresentation;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import javax.ws.rs.ClientErrorException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.StatusType;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KeycloakSyncServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private Keycloak keycloak;

    @Mock
    private RealmResource realmResource;

    @Mock
    private UsersResource usersResource;

    @Mock
    private UserResource userResource;

    @InjectMocks
    private KeycloakSyncService keycloakSyncService;

    private Role defaultRole;
    private User existingUser;
    private UserRepresentation kcUser;
    private KeycloakController.UpdateUserDTO updateUserDTO;

    @BeforeEach
    void setUp() {
        // Set up @Value fields using ReflectionTestUtils
        ReflectionTestUtils.setField(keycloakSyncService, "serverUrl", "http://localhost:8080");
        ReflectionTestUtils.setField(keycloakSyncService, "realm", "test-realm");
        ReflectionTestUtils.setField(keycloakSyncService, "clientId", "test-client");
        ReflectionTestUtils.setField(keycloakSyncService, "clientSecret", "test-secret");

        defaultRole = new Role();
        defaultRole.setId(1L);
        defaultRole.setName("ROLE_USER");

        existingUser = new User();
        existingUser.setId(1L);
        existingUser.setKeycloakId("kc-user-1");
        existingUser.setUsername("existinguser");
        existingUser.setEmail("existing@example.com");
        existingUser.setFirstName("Existing");
        existingUser.setLastName("User");
        existingUser.setDeleted(false);
        existingUser.setRoles(Set.of(defaultRole));

        kcUser = new UserRepresentation();
        kcUser.setId("kc-user-1");
        kcUser.setUsername("testuser");
        kcUser.setEmail("test@example.com");
        kcUser.setFirstName("Test");
        kcUser.setLastName("User");
        kcUser.setEnabled(true);

        updateUserDTO = new KeycloakController.UpdateUserDTO();
    }


    // --- syncUsers ---

    @Test
    void syncUsers_WithNewUsers_ShouldCreateUsers() throws Exception {
        // Arrange
        List<UserRepresentation> kcUsers = List.of(kcUser);
        
        when(roleRepository.findByName("ROLE_USER")).thenReturn(Optional.of(defaultRole));
        when(userRepository.findAnyByKeycloakId("kc-user-1")).thenReturn(Optional.empty());
        when(userRepository.findAnyByEmail("test@example.com")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(1L);
            return user;
        });

        // Mock Keycloak client - now that getKeycloakClient() is package-private, we can mock it
        KeycloakSyncService spyService = spy(keycloakSyncService);
        doReturn(keycloak).when(spyService).getKeycloakClient();
        
        when(keycloak.realm("test-realm")).thenReturn(realmResource);
        when(realmResource.users()).thenReturn(usersResource);
        when(usersResource.list()).thenReturn(kcUsers);

        // Act
        List<User> result = spyService.syncUsers();

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getKeycloakId()).isEqualTo("kc-user-1");
        assertThat(result.get(0).getUsername()).isEqualTo("testuser");
        verify(userRepository).save(any(User.class));
        verify(keycloak).close();
    }

    @Test
    void syncUsers_WithExistingUsers_ShouldUpdateUsers() throws Exception {
        // Arrange
        List<UserRepresentation> kcUsers = List.of(kcUser);
        
        when(roleRepository.findByName("ROLE_USER")).thenReturn(Optional.of(defaultRole));
        when(userRepository.findAnyByKeycloakId("kc-user-1")).thenReturn(Optional.of(existingUser));
        when(userRepository.save(any(User.class))).thenReturn(existingUser);

        // Mock Keycloak client
        KeycloakSyncService spyService = spy(keycloakSyncService);
        doReturn(keycloak).when(spyService).getKeycloakClient();
        when(keycloak.realm("test-realm")).thenReturn(realmResource);
        when(realmResource.users()).thenReturn(usersResource);
        when(usersResource.list()).thenReturn(kcUsers);

        // Act
        List<User> result = spyService.syncUsers();

        // Assert
        assertThat(result).isEmpty(); // Existing users are not added to syncedUsers list
        verify(userRepository).save(existingUser);
        verify(keycloak).close();
    }

    @Test
    void syncUsers_WithDeletedUser_ShouldRestoreUser() throws Exception {
        // Arrange
        existingUser.setDeleted(true);
        List<UserRepresentation> kcUsers = List.of(kcUser);
        
        when(roleRepository.findByName("ROLE_USER")).thenReturn(Optional.of(defaultRole));
        when(userRepository.findAnyByKeycloakId("kc-user-1")).thenReturn(Optional.of(existingUser));
        when(userRepository.save(any(User.class))).thenReturn(existingUser);

        // Mock Keycloak client
        KeycloakSyncService spyService = spy(keycloakSyncService);
        doReturn(keycloak).when(spyService).getKeycloakClient();
        when(keycloak.realm("test-realm")).thenReturn(realmResource);
        when(realmResource.users()).thenReturn(usersResource);
        when(usersResource.list()).thenReturn(kcUsers);

        // Act
        spyService.syncUsers();

        // Assert
        assertThat(existingUser.isDeleted()).isFalse();
        verify(userRepository).save(existingUser);
    }

    @Test
    void syncUsers_WhenUserNeedsUpdate_ShouldUpdateFields() throws Exception {
        // Arrange
        kcUser.setUsername("updatedusername");
        kcUser.setEmail("updated@example.com");
        kcUser.setFirstName("Updated");
        kcUser.setLastName("Name");
        List<UserRepresentation> kcUsers = List.of(kcUser);
        
        when(roleRepository.findByName("ROLE_USER")).thenReturn(Optional.of(defaultRole));
        when(userRepository.findAnyByKeycloakId("kc-user-1")).thenReturn(Optional.of(existingUser));
        when(userRepository.save(any(User.class))).thenReturn(existingUser);

        // Mock Keycloak client
        KeycloakSyncService spyService = spy(keycloakSyncService);
        doReturn(keycloak).when(spyService).getKeycloakClient();
        when(keycloak.realm("test-realm")).thenReturn(realmResource);
        when(realmResource.users()).thenReturn(usersResource);
        when(usersResource.list()).thenReturn(kcUsers);

        // Act
        spyService.syncUsers();

        // Assert
        assertThat(existingUser.getUsername()).isEqualTo("updatedusername");
        assertThat(existingUser.getEmail()).isEqualTo("updated@example.com");
        assertThat(existingUser.getFirstName()).isEqualTo("Updated");
        assertThat(existingUser.getLastName()).isEqualTo("Name");
        verify(userRepository).save(existingUser);
    }

    @Test
    void syncUsers_WhenUserHasNoKeycloakId_ShouldSetKeycloakId() throws Exception {
        // Arrange
        existingUser.setKeycloakId(null);
        List<UserRepresentation> kcUsers = List.of(kcUser);
        
        when(roleRepository.findByName("ROLE_USER")).thenReturn(Optional.of(defaultRole));
        when(userRepository.findAnyByKeycloakId("kc-user-1")).thenReturn(Optional.empty());
        when(userRepository.findAnyByEmail("test@example.com")).thenReturn(Optional.of(existingUser));
        when(userRepository.save(any(User.class))).thenReturn(existingUser);

        // Mock Keycloak client
        KeycloakSyncService spyService = spy(keycloakSyncService);
        doReturn(keycloak).when(spyService).getKeycloakClient();
        when(keycloak.realm("test-realm")).thenReturn(realmResource);
        when(realmResource.users()).thenReturn(usersResource);
        when(usersResource.list()).thenReturn(kcUsers);

        // Act
        spyService.syncUsers();

        // Assert
        assertThat(existingUser.getKeycloakId()).isEqualTo("kc-user-1");
        verify(userRepository).save(existingUser);
    }

    @Test
    void syncUsers_WhenRoleNotFound_ShouldThrowException() throws Exception {
        // Arrange
        List<UserRepresentation> kcUsers = List.of(kcUser);
        
        when(roleRepository.findByName("ROLE_USER")).thenReturn(Optional.empty());

        // Mock Keycloak client
        KeycloakSyncService spyService = spy(keycloakSyncService);
        doReturn(keycloak).when(spyService).getKeycloakClient();
        when(keycloak.realm("test-realm")).thenReturn(realmResource);
        when(realmResource.users()).thenReturn(usersResource);
        when(usersResource.list()).thenReturn(kcUsers);

        // Act & Assert
        assertThatThrownBy(() -> spyService.syncUsers())
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Không tìm thấy Role 'ROLE_USER' trong DB");
        verify(keycloak).close();
    }

    @Test
    void syncUsers_WhenClientErrorException_ShouldThrowException() throws Exception {
        // Arrange
        Response mockResponse = mock(Response.class);
        when(mockResponse.readEntity(String.class)).thenReturn("Keycloak error");
        StatusType statusType = mock(StatusType.class);
        when(statusType.getFamily()).thenReturn(Response.Status.Family.CLIENT_ERROR);
        when(mockResponse.getStatusInfo()).thenReturn(statusType);
        
        // Create a real ClientErrorException instead of mocking it
        ClientErrorException clientError = new ClientErrorException(mockResponse);

        // Mock Keycloak client
        KeycloakSyncService spyService = spy(keycloakSyncService);
        doReturn(keycloak).when(spyService).getKeycloakClient();
        when(keycloak.realm("test-realm")).thenThrow(clientError);

        // Act & Assert
        assertThatThrownBy(() -> spyService.syncUsers())
                .isInstanceOf(ClientErrorException.class);
        verify(keycloak).close();
    }

    @Test
    void syncUsers_WhenGeneralException_ShouldThrowException() throws Exception {
        // Arrange
        // Mock Keycloak client
        KeycloakSyncService spyService = spy(keycloakSyncService);
        doReturn(keycloak).when(spyService).getKeycloakClient();
        when(keycloak.realm("test-realm")).thenThrow(new RuntimeException("Connection failed"));

        // Act & Assert
        assertThatThrownBy(() -> spyService.syncUsers())
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Connection failed");
        verify(keycloak).close();
    }

    @Test
    void syncUsers_WithEmptyKeycloakUsers_ShouldReturnEmptyList() throws Exception {
        // Arrange
        List<UserRepresentation> kcUsers = Collections.emptyList();
        
        when(roleRepository.findByName("ROLE_USER")).thenReturn(Optional.of(defaultRole));

        // Mock Keycloak client
        KeycloakSyncService spyService = spy(keycloakSyncService);
        doReturn(keycloak).when(spyService).getKeycloakClient();
        when(keycloak.realm("test-realm")).thenReturn(realmResource);
        when(realmResource.users()).thenReturn(usersResource);
        when(usersResource.list()).thenReturn(kcUsers);

        // Act
        List<User> result = spyService.syncUsers();

        // Assert
        assertThat(result).isEmpty();
        verify(keycloak).close();
    }

    // --- createUserInDbAndKeycloak ---

    @Test
    void createUserInDbAndKeycloak_ShouldCreateUserInBoth() throws Exception {
        // Arrange
        Response mockResponse = mock(Response.class);
        URI locationUri = new URI("http://localhost:8080/users/kc-user-1");
        when(mockResponse.getStatus()).thenReturn(201);
        when(mockResponse.getLocation()).thenReturn(locationUri);

        when(roleRepository.findByName("ROLE_USER")).thenReturn(Optional.of(defaultRole));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(1L);
            return user;
        });

        // Mock Keycloak client
        KeycloakSyncService spyService = spy(keycloakSyncService);
        doReturn(keycloak).when(spyService).getKeycloakClient();
        when(keycloak.realm("test-realm")).thenReturn(realmResource);
        when(realmResource.users()).thenReturn(usersResource);
        when(usersResource.create(any(UserRepresentation.class))).thenReturn(mockResponse);
        when(usersResource.get("kc-user-1")).thenReturn(userResource);
        doNothing().when(userResource).resetPassword(any());

        // Act
        spyService.createUserInDbAndKeycloak("newuser", "new@example.com", "New User", "password123", "active");

        // Assert
        verify(userRepository, times(2)).save(any(User.class));
        verify(usersResource).create(any(UserRepresentation.class));
        verify(userResource).resetPassword(any());
        verify(keycloak).close();
    }

    @Test
    void createUserInDbAndKeycloak_WithFullName_ShouldSplitName() throws Exception {
        // Arrange
        Response mockResponse = mock(Response.class);
        URI locationUri = new URI("http://localhost:8080/users/kc-user-1");
        when(mockResponse.getStatus()).thenReturn(201);
        when(mockResponse.getLocation()).thenReturn(locationUri);

        when(roleRepository.findByName("ROLE_USER")).thenReturn(Optional.of(defaultRole));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(1L);
            return user;
        });

        // Mock Keycloak client
        KeycloakSyncService spyService = spy(keycloakSyncService);
        doReturn(keycloak).when(spyService).getKeycloakClient();
        when(keycloak.realm("test-realm")).thenReturn(realmResource);
        when(realmResource.users()).thenReturn(usersResource);
        when(usersResource.create(any(UserRepresentation.class))).thenReturn(mockResponse);
        when(usersResource.get("kc-user-1")).thenReturn(userResource);
        doNothing().when(userResource).resetPassword(any());

        // Act
        spyService.createUserInDbAndKeycloak("newuser", "new@example.com", "John Doe Smith", "password123", "active");

        // Assert
        verify(usersResource).create(argThat(userRep -> {
            assertThat(userRep.getFirstName()).isEqualTo("John");
            assertThat(userRep.getLastName()).isEqualTo("Doe Smith");
            return true;
        }));
    }

    @Test
    void createUserInDbAndKeycloak_WithNullFullName_ShouldHandleGracefully() throws Exception {
        // Arrange
        Response mockResponse = mock(Response.class);
        URI locationUri = new URI("http://localhost:8080/users/kc-user-1");
        when(mockResponse.getStatus()).thenReturn(201);
        when(mockResponse.getLocation()).thenReturn(locationUri);

        when(roleRepository.findByName("ROLE_USER")).thenReturn(Optional.of(defaultRole));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(1L);
            return user;
        });

        // Mock Keycloak client
        KeycloakSyncService spyService = spy(keycloakSyncService);
        doReturn(keycloak).when(spyService).getKeycloakClient();
        when(keycloak.realm("test-realm")).thenReturn(realmResource);
        when(realmResource.users()).thenReturn(usersResource);
        when(usersResource.create(any(UserRepresentation.class))).thenReturn(mockResponse);
        when(usersResource.get("kc-user-1")).thenReturn(userResource);
        doNothing().when(userResource).resetPassword(any());

        // Act
        spyService.createUserInDbAndKeycloak("newuser", "new@example.com", null, "password123", "active");

        // Assert
        verify(usersResource).create(argThat(userRep -> {
            assertThat(userRep.getFirstName()).isEqualTo("");
            assertThat(userRep.getLastName()).isEqualTo("");
            return true;
        }));
    }

    @Test
    void createUserInDbAndKeycloak_WithInactiveStatus_ShouldSetEnabledFalse() throws Exception {
        // Arrange
        Response mockResponse = mock(Response.class);
        URI locationUri = new URI("http://localhost:8080/users/kc-user-1");
        when(mockResponse.getStatus()).thenReturn(201);
        when(mockResponse.getLocation()).thenReturn(locationUri);

        when(roleRepository.findByName("ROLE_USER")).thenReturn(Optional.of(defaultRole));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(1L);
            return user;
        });

        // Mock Keycloak client
        KeycloakSyncService spyService = spy(keycloakSyncService);
        doReturn(keycloak).when(spyService).getKeycloakClient();
        when(keycloak.realm("test-realm")).thenReturn(realmResource);
        when(realmResource.users()).thenReturn(usersResource);
        when(usersResource.create(any(UserRepresentation.class))).thenReturn(mockResponse);
        when(usersResource.get("kc-user-1")).thenReturn(userResource);
        doNothing().when(userResource).resetPassword(any());

        // Act
        spyService.createUserInDbAndKeycloak("newuser", "new@example.com", "New User", "password123", "inactive");

        // Assert
        verify(usersResource).create(argThat(userRep -> {
            assertThat(userRep.isEnabled()).isFalse();
            return true;
        }));
    }

    @Test
    void createUserInDbAndKeycloak_WhenKeycloakCreationFails_ShouldRollback() throws Exception {
        // Arrange
        Response mockResponse = mock(Response.class);
        when(mockResponse.getStatus()).thenReturn(400); // Bad request

        when(roleRepository.findByName("ROLE_USER")).thenReturn(Optional.of(defaultRole));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(1L);
            return user;
        });
        when(userRepository.findByUsername("newuser")).thenReturn(Optional.of(existingUser));
        doNothing().when(userRepository).delete(existingUser);

        // Mock Keycloak client
        KeycloakSyncService spyService = spy(keycloakSyncService);
        doReturn(keycloak).when(spyService).getKeycloakClient();
        when(keycloak.realm("test-realm")).thenReturn(realmResource);
        when(realmResource.users()).thenReturn(usersResource);
        when(usersResource.create(any(UserRepresentation.class))).thenReturn(mockResponse);

        // Act & Assert
        assertThatThrownBy(() -> spyService.createUserInDbAndKeycloak("newuser", "new@example.com", "New User", "password123", "active"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to create user in Keycloak");
        verify(userRepository).delete(existingUser);
        verify(keycloak).close();
    }

    @Test
    void createUserInDbAndKeycloak_WhenClientErrorException_ShouldRollback() throws Exception {
        // Arrange
        Response mockResponse = mock(Response.class);
        when(mockResponse.readEntity(String.class)).thenReturn("Keycloak error");
        StatusType statusType = mock(StatusType.class);
        when(statusType.getFamily()).thenReturn(Response.Status.Family.CLIENT_ERROR);
        when(mockResponse.getStatusInfo()).thenReturn(statusType);
        
        // Create a real ClientErrorException instead of mocking it
        ClientErrorException clientError = new ClientErrorException(mockResponse);

        when(roleRepository.findByName("ROLE_USER")).thenReturn(Optional.of(defaultRole));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(1L);
            return user;
        });
        when(userRepository.findByUsername("newuser")).thenReturn(Optional.of(existingUser));
        doNothing().when(userRepository).delete(existingUser);

        // Mock Keycloak client
        KeycloakSyncService spyService = spy(keycloakSyncService);
        doReturn(keycloak).when(spyService).getKeycloakClient();
        when(keycloak.realm("test-realm")).thenReturn(realmResource);
        when(realmResource.users()).thenReturn(usersResource);
        when(usersResource.create(any(UserRepresentation.class))).thenThrow(clientError);

        // Act & Assert
        assertThatThrownBy(() -> spyService.createUserInDbAndKeycloak("newuser", "new@example.com", "New User", "password123", "active"))
                .isInstanceOf(ClientErrorException.class);
        verify(userRepository).delete(existingUser);
        verify(keycloak).close();
    }

    @Test
    void createUserInDbAndKeycloak_WhenRoleNotFound_ShouldThrowException() {
        // Arrange
        when(roleRepository.findByName("ROLE_USER")).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> keycloakSyncService.createUserInDbAndKeycloak("newuser", "new@example.com", "New User", "password123", "active"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Không tìm thấy Role trong DB");
    }

    // --- updateUser ---

    @Test
    void updateUser_ShouldUpdateInBothKeycloakAndDb() throws Exception {
        // Arrange
        updateUserDTO.setUsername("updateduser");
        updateUserDTO.setEmail("updated@example.com");
        updateUserDTO.setFirstName("Updated");
        updateUserDTO.setLastName("Name");
        updateUserDTO.setStatus("active");

        UserRepresentation userRep = new UserRepresentation();
        userRep.setId("kc-user-1");
        userRep.setUsername("existinguser");
        userRep.setEmail("existing@example.com");

        when(userRepository.findByKeycloakId("kc-user-1")).thenReturn(Optional.of(existingUser));
        when(userRepository.save(any(User.class))).thenReturn(existingUser);

        // Mock Keycloak client
        KeycloakSyncService spyService = spy(keycloakSyncService);
        doReturn(keycloak).when(spyService).getKeycloakClient();
        when(keycloak.realm("test-realm")).thenReturn(realmResource);
        when(realmResource.users()).thenReturn(usersResource);
        when(usersResource.get("kc-user-1")).thenReturn(userResource);
        when(userResource.toRepresentation()).thenReturn(userRep);
        doNothing().when(userResource).update(any(UserRepresentation.class));

        // Act
        spyService.updateUser("kc-user-1", updateUserDTO);

        // Assert
        verify(userResource).update(any(UserRepresentation.class));
        verify(userRepository).save(existingUser);
        assertThat(existingUser.getUsername()).isEqualTo("updateduser");
        assertThat(existingUser.getEmail()).isEqualTo("updated@example.com");
        assertThat(existingUser.getFirstName()).isEqualTo("Updated");
        assertThat(existingUser.getLastName()).isEqualTo("Name");
        verify(keycloak).close();
    }

    @Test
    void updateUser_WithPartialUpdates_ShouldUpdateOnlyProvidedFields() throws Exception {
        // Arrange
        updateUserDTO.setUsername("updateduser");
        // Other fields are null

        UserRepresentation userRep = new UserRepresentation();
        userRep.setId("kc-user-1");
        userRep.setUsername("existinguser");

        when(userRepository.findByKeycloakId("kc-user-1")).thenReturn(Optional.of(existingUser));
        when(userRepository.save(any(User.class))).thenReturn(existingUser);

        // Mock Keycloak client
        KeycloakSyncService spyService = spy(keycloakSyncService);
        doReturn(keycloak).when(spyService).getKeycloakClient();
        when(keycloak.realm("test-realm")).thenReturn(realmResource);
        when(realmResource.users()).thenReturn(usersResource);
        when(usersResource.get("kc-user-1")).thenReturn(userResource);
        when(userResource.toRepresentation()).thenReturn(userRep);
        doNothing().when(userResource).update(any(UserRepresentation.class));

        // Act
        spyService.updateUser("kc-user-1", updateUserDTO);

        // Assert
        assertThat(existingUser.getUsername()).isEqualTo("updateduser");
        assertThat(existingUser.getEmail()).isEqualTo("existing@example.com"); // Unchanged
        verify(userRepository).save(existingUser);
    }

    @Test
    void updateUser_WithEmptyStrings_ShouldNotUpdate() throws Exception {
        // Arrange
        updateUserDTO.setUsername("");
        updateUserDTO.setEmail("");

        UserRepresentation userRep = new UserRepresentation();
        userRep.setId("kc-user-1");
        userRep.setUsername("existinguser");

        when(userRepository.findByKeycloakId("kc-user-1")).thenReturn(Optional.of(existingUser));
        when(userRepository.save(any(User.class))).thenReturn(existingUser);

        // Mock Keycloak client
        KeycloakSyncService spyService = spy(keycloakSyncService);
        doReturn(keycloak).when(spyService).getKeycloakClient();
        when(keycloak.realm("test-realm")).thenReturn(realmResource);
        when(realmResource.users()).thenReturn(usersResource);
        when(usersResource.get("kc-user-1")).thenReturn(userResource);
        when(userResource.toRepresentation()).thenReturn(userRep);
        doNothing().when(userResource).update(any(UserRepresentation.class));

        // Act
        spyService.updateUser("kc-user-1", updateUserDTO);

        // Assert
        assertThat(existingUser.getUsername()).isEqualTo("existinguser"); // Unchanged
        verify(userRepository).save(existingUser);
    }

    @Test
    void updateUser_WhenUserNotFoundInDb_ShouldThrowException() throws Exception {
        // Arrange
        updateUserDTO.setUsername("updateduser");

        UserRepresentation userRep = new UserRepresentation();
        userRep.setId("kc-user-1");

        when(userRepository.findByKeycloakId("kc-user-1")).thenReturn(Optional.empty());

        // Mock Keycloak client
        KeycloakSyncService spyService = spy(keycloakSyncService);
        doReturn(keycloak).when(spyService).getKeycloakClient();
        when(keycloak.realm("test-realm")).thenReturn(realmResource);
        when(realmResource.users()).thenReturn(usersResource);
        when(usersResource.get("kc-user-1")).thenReturn(userResource);
        when(userResource.toRepresentation()).thenReturn(userRep);
        doNothing().when(userResource).update(any(UserRepresentation.class));

        // Act & Assert
        assertThatThrownBy(() -> spyService.updateUser("kc-user-1", updateUserDTO))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("User not found in database with keycloakId: kc-user-1");
        verify(keycloak).close();
    }

    @Test
    void updateUser_WhenClientErrorException_ShouldThrowException() throws Exception {
        // Arrange
        updateUserDTO.setUsername("updateduser");

        Response mockResponse = mock(Response.class);
        when(mockResponse.readEntity(String.class)).thenReturn("Keycloak error");
        StatusType statusType = mock(StatusType.class);
        when(statusType.getFamily()).thenReturn(Response.Status.Family.CLIENT_ERROR);
        when(mockResponse.getStatusInfo()).thenReturn(statusType);
        
        // Create a real ClientErrorException instead of mocking it
        ClientErrorException clientError = new ClientErrorException(mockResponse);

        // Mock Keycloak client
        KeycloakSyncService spyService = spy(keycloakSyncService);
        doReturn(keycloak).when(spyService).getKeycloakClient();
        when(keycloak.realm("test-realm")).thenReturn(realmResource);
        when(realmResource.users()).thenReturn(usersResource);
        when(usersResource.get("kc-user-1")).thenReturn(userResource);
        when(userResource.toRepresentation()).thenThrow(clientError);

        // Act & Assert
        assertThatThrownBy(() -> spyService.updateUser("kc-user-1", updateUserDTO))
                .isInstanceOf(ClientErrorException.class);
        verify(keycloak).close();
    }

    @Test
    void updateUser_WithInactiveStatus_ShouldSetEnabledFalse() throws Exception {
        // Arrange
        updateUserDTO.setStatus("inactive");

        UserRepresentation userRep = new UserRepresentation();
        userRep.setId("kc-user-1");
        userRep.setEnabled(true);

        when(userRepository.findByKeycloakId("kc-user-1")).thenReturn(Optional.of(existingUser));
        when(userRepository.save(any(User.class))).thenReturn(existingUser);

        // Mock Keycloak client
        KeycloakSyncService spyService = spy(keycloakSyncService);
        doReturn(keycloak).when(spyService).getKeycloakClient();
        when(keycloak.realm("test-realm")).thenReturn(realmResource);
        when(realmResource.users()).thenReturn(usersResource);
        when(usersResource.get("kc-user-1")).thenReturn(userResource);
        when(userResource.toRepresentation()).thenReturn(userRep);
        doNothing().when(userResource).update(any(UserRepresentation.class));

        // Act
        spyService.updateUser("kc-user-1", updateUserDTO);

        // Assert
        verify(userResource).update(argThat(rep -> !rep.isEnabled()));
    }

    // --- logoutUserFromKeycloak ---

    @Test
    void logoutUserFromKeycloak_ShouldLogoutUser() throws Exception {
        // Arrange
        // Mock Keycloak client
        KeycloakSyncService spyService = spy(keycloakSyncService);
        doReturn(keycloak).when(spyService).getKeycloakClient();
        when(keycloak.realm("test-realm")).thenReturn(realmResource);
        when(realmResource.users()).thenReturn(usersResource);
        when(usersResource.get("kc-user-1")).thenReturn(userResource);
        doNothing().when(userResource).logout();

        // Act
        spyService.logoutUserFromKeycloak("kc-user-1");

        // Assert
        verify(userResource).logout();
        verify(keycloak).close();
    }

    @Test
    void logoutUserFromKeycloak_WhenException_ShouldThrowRuntimeException() throws Exception {
        // Arrange
        // Mock Keycloak client
        KeycloakSyncService spyService = spy(keycloakSyncService);
        doReturn(keycloak).when(spyService).getKeycloakClient();
        when(keycloak.realm("test-realm")).thenReturn(realmResource);
        when(realmResource.users()).thenReturn(usersResource);
        when(usersResource.get("kc-user-1")).thenReturn(userResource);
        doThrow(new RuntimeException("Logout failed")).when(userResource).logout();

        // Act & Assert
        assertThatThrownBy(() -> spyService.logoutUserFromKeycloak("kc-user-1"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to logout user kc-user-1");
        verify(keycloak).close();
    }
}
