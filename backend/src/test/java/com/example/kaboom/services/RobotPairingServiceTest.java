
package com.example.kaboom.services;

import com.example.kaboom.dtos.PairingCodeResponse;
import com.example.kaboom.dtos.PairingStatusResponse;
import com.example.kaboom.dtos.RefreshResponseDto;
import com.example.kaboom.entities.PairingCode;
import com.example.kaboom.entities.Robot;
import com.example.kaboom.entities.User;
import com.example.kaboom.repositories.PairingCodeRepository;
import com.example.kaboom.repositories.RobotRepository;
import com.example.kaboom.repositories.UserRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for RobotPairingService.
 */
@ExtendWith(MockitoExtension.class)
class RobotPairingServiceTest {

    @Mock private PairingCodeRepository pairingCodeRepository;
    @Mock private RobotRepository robotRepository;
    @Mock private UserRepository userRepository;
    @Mock private JwtServices jwtServices;

    @InjectMocks private RobotPairingService robotPairingService;

    // ---------- helpers ----------

    private User userWithId(UUID id) {
        User user = mock(User.class);
        lenient().when(user.getId()).thenReturn(id);
        return user;
    }

    private Robot robotWithSerial(String serialNumber, User user) {
        Robot robot = mock(Robot.class);
        lenient().when(robot.getSerialNumber()).thenReturn(serialNumber);
        lenient().when(robot.getUser()).thenReturn(user);
        return robot;
    }

    private PairingCode pairingCode(
            String serialNumber,
            String code,
            String secretHash,
            LocalDateTime expiresAt,
            boolean consumed,
            User claimedByUser,
            Robot robot) {

        PairingCode pairingCode = mock(PairingCode.class);

        lenient().when(pairingCode.getSerialNumber()).thenReturn(serialNumber);
        lenient().when(pairingCode.getCode()).thenReturn(code);
        lenient().when(pairingCode.getSecretHash()).thenReturn(secretHash);
        lenient().when(pairingCode.getExpiresAt()).thenReturn(expiresAt);
        lenient().when(pairingCode.isConsumed()).thenReturn(consumed);
        lenient().when(pairingCode.getClaimedByUser()).thenReturn(claimedByUser);
        lenient().when(pairingCode.getRobot()).thenReturn(robot);

        return pairingCode;
    }

    private void assertStatus(
            HttpStatus expected,
            ResponseStatusException exception) {
        assertEquals(expected, exception.getStatusCode());
    }

    private String sha256(String value) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");

        return HexFormat.of().formatHex(
                digest.digest(value.getBytes(StandardCharsets.UTF_8)));
    }

    // ---------- requestCode ----------

    @Test
    void requestCode_generatesAndSavesPairingCode() {
        String serialNumber = "ROBOT-001";

        when(pairingCodeRepository
                .existsByCodeAndConsumedFalseAndExpiresAtAfter(
                        anyString(), any(LocalDateTime.class)))
                .thenReturn(false);

        PairingCodeResponse result =
                robotPairingService.requestCode(serialNumber);

        assertNotNull(result);

        verify(pairingCodeRepository)
                .deleteBySerialNumberAndConsumedFalse(serialNumber);

        ArgumentCaptor<PairingCode> captor =
                ArgumentCaptor.forClass(PairingCode.class);

        verify(pairingCodeRepository).save(captor.capture());

        PairingCode saved = captor.getValue();

        assertEquals(serialNumber, saved.getSerialNumber());
        assertNotNull(saved.getCode());
        assertTrue(saved.getCode().matches("\\d{6}"));

        assertNotNull(saved.getSecretHash());
        assertEquals(64, saved.getSecretHash().length());

        assertNotNull(saved.getExpiresAt());
        assertTrue(saved.getExpiresAt().isAfter(LocalDateTime.now()));
        assertTrue(saved.getExpiresAt().isBefore(
                LocalDateTime.now().plusMinutes(11)));

        assertFalse(saved.isConsumed());
    }

    @Test
    void requestCode_deletesPreviousUnconsumedCodesBeforeCreatingNewCode() {
        String serialNumber = "ROBOT-002";

        when(pairingCodeRepository
                .existsByCodeAndConsumedFalseAndExpiresAtAfter(
                        anyString(), any(LocalDateTime.class)))
                .thenReturn(false);

        robotPairingService.requestCode(serialNumber);

        verify(pairingCodeRepository)
                .deleteBySerialNumberAndConsumedFalse(serialNumber);

        verify(pairingCodeRepository).save(any(PairingCode.class));
    }

    // ---------- claim ----------

    @Test
    void claim_validCode_createsRobotAndPairsItWithUser() {
        String email = "test@example.com";
        String code = "123456";
        String serial = "ROBOT-001";

        UUID userId = UUID.randomUUID();

        User user = userWithId(userId);

        PairingCode pairingCode = pairingCode(
                serial,
                code,
                "some-hash",
                LocalDateTime.now().plusMinutes(5),
                false,
                null,
                null
        );

        when(pairingCodeRepository
                .findByCodeAndConsumedFalseAndClaimedByUserIsNullAndExpiresAtAfter(
                        eq(code), any(LocalDateTime.class)))
                .thenReturn(Optional.of(pairingCode));

        when(userRepository.findByEmail(email))
                .thenReturn(Optional.of(user));

        when(robotRepository.findByUserId(userId))
                .thenReturn(List.of());

        when(robotRepository.findBySerialNumber(serial))
                .thenReturn(Optional.empty());

        Robot createdRobot = mock(Robot.class);

        when(robotRepository.save(any(Robot.class)))
                .thenReturn(createdRobot);

        Robot result = robotPairingService.claim(email, code);

        assertNotNull(result);
        assertSame(createdRobot, result);

        ArgumentCaptor<Robot> robotCaptor =
                ArgumentCaptor.forClass(Robot.class);

        verify(robotRepository).save(robotCaptor.capture());

        Robot savedRobot = robotCaptor.getValue();

        assertEquals(serial, savedRobot.getSerialNumber());
        assertSame(user, savedRobot.getUser());

        verify(pairingCode).setClaimedByUser(user);
        verify(pairingCode).setRobot(createdRobot);

        verify(pairingCodeRepository).save(pairingCode);
    }

    @Test
    void claim_existingRobotWithSameSerial_pairsExistingRobot() {
        String email = "test@example.com";
        String code = "123456";
        String serial = "ROBOT-001";

        UUID userId = UUID.randomUUID();

        User user = userWithId(userId);
        Robot existingRobot = robotWithSerial(serial, user);

        PairingCode pairingCode = pairingCode(
                serial,
                code,
                "some-hash",
                LocalDateTime.now().plusMinutes(5),
                false,
                null,
                null
        );

        when(pairingCodeRepository
                .findByCodeAndConsumedFalseAndClaimedByUserIsNullAndExpiresAtAfter(
                        eq(code), any(LocalDateTime.class)))
                .thenReturn(Optional.of(pairingCode));

        when(userRepository.findByEmail(email))
                .thenReturn(Optional.of(user));

        when(robotRepository.findByUserId(userId))
                .thenReturn(List.of(existingRobot));

        when(robotRepository.findBySerialNumber(serial))
                .thenReturn(Optional.of(existingRobot));

        when(robotRepository.save(existingRobot))
                .thenReturn(existingRobot);

        Robot result = robotPairingService.claim(email, code);

        assertSame(existingRobot, result);

        verify(robotRepository).findBySerialNumber(serial);
        verify(robotRepository).save(existingRobot);

        verify(existingRobot).setUser(user);
        verify(pairingCode).setClaimedByUser(user);
        verify(pairingCode).setRobot(existingRobot);
        verify(pairingCodeRepository).save(pairingCode);
    }

    @Test
    void claim_pairingCodeNotFound_throwsNotFound() {
        String email = "test@example.com";
        String code = "123456";

        when(pairingCodeRepository
                .findByCodeAndConsumedFalseAndClaimedByUserIsNullAndExpiresAtAfter(
                        eq(code), any(LocalDateTime.class)))
                .thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> robotPairingService.claim(email, code)
        );

        assertStatus(HttpStatus.NOT_FOUND, ex);

        verifyNoInteractions(userRepository);
        verifyNoInteractions(robotRepository);
    }

    @Test
    void claim_userNotFound_throwsNotFound() {
        String email = "unknown@example.com";
        String code = "123456";
        String serial = "ROBOT-001";

        PairingCode pairingCode = pairingCode(
                serial,
                code,
                "some-hash",
                LocalDateTime.now().plusMinutes(5),
                false,
                null,
                null
        );

        when(pairingCodeRepository
                .findByCodeAndConsumedFalseAndClaimedByUserIsNullAndExpiresAtAfter(
                        eq(code), any(LocalDateTime.class)))
                .thenReturn(Optional.of(pairingCode));

        when(userRepository.findByEmail(email))
                .thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> robotPairingService.claim(email, code)
        );

        assertStatus(HttpStatus.NOT_FOUND, ex);

        verifyNoInteractions(robotRepository);
        verify(pairingCodeRepository, never()).save(any());
    }

    @Test
    void claim_userAlreadyOwnsDifferentRobot_throwsConflict() {
        String email = "test@example.com";
        String code = "123456";
        String serial = "ROBOT-NEW";

        UUID userId = UUID.randomUUID();

        User user = userWithId(userId);

        Robot otherRobot = robotWithSerial("ROBOT-OLD", user);

        PairingCode pairingCode = pairingCode(
                serial,
                code,
                "some-hash",
                LocalDateTime.now().plusMinutes(5),
                false,
                null,
                null
        );

        when(pairingCodeRepository
                .findByCodeAndConsumedFalseAndClaimedByUserIsNullAndExpiresAtAfter(
                        eq(code), any(LocalDateTime.class)))
                .thenReturn(Optional.of(pairingCode));

        when(userRepository.findByEmail(email))
                .thenReturn(Optional.of(user));

        when(robotRepository.findByUserId(userId))
                .thenReturn(List.of(otherRobot));

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> robotPairingService.claim(email, code)
        );

        assertStatus(HttpStatus.CONFLICT, ex);

        verify(robotRepository, never()).findBySerialNumber(anyString());
        verify(robotRepository, never()).save(any());
        verify(pairingCodeRepository, never()).save(any());
    }

    // ---------- collect ----------

    @Test
    void collect_unclaimedCode_returnsPendingStatus() throws Exception {
        String secret = "test-secret";

        PairingCode pairingCode = pairingCode(
                "ROBOT-001",
                "123456",
                sha256(secret),
                LocalDateTime.now().plusMinutes(5),
                false,
                null,
                null
        );

        when(pairingCodeRepository.findBySecretHash(sha256(secret)))
                .thenReturn(Optional.of(pairingCode));

        PairingStatusResponse result =
                robotPairingService.collect(secret);

        assertNotNull(result);

        verify(pairingCodeRepository).findBySecretHash(sha256(secret));

        verifyNoInteractions(jwtServices);

        verify(pairingCodeRepository, never()).save(any());
    }

    @Test
    void collect_claimedCode_generatesTokensAndMarksCodeConsumed()
            throws Exception {

        String secret = "test-secret";
        UUID robotId = UUID.randomUUID();

        User user = userWithId(UUID.randomUUID());
        Robot robot = mock(Robot.class);

        when(robot.getId()).thenReturn(robotId);

        PairingCode pairingCode = pairingCode(
                "ROBOT-001",
                "123456",
                sha256(secret),
                LocalDateTime.now().plusMinutes(5),
                false,
                user,
                robot
        );

        when(pairingCodeRepository.findBySecretHash(sha256(secret)))
                .thenReturn(Optional.of(pairingCode));

        when(jwtServices.generateDeviceAccessToken(robotId))
                .thenReturn("access-token");

        when(jwtServices.generateDeviceRefreshToken(robotId))
                .thenReturn("refresh-token");

        when(jwtServices.getDeviceAccessExpirationSeconds())
                .thenReturn(900L);

        PairingStatusResponse result =
                robotPairingService.collect(secret);

        assertNotNull(result);

        verify(jwtServices).generateDeviceAccessToken(robotId);
        verify(jwtServices).generateDeviceRefreshToken(robotId);
        verify(jwtServices).getDeviceAccessExpirationSeconds();

        verify(pairingCode).setConsumed(true);
        verify(pairingCodeRepository).save(pairingCode);
    }

    @Test
    void collect_unknownSecret_throwsNotFound() throws Exception {
        String secret = "unknown-secret";

        when(pairingCodeRepository.findBySecretHash(sha256(secret)))
                .thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> robotPairingService.collect(secret)
        );

        assertStatus(HttpStatus.NOT_FOUND, ex);

        verifyNoInteractions(jwtServices);
    }

    @Test
    void collect_alreadyConsumedCode_throwsGone() throws Exception {
        String secret = "test-secret";

        PairingCode pairingCode = pairingCode(
                "ROBOT-001",
                "123456",
                sha256(secret),
                LocalDateTime.now().plusMinutes(5),
                true,
                userWithId(UUID.randomUUID()),
                mock(Robot.class)
        );

        when(pairingCodeRepository.findBySecretHash(sha256(secret)))
                .thenReturn(Optional.of(pairingCode));

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> robotPairingService.collect(secret)
        );

        assertStatus(HttpStatus.GONE, ex);

        verifyNoInteractions(jwtServices);
        verify(pairingCodeRepository, never()).save(any());
    }

    @Test
    void collect_expiredUnclaimedCode_throwsGone() throws Exception {
        String secret = "expired-secret";

        PairingCode pairingCode = pairingCode(
                "ROBOT-001",
                "123456",
                sha256(secret),
                LocalDateTime.now().minusMinutes(1),
                false,
                null,
                null
        );

        when(pairingCodeRepository.findBySecretHash(sha256(secret)))
                .thenReturn(Optional.of(pairingCode));

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> robotPairingService.collect(secret)
        );

        assertStatus(HttpStatus.GONE, ex);

        verifyNoInteractions(jwtServices);
        verify(pairingCodeRepository, never()).save(any());
    }

    // ---------- refreshDevice ----------

    @Test
    void refreshDevice_pairedRobot_returnsNewAccessToken() {
        String refreshToken = "refresh-token";
        UUID robotId = UUID.randomUUID();

        User user = userWithId(UUID.randomUUID());
        Robot robot = robotWithSerial("ROBOT-001", user);

        when(robot.getId()).thenReturn(robotId);

        when(jwtServices.extractRobotId(refreshToken))
                .thenReturn(robotId);

        when(robotRepository.findById(robotId))
                .thenReturn(Optional.of(robot));

        when(jwtServices.generateDeviceAccessToken(robotId))
                .thenReturn("new-access-token");

        when(jwtServices.getDeviceAccessExpirationSeconds())
                .thenReturn(900L);

        RefreshResponseDto result =
                robotPairingService.refreshDevice(refreshToken);

        assertNotNull(result);

        verify(jwtServices).extractRobotId(refreshToken);
        verify(robotRepository).findById(robotId);
        verify(jwtServices).generateDeviceAccessToken(robotId);
        verify(jwtServices).getDeviceAccessExpirationSeconds();
    }

    @Test
    void refreshDevice_robotNotFound_throwsUnauthorized() {
        String refreshToken = "refresh-token";
        UUID robotId = UUID.randomUUID();

        when(jwtServices.extractRobotId(refreshToken))
                .thenReturn(robotId);

        when(robotRepository.findById(robotId))
                .thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> robotPairingService.refreshDevice(refreshToken)
        );

        assertStatus(HttpStatus.UNAUTHORIZED, ex);

        verify(jwtServices, never()).generateDeviceAccessToken(any());
    }

    @Test
    void refreshDevice_robotNotPaired_throwsUnauthorized() {
        String refreshToken = "refresh-token";
        UUID robotId = UUID.randomUUID();

        Robot robot = robotWithSerial("ROBOT-001", null);

        when(jwtServices.extractRobotId(refreshToken))
                .thenReturn(robotId);

        when(robotRepository.findById(robotId))
                .thenReturn(Optional.of(robot));

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> robotPairingService.refreshDevice(refreshToken)
        );

        assertStatus(HttpStatus.UNAUTHORIZED, ex);

        verify(jwtServices, never()).generateDeviceAccessToken(any());
    }
}
