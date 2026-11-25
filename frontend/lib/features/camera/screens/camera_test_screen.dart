import 'package:camera/camera.dart';
import 'package:flutter/material.dart';

import '../../../core/theme/app_colors.dart';
import '../services/focus_detector.dart';

/// "Test the camera": shows what the camera sees and what it decides, live
/// (on a phone or on the website).
/// Used to check camera mode before a real session, and to see why the
/// state changes (the head angles, the eyes, and the reasons).
class CameraTestScreen extends StatefulWidget {
  const CameraTestScreen({super.key});

  @override
  State<CameraTestScreen> createState() => _CameraTestScreenState();
}

class _CameraTestScreenState extends State<CameraTestScreen> {
  final _detector = FocusDetector();
  String? _error;
  bool _starting = true;

  @override
  void initState() {
    super.initState();
    _start();
  }

  Future<void> _start() async {
    try {
      await _detector.start();
    } on CameraException catch (e) {
      _error = e.code == 'CameraAccessDenied'
          ? 'Kaboom needs the camera. Allow it in the phone or browser settings, then try again.'
          : 'Could not open the camera (${e.code}).';
    } catch (e) {
      _error = 'Could not open the camera: $e';
    }
    if (mounted) setState(() => _starting = false);
  }

  @override
  void dispose() {
    _detector.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Test the camera'),
        backgroundColor: Colors.transparent,
        surfaceTintColor: Colors.transparent,
        foregroundColor: AppColors.textPrimary,
      ),
      body: SafeArea(child: _buildBody()),
    );
  }

  Widget _buildBody() {
    if (_starting) return const Center(child: CircularProgressIndicator());
    final error = _error;
    if (error != null) {
      return Center(
        child: Padding(
          padding: const EdgeInsets.all(32),
          child: Text(error, textAlign: TextAlign.center),
        ),
      );
    }
    return SingleChildScrollView(
      padding: const EdgeInsets.fromLTRB(24, 8, 24, 24),
      child: Column(
        children: [
          // The preview, small and round-cornered. Only to help place the phone.
          ClipRRect(
            borderRadius: BorderRadius.circular(16),
            child: SizedBox(
              width: 200,
              child: AspectRatio(
                aspectRatio: _detector.previewAspectRatio,
                child: _detector.buildPreview(),
              ),
            ),
          ),
          const SizedBox(height: 20),
          StreamBuilder<FocusReading>(
            stream: _detector.readings,
            builder: (context, snapshot) {
              final reading = snapshot.data;
              if (reading == null) return const Text('Looking for your face...');
              return _ReadingView(reading: reading);
            },
          ),
          const SizedBox(height: 12),
          // If frames can't be checked, say why instead of waiting forever.
          ValueListenableBuilder<String?>(
            valueListenable: _detector.problem,
            builder: (context, problem, _) => problem == null
                ? const SizedBox.shrink()
                : Text(
                    '$problem\nFrames from the camera: ${_detector.framesReceived}',
                    textAlign: TextAlign.center,
                    style: const TextStyle(fontSize: 12, color: AppColors.away),
                  ),
          ),
        ],
      ),
    );
  }
}

/// The state, and the numbers behind it.
class _ReadingView extends StatelessWidget {
  const _ReadingView({required this.reading});

  final FocusReading reading;

  @override
  Widget build(BuildContext context) {
    final obs = reading.observation;
    final state = reading.state;

    return Column(
      children: [
        Container(
          padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 8),
          decoration: BoxDecoration(
            color: state.chipColor,
            borderRadius: BorderRadius.circular(18),
            border: Border.all(color: state.chipBorder, width: 2),
          ),
          child: Text(
            state.label,
            style: const TextStyle(fontSize: 18, fontWeight: FontWeight.w700),
          ),
        ),
        const SizedBox(height: 8),
        Text(
          reading.calibrated
              ? (reading.reasons.isEmpty ? 'Everything looks fine' : reading.reasons.join(', '))
              : 'Learning your working pose... look at your work for 3 seconds',
          textAlign: TextAlign.center,
          style: const TextStyle(fontSize: 13, color: AppColors.textMuted),
        ),
        const SizedBox(height: 16),
        _Row('Face', obs.facePresent ? 'yes' : 'no'),
        _Row('Head left/right (yaw)', _deg(obs.yaw), limit: '30°'),
        _Row('Head up/down (pitch)', _deg(obs.pitch), limit: '20°'),
        _Row('Head tilt (roll)', _deg(obs.roll), limit: '30°'),
        _Row('Eyes open', _pct(obs.eyeOpen)),
        _Row('Eyes vs. your normal', _pct(obs.eyeOpenRatio), limit: '40%'),
        const SizedBox(height: 12),
        const Text(
          'Distracted after 5 s with the head turned or 3 s with the eyes closed. '
          'Away after 3 s with no face. The angles are measured from your working pose.',
          textAlign: TextAlign.center,
          style: TextStyle(fontSize: 12, color: AppColors.textMuted),
        ),
      ],
    );
  }

  static String _deg(double? v) => v == null ? '-' : '${v.toStringAsFixed(0)}°';
  static String _pct(double? v) => v == null ? '-' : '${(v * 100).toStringAsFixed(0)}%';
}

class _Row extends StatelessWidget {
  const _Row(this.label, this.value, {this.limit});

  final String label;
  final String value;
  final String? limit;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 3),
      child: Row(
        children: [
          Expanded(child: Text(label, style: const TextStyle(fontSize: 14))),
          Text(
            limit == null ? value : '$value  (limit $limit)',
            style: const TextStyle(fontSize: 14, fontWeight: FontWeight.w600),
          ),
        ],
      ),
    );
  }
}