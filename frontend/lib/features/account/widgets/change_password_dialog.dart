import 'package:flutter/material.dart';

import '../../../core/theme/app_colors.dart';
import '../../../shared/widgets/app_text_field.dart';
import '../controllers/account_controller.dart';

/// Current password + new password twice. [onSave] returns null on success, or a message.
/// The dialog returns true when it saved.
Future<bool> showChangePasswordDialog(
  BuildContext context, {
  required Future<String?> Function(String current, String newPassword) onSave,
}) async {
  final saved = await showDialog<bool>(
    context: context,
    builder: (context) => _ChangePasswordDialog(onSave: onSave),
  );
  return saved ?? false;
}

class _ChangePasswordDialog extends StatefulWidget {
  const _ChangePasswordDialog({required this.onSave});

  final Future<String?> Function(String current, String newPassword) onSave;

  @override
  State<_ChangePasswordDialog> createState() => _ChangePasswordDialogState();
}

class _ChangePasswordDialogState extends State<_ChangePasswordDialog> {
  final _formKey = GlobalKey<FormState>();
  final _current = TextEditingController();
  final _new = TextEditingController();
  final _confirm = TextEditingController();
  bool _saving = false;
  String? _error;

  @override
  void dispose() {
    _current.dispose();
    _new.dispose();
    _confirm.dispose();
    super.dispose();
  }

  Future<void> _save() async {
    if (!_formKey.currentState!.validate()) return;
    setState(() {
      _saving = true;
      _error = null;
    });
    final error = await widget.onSave(_current.text, _new.text);
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
      title: const Text('Change password'),
      content: Form(
        key: _formKey,
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            AppTextField(
              label: 'Current password',
              icon: Icons.lock_outline,
              controller: _current,
              obscureText: true,
              validator: (value) =>
                  (value ?? '').isEmpty ? 'Enter your current password' : null,
            ),
            const SizedBox(height: 14),
            AppTextField(
              label: 'New password',
              icon: Icons.lock_reset,
              controller: _new,
              obscureText: true,
              validator: AccountController.validateNewPassword,
            ),
            const SizedBox(height: 14),
            AppTextField(
              label: 'Confirm new password',
              icon: Icons.lock_reset,
              controller: _confirm,
              obscureText: true,
              validator: (value) =>
                  value != _new.text ? "The passwords don't match" : null,
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
