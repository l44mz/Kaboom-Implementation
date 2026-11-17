import 'package:flutter/material.dart';
import 'package:get/get.dart';
import 'package:go_router/go_router.dart';

import '../../../core/router/app_router.dart';
import '../../../shared/widgets/app_snackbar.dart';
import '../../../shared/widgets/app_text_field.dart';
import '../../../shared/widgets/primary_button.dart';
import '../controllers/auth_controller.dart';
import '../widgets/auth_footer_link.dart';
import '../widgets/auth_header.dart';

class LoginScreen extends StatefulWidget {
  const LoginScreen({super.key});

  @override
  State<LoginScreen> createState() => _LoginScreenState();
}

class _LoginScreenState extends State<LoginScreen> {
  final _formKey = GlobalKey<FormState>();
  final _emailController = TextEditingController();
  final _passwordController = TextEditingController();
  // Own tag, so it never mixes with the Sign Up screen's AuthController.
  final _controller = Get.put(AuthController(), tag: 'login');

  @override
  void dispose() {
    _emailController.dispose();
    _passwordController.dispose();
    Get.delete<AuthController>(tag: 'login');
    super.dispose();
  }

  Future<void> _onSubmit() async {
    // Hide the keyboard and stop if any field is invalid.
    FocusScope.of(context).unfocus();
    if (!_formKey.currentState!.validate()) return;

    final user = await _controller.login(
      email: _emailController.text,
      password: _passwordController.text,
    );
    if (!mounted) return;

    if (user == null) {
      showAppSnackBar(context, _controller.errorMessage.value ?? '', isError: true);
    } else {
      context.go(user.isAdmin ? AppRoutes.admin : AppRoutes.home);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: SafeArea(
        child: SingleChildScrollView(
          padding: const EdgeInsets.symmetric(horizontal: 32, vertical: 40),
          child: Form(
            key: _formKey,
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                const AuthHeader(
                  title: 'Log In',
                  subtitle: 'Sparky Is Ready To Focus Again!',
                ),
                const SizedBox(height: 40),
                AppTextField(
                  label: 'Email',
                  hint: 'Email',
                  icon: Icons.mail_outline,
                  controller: _emailController,
                  keyboardType: TextInputType.emailAddress,
                  validator: AuthController.validateEmail,
                ),
                const SizedBox(height: 16),
                AppTextField(
                  label: 'Password',
                  hint: 'Password',
                  icon: Icons.key_outlined,
                  controller: _passwordController,
                  obscureText: true,
                  validator: AuthController.validateLoginPassword,
                ),
                const SizedBox(height: 48),
                // Rebuilds only the button when isLoading changes.
                Obx(
                  () => PrimaryButton(
                    label: 'Login',
                    onPressed: _onSubmit,
                    isLoading: _controller.isLoading.value,
                  ),
                ),
                const SizedBox(height: 24),
                AuthFooterLink(
                  question: "Don't have an account?",
                  action: 'Sign Up',
                  onTap: () => context.go(AppRoutes.signUp),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}