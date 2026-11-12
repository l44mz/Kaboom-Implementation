import 'package:flutter/material.dart';

import 'app_lottie.dart';

/// Bouncing dots (a Lottie animation), with optional text under it.
class LoadingView extends StatelessWidget {
  const LoadingView({super.key, this.message});

  final String? message;

  @override
  Widget build(BuildContext context) {
    return Center(
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          const AppLottie(AppAnimation.loading, width: 120, height: 44),
          if (message != null) ...[
            const SizedBox(height: 16),
            Text(message!, textAlign: TextAlign.center),
          ],
        ],
      ),
    );
  }
}
