package auth.service;

import dto.ProfileUpdateDTO;
import entity.User;
import jakarta.ejb.Local;
import java.io.InputStream;

@Local
public interface UserService {
    /**
     * Updates the profile for the given user.
     * @param username The username of the user to update (from the JWT).
     * @param profileUpdateDTO The DTO containing the new data.
     */
    void updateUserProfile(String username, ProfileUpdateDTO profileUpdateDTO);

    /**
     * Uploads a profile picture for the user.
     * @param username The username of the user.
     * @param avatarStream The input stream of the avatar image.
     * @param fileName The filename of the avatar image.
     * @return The URL/path to the uploaded avatar.
     */
    String uploadProfilePicture(String username, InputStream avatarStream, String fileName);

    /**
     * Gets the profile picture URL for the user.
     * @param username The username of the user.
     * @return The URL/path to the user's avatar or null if not found.
     */
    String getProfilePictureUrl(String username);

    /**
     * Deletes the profile picture for the user.
     * @param username The username of the user.
     */
    void deleteProfilePicture(String username);

    /**
     * Gets the user profile details.
     * @param username The username of the user.
     * @return The User entity or null if not found.
     */
    User getUserProfile(String username);
}