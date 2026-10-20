/// The logged-in user, as returned by GET /users/me and inside the login response.
class User {
  const User({
    required this.id,
    required this.name,
    required this.email,
    required this.coins,
    required this.role,
  });

  final String id;
  final String name;
  final String email;
  final int coins;
  final String role;

  /// Builds a User from the backend's JSON (UserDto).
  factory User.fromJson(Map<String, dynamic> json) => User(
        id: json['id'] as String,
        name: json['name'] as String,
        email: json['email'] as String,
        coins: json['coins'] as int,
        role: json['role'] as String,
      );

  /// True for admin accounts, used to show the Admin link in the menu.
  bool get isAdmin => role == 'admin';
}
