import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';

import '../../../core/router/app_router.dart';
import '../../../shared/widgets/app_snackbar.dart';
import '../../../shared/widgets/app_text_field.dart';
import '../../../shared/widgets/primary_button.dart';
import '../controllers/auth_controller.dart';
import '../widgets/auth_footer_link.dart';
import '../widgets/auth_header.dart';

class SignUpScreen extends StatefulWidget {
  const SignUpScreen({super.key});

  @override
  State<SignUpScreen> createState() => _SignUpScreenState();
}

class _SignUpScreenState extends State<SignUpScreen> {
  final _formKey = GlobalKey<FormState>();
  final _nameController = TextEditingController();
  final _emailController = TextEditingController();
  final _passwordController = TextEditingController();
  final _controller = AuthController();

  @override
  void dispose() {
    _nameController.dispose();
    _emailController.dispose();
    _passwordController.dispose();
    _controller.dispose();
    super.dispose();
  }

  Future<void> _onSubmit() async {
    FocusScope.of(context).unfocus();
    if (!_formKey.currentState!.validate()) return;

    final ok = await _controller.signUp(
      name: _nameController.text,
      email: _emailController.text,
      password: _passwordController.text,
    );
    if (!mounted) return;

    if (ok) {
      showAppSnackBar(context, 'Account created! Please log in.');
      context.go(AppRoutes.login);
    } else {
      showAppSnackBar(context, _controller.errorMessage!, isError: true);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: SafeArea(
        child: SingleChildScrollView(
          padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 32),
          child: Form(
            key: _formKey,
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                const AuthHeader(
                  title: 'Create Your Account',
                  subtitle: 'Start Your Journey With Sparky!',
                ),
                const SizedBox(height: 32),
                AppTextField(
                  label: 'Name',
                  hint: 'Your Name',
                  icon: Icons.person_outline,
                  controller: _nameController,
                  validator: AuthController.validateName,
                ),
                const SizedBox(height: 16),
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
                  validator: AuthController.validateNewPassword,
                ),
                const SizedBox(height: 40),
                ListenableBuilder(
                  listenable: _controller,
                  builder: (context, _) => PrimaryButton(
                    label: 'Sign Up',
                    onPressed: _onSubmit,
                    isLoading: _controller.isLoading,
                  ),
                ),
                const SizedBox(height: 24),
                AuthFooterLink(
                  question: 'Already have an account?',
                  action: 'Login here',
                  onTap: () => context.go(AppRoutes.login),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}