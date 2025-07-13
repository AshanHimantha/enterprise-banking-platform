package auth.service;

import dto.UserSearchResultDTO;
import jakarta.ejb.Local;
import java.util.List;

@Local
public interface SearchService {
    /**
     * Searches for users based on a single search term which can be an
     * email, username, or account number.
     * @param searchTerm The value to search for.
     * @return A list of matching user search results, limited to 5.
     */
    List<UserSearchResultDTO> searchUsers(String searchTerm);
}