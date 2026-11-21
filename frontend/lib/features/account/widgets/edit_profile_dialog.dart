import 'package:flutter/material.dart';

import '../../../core/theme/app_colors.dart';
import '../../../shared/widgets/app_text_field.dart';
import '../controllers/account_controller.dart';

/// Edit name and email. [onSave] returns null on success, or a message to show.
/// The dialog returns true when it saved.
Future<bool> showEditProfileDialog(
  BuildContext context, {
  required String name,
  required String email,
  required Future<String?> Function(String name, String email) onSave,
}) async {
  final saved = await showDialog<bool>(
    context: context,
    builder: (context) => _EditProfileDialog(name: name, email: email, onSave: onSave),
  );
  return saved ?? false;
}

class _EditProfileDialog extends StatefulWidget {
  const _EditProfileDialog({
    required this.name,
    required this.email,
    required this.onSave,
  });

  final String name;
  final String email;
  final Future<String?> Function(String name, String email) onSave;

  @override
  State<_EditProfileDialog> createState() => _EditProfileDialogState();
}

class _EditProfileDialogState extends State<_EditProfileDialog> {
  final _formKey = GlobalKey<FormState>();
  late final _name = TextEditingController(text: widget.name);
  late final _email = TextEditingController(text: widget.email);
  bool _saving = false;
  String? _error;

  @override
  void dispose() {
    _name.dispose();
    _email.dispose();
    super.dispose();
  }

  Future<void> _save() async {
    if (!_formKey.currentState!.validate()) return;
    setState(() {
      _saving = true;
      _error = null;
    });
    final error = await widget.onSave(_name.text, _email.text);
    if (!mounted) return;
    if (error == null) {
      Navigator.of(context).pop(true);
    } else {
      setState(() {
        _saving = false;
        _error = error;
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    return AlertDialog(
      backgroundColor: AppColors.surface,
      title: const Text('Edit profile'),
      content: Form(
        key: _formKey,
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            AppTextField(
              label: 'Name',
              icon: Icons.person_outline,
              controller: _name,
              validator: AccountController.validateName,
            ),
            const SizedBox(height: 14),
            AppTextField(
              label: 'Email',
              icon: Icons.mail_outline,
              controller: _email,
              keyboardType: TextInputType.emailAddress,
              validator: AccountController.validateEmail,
            ),
            const SizedBox(height: 10),
            const Text(
              "If you change your email, you'll log in again with the new one.",
              style: TextStyle(fontSize: 12, color: AppColors.textMuted),
            ),
            if (_error != null) ...[
              const SizedBox(height: 10),
              Text(_error!, style: const TextStyle(color: AppColors.away)),
            ],
          ],
        ),
      ),
      actions: [
        TextButton(
          onPressed: _saving ? null : () => Navigator.of(context).pop(false),
          child: const Text('Cancel'),
        ),
        FilledButton(
          onPressed: _saving ? null : _save,
          child: _saving
              ? const SizedBox(
                  width: 18,
                  height: 18,
                  child: CircularProgressIndicator(strokeWidth: 2),
                )
              : const Text('Save'),
        ),
      ],
    );
  }
}
