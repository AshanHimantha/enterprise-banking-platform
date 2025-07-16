package auth.service.impl;


import auth.service.UserService;
import dto.ProfileUpdateDTO;
import entity.User;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.NoResultException;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Stateless
public class UserServiceImpl implements UserService {

    @PersistenceContext(unitName = "bankingPU")
    private EntityManager em;

    // Method to get the webapp's real path for avatar uploads (similar to KYC service)
    private String getWebappAvatarDirectory() {

        String webappPath = System.getProperty("com.sun.aas.instanceRoot");
        if (webappPath != null) {
            // Payara/GlassFish specific path
            return webappPath + "/applications/banking-ear/assets/avatars/";
        } else {
            // Fallback to a local directory
            return "C:\\banking_uploads\\avatars\\";
        }
    }

    @Override
    public void updateUserProfile(String username, ProfileUpdateDTO profileUpdateDTO) {
        // Find the user entity
        TypedQuery<User> query = em.createQuery("SELECT u FROM User u WHERE u.username = :username", User.class);
        query.setParameter("username", username);

        User user = query.getSingleResult(); // Throws NoResultException if user not found, which is fine

        // Update the fields if new values are provided in the DTO
        if (profileUpdateDTO.getEmail() != null && !profileUpdateDTO.getEmail().isEmpty()) {
            // You might want to add validation to ensure the new email isn't already taken
            user.setEmail(profileUpdateDTO.getEmail());
            // When email is changed, it should be marked as unverified again.
            user.setEmailVerified(false);
            // You would also trigger a new verification email here.
        }

        if (profileUpdateDTO.getPhoneNumber() != null && !profileUpdateDTO.getPhoneNumber().isEmpty()) {
            // Add validation for new phone number if needed
            user.setPhoneNumber(profileUpdateDTO.getPhoneNumber());
        }

        if (profileUpdateDTO.getProfilePictureUrl() != null) {
            user.setProfilePictureUrl(profileUpdateDTO.getProfilePictureUrl());
        }

        // Use merge to save the changes to the database
        em.merge(user);
    }

    @Override
    public String uploadProfilePicture(String username, InputStream avatarStream, String fileName) {
        try {
            // Find the user
            TypedQuery<User> query = em.createQuery("SELECT u FROM User u WHERE u.username = :username", User.class);
            query.setParameter("username", username);
            User user = query.getSingleResult();

            // Delete old avatar if exists
            if (user.getProfilePictureUrl() != null) {
                deleteExistingAvatar(user.getProfilePictureUrl());
            }

            // Save the uploaded file using the same pattern as KYC service
            String savedAvatarPath = saveAvatarFile(avatarStream, fileName, username);

            // Update user's profile picture URL (store relative path for web access)
            String avatarUrl =   Paths.get(savedAvatarPath).getFileName().toString();
            user.setProfilePictureUrl(avatarUrl);
            em.merge(user);

            return avatarUrl;

        } catch (NoResultException e) {
            throw new IllegalArgumentException("User not found: " + username);
        } catch (Exception e) {
            throw new RuntimeException("Failed to upload avatar", e);
        }
    }

    private String saveAvatarFile(InputStream inputStream, String originalFileName, String username) throws Exception {
        // Create a unique filename to avoid conflicts (same pattern as KYC service)
        String extension = originalFileName.substring(originalFileName.lastIndexOf("."));
        String uniqueFileName = username + "_avatar_" + UUID.randomUUID().toString() + extension;

        // Ensure the upload directory exists
        File uploadDir = new File(getWebappAvatarDirectory());
        if (!uploadDir.exists()) {
            uploadDir.mkdirs();
        }

        Path destination = Paths.get(getWebappAvatarDirectory() + uniqueFileName);
        Files.copy(inputStream, destination, StandardCopyOption.REPLACE_EXISTING);

        return destination.toString(); // Return the full path
    }

    @Override
    public String getProfilePictureUrl(String username) {
        try {
            TypedQuery<User> query = em.createQuery("SELECT u FROM User u WHERE u.username = :username", User.class);
            query.setParameter("username", username);
            User user = query.getSingleResult();
            return user.getProfilePictureUrl();
        } catch (NoResultException e) {
            return null;
        }
    }

    @Override
    public void deleteProfilePicture(String username) {
        try {
            TypedQuery<User> query = em.createQuery("SELECT u FROM User u WHERE u.username = :username", User.class);
            query.setParameter("username", username);
            User user = query.getSingleResult();

            if (user.getProfilePictureUrl() != null) {
                // Delete the physical file
                deleteExistingAvatar(user.getProfilePictureUrl());

                // Clear the URL from database
                user.setProfilePictureUrl(null);
                em.merge(user);
            }
        } catch (NoResultException e) {
            throw new IllegalArgumentException("User not found: " + username);
        }
    }

    @Override
    public User getUserProfile(String username) {
        try {
            TypedQuery<User> query = em.createQuery("SELECT u FROM User u WHERE u.username = :username", User.class);
            query.setParameter("username", username);
            return query.getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }

    // Helper method to delete existing avatar file
    private void deleteExistingAvatar(String avatarUrl) {
        try {
            String fileName = avatarUrl.substring(avatarUrl.lastIndexOf('/') + 1);
            Path avatarPath = Paths.get(getWebappAvatarDirectory(), fileName);
            Files.deleteIfExists(avatarPath);
        } catch (Exception e) {
            // Log but don't throw - this is cleanup, not critical
            System.err.println("Failed to delete existing avatar file: " + e.getMessage());
        }
    }
}