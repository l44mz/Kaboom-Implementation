package com.example.kaboom.services;


import com.example.kaboom.dtos.*;
import com.example.kaboom.entities.User;
import com.example.kaboom.mappers.UserMapper;
import com.example.kaboom.repositories.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;
import java.util.Optional;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
class UserServicesTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtServices jwtServices;
    @Mock private UserMapper userMapper;

    @InjectMocks private UserServices userServices;

    // --------------------

    private RegisterDto registerDto(String name, String email, String password) {
        RegisterDto dto = mock(RegisterDto.class);
        lenient().when(dto.getName()).thenReturn(name);
        lenient().when(dto.getEmail()).thenReturn(email);
        lenient().when(dto.getPassword()).thenReturn(password);
        return dto;
    }

    private LoginDto loginDto(String email, String password) {
        LoginDto dto = mock(LoginDto.class);
        lenient().when(dto.getEmail()).thenReturn(email);
        lenient().when(dto.getPassword()).thenReturn(password);
        return dto;
    }

    private UserDto userDto(String name, String email) {
        UserDto dto = mock(UserDto.class);
        lenient().when(dto.getName()).thenReturn(name);
        lenient().when(dto.getEmail()).thenReturn(email);
        return dto;
    }

    private RefreshDto refreshDto(String token) {
        RefreshDto dto = mock(RefreshDto.class);
        lenient().when(dto.getRefreshToken()).thenReturn(token);
        return dto;
    }

    private User userWithId(UUID id) {
        User user = mock(User.class);
        lenient().when(user.getId()).thenReturn(id);
        return user;
    }

    private void assertStatus(HttpStatus expected, ResponseStatusException ex) {
        assertEquals(expected, ex.getStatusCode());
    }

    // --------------------

    @Test
    void register_validInput_savesUserWithNormalizedFieldsAndHashedPassword() {
        RegisterDto dto = registerDto("  john  ", "  John_12@gmail.com ", "password123");
        when(userRepository.findByEmail("john_12@gmail.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("password123")).thenReturn("hashed");
        when(userMapper.toDto(any(User.class))).thenReturn(mock(UserDto.class));

        RegisterResponseDto result = userServices.register(dto);

        assertNotNull(result);
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User saved = captor.getValue();
        assertEquals("john", saved.getName());                // trimmed
        assertEquals("john_12@gmail.com", saved.getEmail());   // trimmed + lowercased
        assertEquals("hashed", saved.getPasswordHash());     // never the raw password
    }

    @ParameterizedTest
    @CsvSource({
            "'',          a@b.com,  password123",   // blank name
            "john,         not-email, password123",  // bad email
            "john,         a@b.com,  short"          // password < 8 chars
    })
    void register_invalidInput_throwsBadRequest(String name, String email, String password) {
        RegisterDto dto = registerDto(name, email, password);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> userServices.register(dto));

        assertStatus(HttpStatus.BAD_REQUEST, ex);
        verify(userRepository, never()).save(any());
    }

    @Test
    void register_nullName_throwsBadRequest() {
        RegisterDto dto = registerDto(null, "a@b.com", "password123");

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> userServices.register(dto));

        assertStatus(HttpStatus.BAD_REQUEST, ex);
    }

    @Test
    void register_emailAlreadyExists_throwsConflict() {
        RegisterDto dto = registerDto("john", "taken@example.com", "password123");
        when(userRepository.findByEmail("taken@example.com"))
                .thenReturn(Optional.of(mock(User.class)));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> userServices.register(dto));

        assertStatus(HttpStatus.CONFLICT, ex);
        verify(userRepository, never()).save(any());
    }

    // ---------- login ----------

    @Test
    void login_correctCredentials_returnsTokens() {
        User user = mock(User.class);
        when(user.getEmail()).thenReturn("john_12@gmail.com");
        when(user.getPasswordHash()).thenReturn("hashed");
        when(userRepository.findByEmail("john_12@gmail.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "hashed")).thenReturn(true);
        when(jwtServices.generateToken(any(), any())).thenReturn("access-token");
        when(jwtServices.generateRefreshToken("john_12@gmail.com")).thenReturn("refresh-token");
        when(userMapper.toDto(user)).thenReturn(mock(UserDto.class));

        LoginResponseDto result = userServices.login(loginDto("  john_12@gmail.com ", "password123"));

        assertNotNull(result);
        verify(jwtServices).generateToken(any(), any());
        verify(jwtServices).generateRefreshToken("john_12@gmail.com");
    }

    @Test
    void login_wrongPassword_throwsUnauthorized() {
        User user = mock(User.class);
        when(user.getPasswordHash()).thenReturn("hashed");
        when(userRepository.findByEmail("john_12@gmail.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "hashed")).thenReturn(false);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> userServices.login(loginDto("john_12@gmail.com", "wrong")));

        assertStatus(HttpStatus.UNAUTHORIZED, ex);
        verifyNoInteractions(jwtServices);
    }

    @Test
    void login_unknownEmail_throwsUnauthorized() {
        when(userRepository.findByEmail("nobody@example.com")).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> userServices.login(loginDto("nobody@example.com", "password123")));

        assertStatus(HttpStatus.UNAUTHORIZED, ex);
    }

    @Test
    void login_nullPassword_throwsUnauthorizedWithoutCheckingHash() {
        User user = mock(User.class);
        when(userRepository.findByEmail("john_12@gmail.com")).thenReturn(Optional.of(user));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> userServices.login(loginDto("john_12@gmail.com", null)));

        assertStatus(HttpStatus.UNAUTHORIZED, ex);
        verifyNoInteractions(passwordEncoder);
    }

    // ---------- getUserById ----------

    @Test
    void getUserById_found_returnsUser() {
        UUID id = UUID.randomUUID();
        User user = userWithId(id);
        when(userRepository.findById(id)).thenReturn(Optional.of(user));

        assertSame(user, userServices.getUserById(id));
    }

    @Test
    void getUserById_missing_throwsNotFound() {
        UUID id = UUID.randomUUID();
        when(userRepository.findById(id)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> userServices.getUserById(id));

        assertStatus(HttpStatus.NOT_FOUND, ex);
    }

    // ---------- updateUser ----------

    @Test
    void updateUser_valid_updatesNameAndEmail() {
        UUID id = UUID.randomUUID();
        User user = userWithId(id);
        when(userRepository.findById(id)).thenReturn(Optional.of(user));
        when(userRepository.findByEmail("new@example.com")).thenReturn(Optional.empty());
        when(userMapper.toDto(user)).thenReturn(mock(UserDto.class));

        UserDto result = userServices.updateUser(id, userDto("  New Name ", " NEW@example.com "));

        assertNotNull(result);
        verify(user).setName("New Name");
        verify(user).setEmail("new@example.com");
        verify(userRepository).save(user);
    }

    @Test
    void updateUser_keepingOwnEmail_isAllowed() {
        UUID id = UUID.randomUUID();
        User user = userWithId(id);
        when(userRepository.findById(id)).thenReturn(Optional.of(user));
        when(userRepository.findByEmail("me@example.com")).thenReturn(Optional.of(user)); // same user
        when(userMapper.toDto(user)).thenReturn(mock(UserDto.class));

        assertDoesNotThrow(() -> userServices.updateUser(id, userDto("Me", "me@example.com")));
        verify(userRepository).save(user);
    }

    @Test
    void updateUser_userMissing_throwsNotFound() {
        UUID id = UUID.randomUUID();
        when(userRepository.findById(id)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> userServices.updateUser(id, userDto("Me", "me@example.com")));

        assertStatus(HttpStatus.NOT_FOUND, ex);
    }

    @Test
    void updateUser_blankName_throwsBadRequest() {
        UUID id = UUID.randomUUID();
        User existing = userWithId(id);
        when(userRepository.findById(id)).thenReturn(Optional.of(existing));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> userServices.updateUser(id, userDto("   ", "me@example.com")));

        assertStatus(HttpStatus.BAD_REQUEST, ex);
        verify(userRepository, never()).save(any());
    }

    @Test
    void updateUser_invalidEmail_throwsBadRequest() {
        UUID id = UUID.randomUUID();
        User existing = userWithId(id);
        when(userRepository.findById(id)).thenReturn(Optional.of(existing));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> userServices.updateUser(id, userDto("Me", "not-an-email")));

        assertStatus(HttpStatus.BAD_REQUEST, ex);
    }

    @Test
    void updateUser_emailBelongsToAnotherUser_throwsConflict() {
        UUID id = UUID.randomUUID();
        User existing = userWithId(id);
        User other = userWithId(UUID.randomUUID());
        when(userRepository.findById(id)).thenReturn(Optional.of(existing));
        when(userRepository.findByEmail("other@example.com")).thenReturn(Optional.of(other));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> userServices.updateUser(id, userDto("Me", "other@example.com")));

        assertStatus(HttpStatus.CONFLICT, ex);
        verify(userRepository, never()).save(any());
    }

    // ---------- update (boolean version) ----------

    @Test
    void update_userMissing_returnsFalse() {
        UUID id = UUID.randomUUID();
        when(userRepository.findById(id)).thenReturn(Optional.empty());

        assertFalse(userServices.update(id, "Name", "a@b.com", "password123"));
        verify(userRepository, never()).save(any());
    }

    @Test
    void update_invalidEmail_returnsFalse() {
        UUID id = UUID.randomUUID();
        User existing =  userWithId(id);
        when(userRepository.findById(id)).thenReturn(Optional.of(existing));
        assertFalse(userServices.update(id, null, "bad-email", null));
        verify(userRepository, never()).save(any());
    }

    @Test
    void update_shortPassword_returnsFalse() {
        UUID id = UUID.randomUUID();
        User update =  userWithId(id);
        when(userRepository.findById(id)).thenReturn(Optional.of(update));

        assertFalse(userServices.update(id, null, null, "short"));
        verify(userRepository, never()).save(any());
    }

    @Test
    void update_emailBelongsToAnotherUser_throwsConflict() {
        UUID id = UUID.randomUUID();
        User existing = userWithId(id);
        User other = userWithId(UUID.randomUUID());

        when(userRepository.findById(id)).thenReturn(Optional.of(existing));
        when(userRepository.findByEmail("other@example.com")).thenReturn(Optional.of(other));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> userServices.update(id, null, "other@example.com", null));

        assertStatus(HttpStatus.CONFLICT, ex);
    }

    @Test
    void update_allFieldsValid_updatesEverythingAndReturnsTrue() {
        UUID id = UUID.randomUUID();
        User user = userWithId(id);
        when(userRepository.findById(id)).thenReturn(Optional.of(user));
        when(userRepository.findByEmail("new@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("newPassword1")).thenReturn("newHash");

        boolean result = userServices.update(id, " New ", " NEW@example.com ", "newPassword1");

        assertTrue(result);
        verify(user).setName("New");
        verify(user).setEmail("new@example.com");
        verify(user).setPasswordHash("newHash");
        verify(userRepository).save(user);
    }

    @Test
    void update_onlyName_leavesEmailAndPasswordUntouched() {
        UUID id = UUID.randomUUID();
        User user = userWithId(id);
        when(userRepository.findById(id)).thenReturn(Optional.of(user));

        assertTrue(userServices.update(id, "Just Name", null, null));

        verify(user).setName("Just Name");
        verify(user, never()).setEmail(any());
        verify(user, never()).setPasswordHash(any());
        verifyNoInteractions(passwordEncoder);
    }

    // ---------- refresh ----------

    @Test
    void refresh_nullDto_throwsUnauthorized() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> userServices.refresh(null));

        assertStatus(HttpStatus.UNAUTHORIZED, ex);
    }

    @Test
    void refresh_invalidToken_throwsUnauthorized() {
        when(jwtServices.isValidRefreshToken("bad")).thenReturn(false);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> userServices.refresh(refreshDto("bad")));

        assertStatus(HttpStatus.UNAUTHORIZED, ex);
    }

    @Test
    void refresh_validTokenButUserDeleted_throwsUnauthorized() {
        when(jwtServices.isValidRefreshToken("good")).thenReturn(true);
        when(jwtServices.extractEmail("good")).thenReturn("gone@example.com");
        when(userRepository.findByEmail("gone@example.com")).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> userServices.refresh(refreshDto("good")));

        assertStatus(HttpStatus.UNAUTHORIZED, ex);
    }

    @Test
    void refresh_validToken_returnsNewAccessToken() {
        User user = mock(User.class);
        when(user.getEmail()).thenReturn("john_12@gmail.com");
        when(jwtServices.isValidRefreshToken("good")).thenReturn(true);
        when(jwtServices.extractEmail("good")).thenReturn("john_12@gmail.com");
        when(userRepository.findByEmail("john_12@gmail.com")).thenReturn(Optional.of(user));
        when(jwtServices.generateToken(any(), any())).thenReturn("new-access-token");

        RefreshResponseDto result = userServices.refresh(refreshDto("good"));

        assertNotNull(result);
        verify(jwtServices).generateToken(any(), any());
    }

    // ---------- logout -----------

    @Test
    void logout_returnsMessage() {
        MessageResponseDto result = userServices.logout(mock(LogoutDto.class));

        assertNotNull(result);
    }

    // ---------- deleteUser / delete ----------

    @Test
    void deleteUser_exists_deletes() {
        UUID id = UUID.randomUUID();
        when(userRepository.existsById(id)).thenReturn(true);

        userServices.deleteUser(id);

        verify(userRepository).deleteById(id);
    }

    @Test
    void deleteUser_missing_throwsNotFound() {
        UUID id = UUID.randomUUID();
        when(userRepository.existsById(id)).thenReturn(false);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> userServices.deleteUser(id));

        assertStatus(HttpStatus.NOT_FOUND, ex);
        verify(userRepository, never()).deleteById(any());
    }

    @Test
    void delete_exists_returnsTrueAndDeletes() {
        UUID id = UUID.randomUUID();
        when(userRepository.existsById(id)).thenReturn(true);

        assertTrue(userServices.delete(id));
        verify(userRepository).deleteById(id);
    }

    @Test
    void delete_missing_returnsFalse() {
        UUID id = UUID.randomUUID();
        when(userRepository.existsById(id)).thenReturn(false);

        assertFalse(userServices.delete(id));
        verify(userRepository, never()).deleteById(any());
    }
}