import 'package:flutter/material.dart';
import 'package:lottie/lottie.dart';

/// Kaboom's Lottie animations, in assets/animations.
enum AppAnimation {
  confetti('confetti'),
  coinShine('coin_shine'),
  sparkles('sparkles'),
  loading('loading'),
  pairing('pairing');

  const AppAnimation(this.file);

  final String file;

  String get path => 'assets/animations/$file.json';
}

/// Plays one of the [AppAnimation]s. Set [repeat] to false to play it once.
class AppLottie extends StatelessWidget {
  const AppLottie(
    this.animation, {
    super.key,
    this.width,
    this.height,
    this.repeat = true,
  });

  final AppAnimation animation;
  final double? width;
  final double? height;
  final bool repeat;

  @override
  Widget build(BuildContext context) {
    return Lottie.asset(
      animation.path,
      width: width,
      height: height,
      repeat: repeat,
      fit: BoxFit.contain,
      // If the file can't load, keep the space empty instead of showing an error.
      errorBuilder: (context, error, stack) =>
          SizedBox(width: width, height: height),
    );
  }
}
