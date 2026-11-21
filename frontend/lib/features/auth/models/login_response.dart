import '../../../shared/models/user.dart';

/// What POST /api/v1/auth/login returns.
class LoginResponse {
  const LoginResponse({
    required this.accessToken,
    required this.refreshToken,
    required this.expiresInSeconds,
    required this.user,
  });

  final String accessToken;
  final String refreshToken;
  final int expiresInSeconds;
  final User user;

  factory LoginResponse.fromJson(Map<String, dynamic> json) => LoginResponse(
        accessToken: json['accessToken'] as String,
        refreshToken: json['refreshToken'] as String,
        expiresInSeconds: json['expiresInSeconds'] as int,
        user: User.fromJson(json['user'] as Map<String, dynamic>),
      );
}