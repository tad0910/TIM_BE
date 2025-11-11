package com.tim.appTim.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.ws.rs.ClientErrorException;
import javax.ws.rs.core.Response;

import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tim.appTim.controller.KeycloakController.UpdateUserDTO;
import com.tim.appTim.entity.User;
import com.tim.appTim.entity.Role;
import com.tim.appTim.repository.UserRepository;
import com.tim.appTim.repository.RoleRepository;


@Service("keycloakService")
public class KeycloakSyncService {

    @Value("${keycloak.server-url}")
    private String serverUrl;

    @Value("${keycloak.realm}")
    private String realm;

    @Value("${keycloak.client-id-app}")
    private String clientId;

    @Value("${keycloak.client-secret}")
    private String clientSecret;

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;


    public KeycloakSyncService(UserRepository userRepository, RoleRepository roleRepository) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
    }

    private Keycloak getKeycloakClient() {
        return KeycloakBuilder.builder()
                .serverUrl(serverUrl.trim())
                .realm(realm.trim())
                .grantType(OAuth2Constants.CLIENT_CREDENTIALS)
                .clientId(clientId.trim())
                .clientSecret(clientSecret.trim())
                .build();
    }

    @Transactional
    @Scheduled(fixedDelay = 300_000)
    public List<User> syncUsers() {
        List<User> syncedUsers = new ArrayList<>();
        Keycloak keycloak = null;
        try {
            keycloak = getKeycloakClient();
            RealmResource realmResource = keycloak.realm(realm);
            List<UserRepresentation> kcUsers = realmResource.users().list();

            Role defaultRole = roleRepository.findByName("ROLE_USER")
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy Role 'ROLE_USER' trong DB"));

            for (UserRepresentation kcUser : kcUsers) {

                User userToSync = userRepository.findByKeycloakId(kcUser.getId())
                        .or(() -> userRepository.findByEmail(kcUser.getEmail()))
                        .orElse(null);

                if (userToSync != null) {

                    boolean needsUpdate = false;

                    if (userToSync.getKeycloakId() == null) {
                        userToSync.setKeycloakId(kcUser.getId());
                        needsUpdate = true;
                    }

                    if (kcUser.getUsername() != null && !kcUser.getUsername().equals(userToSync.getUsername())) {
                        userToSync.setUsername(kcUser.getUsername());
                        needsUpdate = true;
                    }
                    if (kcUser.getEmail() != null && !kcUser.getEmail().equals(userToSync.getEmail())) {
                        userToSync.setEmail(kcUser.getEmail());
                        needsUpdate = true;
                    }
                    if (kcUser.getFirstName() != null && !kcUser.getFirstName().equals(userToSync.getFirstName())) {
                        userToSync.setFirstName(kcUser.getFirstName());
                        needsUpdate = true;
                    }
                    if (kcUser.getLastName() != null && !kcUser.getLastName().equals(userToSync.getLastName())) {
                        userToSync.setLastName(kcUser.getLastName());
                        needsUpdate = true;
                    }

                    if (needsUpdate) {
                        userRepository.save(userToSync);
                        System.out.println("🔄 Synced/Updated existing user in DB: " + kcUser.getUsername());
                    }

                } else {
                    User newUser = new User();
                    newUser.setKeycloakId(kcUser.getId());
                    newUser.setUsername(kcUser.getUsername());
                    newUser.setEmail(kcUser.getEmail());
                    newUser.setFirstName(kcUser.getFirstName());
                    newUser.setLastName(kcUser.getLastName());
                    newUser.setCreatedAt(LocalDateTime.now());
                    newUser.setPassword("KEYCLOAK_MANAGED");
                    newUser.setRoles(Set.of(defaultRole));

                    userRepository.save(newUser);
                    syncedUsers.add(newUser);
                    System.out.println("✅ Synced new user to DB: " + kcUser.getUsername());
                }
            }
            return syncedUsers;
        } catch (ClientErrorException e) {
            System.err.println("❌ Keycloak sync failed: " + e.getResponse().readEntity(String.class));
            throw e;
        } catch (Exception e) {
            System.err.println("❌ Keycloak sync failed: " + e.getMessage());
            throw e;
        } finally {
            if (keycloak != null) {
                keycloak.close();
            }
        }
    }

    public void createUserInDbAndKeycloak(String username, String email, String fullName, String password, String status) {
        Keycloak keycloak = null;
        try {
            User newUser = new User();
            newUser.setUsername(username);
            newUser.setEmail(email);
            newUser.setCreatedAt(LocalDateTime.now());
            newUser.setPassword("KEYCLOAK_MANAGED");

            Role defaultRole = roleRepository.findByName("ROLE_USER")
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy Role trong DB"));
            newUser.setRoles(Set.of(defaultRole));

            userRepository.save(newUser);
            System.out.println("✅ Created user in DB: " + username);

            keycloak = getKeycloakClient();
            RealmResource realmResource = keycloak.realm(realm);

            UserRepresentation userRepresentation = new UserRepresentation();
            userRepresentation.setUsername(username);
            userRepresentation.setEmail(email);
            userRepresentation.setFirstName(fullName != null ? fullName.split(" ")[0] : "");
            userRepresentation.setLastName(fullName != null ?
                    String.join(" ", java.util.Arrays.copyOfRange(fullName.split(" "), 1, fullName.split(" ").length)) : "");
            userRepresentation.setEnabled("active".equalsIgnoreCase(status));
            userRepresentation.setEmailVerified(false);

            Response response = realmResource.users().create(userRepresentation);
            if (response.getStatus() >= 200 && response.getStatus() < 300) {
                String userId = response.getLocation().getPath().replaceAll(".*/([^/]+)$", "$1");
                System.out.println("✅ Created user in Keycloak: " + username);

                CredentialRepresentation credential = new CredentialRepresentation();
                credential.setType(CredentialRepresentation.PASSWORD);
                credential.setValue(password);
                credential.setTemporary(false);

                UserResource userResource = realmResource.users().get(userId);
                userResource.resetPassword(credential);
                System.out.println("✅ Password set for user: " + username);

                newUser.setKeycloakId(userId);
                userRepository.save(newUser);
            } else {
                throw new RuntimeException("Failed to create user in Keycloak: " + response.getStatusInfo());
            }
        } catch (ClientErrorException e) {
            System.err.println("❌ Failed to create user in Keycloak: " + e.getResponse().readEntity(String.class));
            userRepository.findByUsername(username).ifPresent(userRepository::delete);
            throw e;
        } catch (Exception e) {
            System.err.println("❌ Failed to create user: " + e.getMessage());
            userRepository.findByUsername(username).ifPresent(userRepository::delete);
            throw e;
        } finally {
            if (keycloak != null) {
                keycloak.close();
            }
        }
    }

    @Transactional
    public void updateUser(String userId, UpdateUserDTO updateUserDTO) {
        Keycloak keycloak = null;
        try {
            keycloak = getKeycloakClient();

            RealmResource realmResource = keycloak.realm(realm);
            UserResource userResource = realmResource.users().get(userId);
            UserRepresentation userRepresentation = userResource.toRepresentation();

            if (updateUserDTO.getUsername() != null && !updateUserDTO.getUsername().isEmpty()) {
                userRepresentation.setUsername(updateUserDTO.getUsername());
            }
            if (updateUserDTO.getEmail() != null && !updateUserDTO.getEmail().isEmpty()) {
                userRepresentation.setEmail(updateUserDTO.getEmail());
            }
            if (updateUserDTO.getFirstName() != null && !updateUserDTO.getFirstName().isEmpty()) {
                userRepresentation.setFirstName(updateUserDTO.getFirstName());
            }
            if (updateUserDTO.getLastName() != null && !updateUserDTO.getLastName().isEmpty()) {
                userRepresentation.setLastName(updateUserDTO.getLastName());
            }
            if (updateUserDTO.getStatus() != null && !updateUserDTO.getStatus().isEmpty()) {
                userRepresentation.setEnabled("active".equalsIgnoreCase(updateUserDTO.getStatus()));
            }

            userResource.update(userRepresentation);
            System.out.println("✅ Updated user in Keycloak: " + userId);

            User dbUser = userRepository.findByKeycloakId(userId)
                    .orElseThrow(() -> new RuntimeException("User not found in database with keycloakId: " + userId));
            if (updateUserDTO.getUsername() != null && !updateUserDTO.getUsername().isEmpty()) {
                dbUser.setUsername(updateUserDTO.getUsername());
            }
            if (updateUserDTO.getEmail() != null && !updateUserDTO.getEmail().isEmpty()) {
                dbUser.setEmail(updateUserDTO.getEmail());
            }
            if (updateUserDTO.getFirstName() != null && !updateUserDTO.getFirstName().isEmpty()) {
                dbUser.setFirstName(updateUserDTO.getFirstName());
            }
            if (updateUserDTO.getLastName() != null && !updateUserDTO.getLastName().isEmpty()) {
                dbUser.setLastName(updateUserDTO.getLastName());
            }
            userRepository.save(dbUser);
            System.out.println("✅ Updated user in DB: " + userId);
        } catch (ClientErrorException e) {
            System.err.println("❌ Failed to update user in Keycloak: " + e.getResponse().readEntity(String.class));
            throw e;
        } catch (Exception e) {
            System.err.println("❌ Failed to update user: " + e.getMessage());
            throw e;
        } finally {
            if (keycloak != null) {
                keycloak.close();
            }
        }
    }
    public void logoutUserFromKeycloak(String userId) {
        Keycloak keycloak = null;
        try {
            keycloak = getKeycloakClient();
            UserResource userResource = keycloak.realm(realm).users().get(userId);
            userResource.logout();
            System.out.println("✅ Successfully logged out user: " + userId);
        } catch (Exception e) {
            System.err.println("❌ Failed to logout user " + userId + ": " + e.getMessage());
            throw new RuntimeException("Failed to logout user " + userId, e);
        } finally {
            if (keycloak != null) {
                keycloak.close();
            }
        }
    }


}