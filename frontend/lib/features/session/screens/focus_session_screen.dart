import 'package:flutter/material.dart';

import '../models/focus_session.dart';

class FocusSessionScreen extends StatelessWidget {
  const FocusSessionScreen({super.key, this.session});

  /// The session just started on Home, passed through the router's `extra`.
  final FocusSession? session;

  @override
  Widget build(BuildContext context) {
    return const Scaffold(
      body: SafeArea(child: Center(child: Text('FocusSession'))),
    );
  }
}
