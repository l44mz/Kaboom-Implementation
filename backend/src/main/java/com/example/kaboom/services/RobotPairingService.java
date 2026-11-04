package com.example.kaboom.services;

import com.example.kaboom.dtos.*;
import com.example.kaboom.entities.PairingCode;
import com.example.kaboom.entities.Robot;
import com.example.kaboom.entities.User;
import com.example.kaboom.repositories.PairingCodeRepository;
import com.example.kaboom.repositories.RobotRepository;
import com.example.kaboom.repositories.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

@Service
public class RobotPairingService {

    private static final int CODE_TTL_MINUTES = 10;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final PairingCodeRepository pairingCodeRepository;
    private final RobotRepository robotRepository;
    private final UserRepository userRepository;
    private final JwtServices jwtServices;

    public RobotPairingService(PairingCodeRepository pairingCodeRepository,
                               RobotRepository robotRepository,
                               UserRepository userRepository,
                               JwtServices jwtServices) {
        this.pairingCodeRepository = pairingCodeRepository;
        this.robotRepository = robotRepository;
        this.userRepository = userRepository;
        this.jwtServices = jwtServices;
    }

    /** Robot asks for a code to show on its screen. */
    @Transactional
    public PairingCodeResponse requestCode(String serialNumber) {
        pairingCodeRepository.deleteBySerialNumberAndConsumedFalse(serialNumber);

        LocalDateTime now = LocalDateTime.now();
        String code;
        do {
            code = String.format("%06d", RANDOM.nextInt(1_000_000));
        } while (pairingCodeRepository
                .existsByCodeAndConsumedFalseAndExpiresAtAfter(code, now));

        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String secret = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        PairingCode pairingCode = new PairingCode();               // ADAPT: your constructor/setters
        pairingCode.setSerialNumber(serialNumber);
        pairingCode.setCode(code);
        pairingCode.setSecretHash(sha256(secret));
        pairingCode.setExpiresAt(now.plusMinutes(CODE_TTL_MINUTES));
        pairingCodeRepository.save(pairingCode);

        return new PairingCodeResponse(code, secret, pairingCode.getExpiresAt());
    }

    /** App claims the code with the user's JWT. */
    @Transactional
    public Robot claim(String email, String code) {
        PairingCode pairingCode = pairingCodeRepository
                .findByCodeAndConsumedFalseAndClaimedByUserIsNullAndExpiresAtAfter(
                        code, LocalDateTime.now())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Pairing code not found or expired"));

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "User not found"));

        String serial = pairingCode.getSerialNumber();

        // The user may already own a robot, but only the same physical one is allowed.
        List<Robot> owned = robotRepository.findByUserId(user.getId());
        boolean hasDifferentRobot = owned.stream()
                .anyMatch(r -> !serial.equals(r.getSerialNumber()));
        if (hasDifferentRobot) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "User already has a different robot");
        }

        Robot robot = robotRepository.findBySerialNumber(serial).orElseGet(() -> {
            Robot created = new Robot();
            created.setSerialNumber(serial);
            return created;
        });
        robot.setUser(user);
        robot = robotRepository.save(robot);

        pairingCode.setClaimedByUser(user);
        pairingCode.setRobot(robot);
        pairingCodeRepository.save(pairingCode);

        return robot;
    }

    /** Robot polls with its secret; tokens are returned exactly once. */
    @Transactional
    public PairingStatusResponse collect(String secret) {
        PairingCode pairingCode = pairingCodeRepository.findBySecretHash(sha256(secret))
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Unknown pairing secret"));

        if (pairingCode.isConsumed()) {
            throw new ResponseStatusException(HttpStatus.GONE, "Tokens already collected");
        }

        if (pairingCode.getClaimedByUser() == null) {
            if (pairingCode.getExpiresAt().isBefore(LocalDateTime.now())) {
                throw new ResponseStatusException(HttpStatus.GONE, "Pairing code expired");
            }
            return new PairingStatusResponse("pending", null, null, null, null);
        }

        Robot robot = pairingCode.getRobot();

        String accessToken = jwtServices.generateDeviceAccessToken(robot.getId());
        String refreshToken = jwtServices.generateDeviceRefreshToken(robot.getId());
        long expiresIn = jwtServices.getDeviceAccessExpirationSeconds();

        pairingCode.setConsumed(true);
        pairingCodeRepository.save(pairingCode);

        return new PairingStatusResponse(
                "paired", robot.getId(), accessToken, refreshToken, expiresIn);
    }

    @Transactional(readOnly = true)
    public RefreshResponseDto refreshDevice(String refreshToken) {
        UUID robotId = jwtServices.extractRobotId(refreshToken);

        Robot robot = robotRepository.findById(robotId)
                .filter(r -> r.getUser() != null)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "Robot is not paired"));

        // ADAPT: match the constructor of your RefreshResponseDto
        return new RefreshResponseDto(
                jwtServices.generateDeviceAccessToken(robot.getId()),
                jwtServices.getDeviceAccessExpirationSeconds());
    }

    private static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(
                    digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
}
