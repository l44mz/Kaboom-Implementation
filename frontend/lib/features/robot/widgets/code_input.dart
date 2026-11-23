import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import '../../../core/theme/app_colors.dart';

/// 6 boxes for the pairing code, like the ones on Sparky's screen: 482 913.
///
/// It is one real (invisible) TextField behind 6 boxes that only show the digits,
/// so the keyboard, paste and delete all work normally.
class CodeInput extends StatelessWidget {
  const CodeInput({
    super.key,
    required this.controller,
    required this.focusNode,
    this.length = 6,
    this.hasError = false,
    this.enabled = true,
    this.onChanged,
    this.onCompleted,
  });

  final TextEditingController controller;
  final FocusNode focusNode;
  final int length;
  final bool hasError;
  final bool enabled;
  final ValueChanged<String>? onChanged;

  /// Called when all the digits are typed.
  final ValueChanged<String>? onCompleted;

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      behavior: HitTestBehavior.opaque,
      onTap: enabled ? focusNode.requestFocus : null,
      child: Stack(
        alignment: Alignment.center,
        children: [
          // The real field: invisible, but it gets the keyboard.
          Opacity(
            opacity: 0,
            child: TextField(
              key: const Key('pairing-code-field'),
              controller: controller,
              focusNode: focusNode,
              enabled: enabled,
              autofocus: true,
              keyboardType: TextInputType.number,
              maxLength: length,
              inputFormatters: [FilteringTextInputFormatter.digitsOnly],
              showCursor: false,
              enableInteractiveSelection: false,
              decoration: const InputDecoration(counterText: ''),
              onChanged: (value) {
                onChanged?.call(value);
                if (value.length == length) onCompleted?.call(value);
              },
            ),
          ),
          // What the user sees: the boxes, redrawn when the text or the focus changes.
          ListenableBuilder(
            listenable: Listenable.merge([controller, focusNode]),
            builder: (context, _) {
              final text = controller.text;
              return FittedBox(
                fit: BoxFit.scaleDown,
                child: Row(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    for (var i = 0; i < length; i++) ...[
                      if (i == length ~/ 2) const SizedBox(width: 12), // 482 913
                      _Box(
                        digit: i < text.length ? text[i] : '',
                        active: focusNode.hasFocus && i == text.length.clamp(0, length - 1),
                        hasError: hasError,
                      ),
                      if (i < length - 1) const SizedBox(width: 8),
                    ],
                  ],
                ),
              );
            },
          ),
        ],
      ),
    );
  }
}

class _Box extends StatelessWidget {
  const _Box({required this.digit, required this.active, required this.hasError});

  final String digit;
  final bool active;
  final bool hasError;

  @override
  Widget build(BuildContext context) {
    final borderColor = hasError
        ? AppColors.away
        : active
            ? AppColors.secondary
            : AppColors.primary;
    return AnimatedContainer(
      duration: const Duration(milliseconds: 150),
      width: 44,
      height: 58,
      alignment: Alignment.center,
      decoration: BoxDecoration(
        color: AppColors.field,
        borderRadius: BorderRadius.circular(10),
        border: Border.all(color: borderColor, width: active ? 3 : 2),
      ),
      child: Text(
        digit,
        style: const TextStyle(
          fontSize: 28,
          fontWeight: FontWeight.w700,
          color: AppColors.textPrimary,
        ),
      ),
    );
  }
}