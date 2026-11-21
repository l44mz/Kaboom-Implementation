import 'package:flutter/material.dart';
import 'package:get/get.dart';

import '../../../core/theme/app_colors.dart';
import '../../../shared/widgets/app_drawer.dart';
import '../../../shared/widgets/error_view.dart';
import '../../../shared/widgets/filter_pills.dart';
import '../../../shared/widgets/page_header.dart';
import '../../../shared/widgets/stat_box.dart';
import '../controllers/history_controller.dart';
import '../models/history.dart';
import '../widgets/focus_bar_chart.dart';
import '../widgets/past_session_tile.dart';

/// Session History (Figma "Session History").
class HistoryScreen extends StatefulWidget {
  const HistoryScreen({super.key});

  @override
  State<HistoryScreen> createState() => _HistoryScreenState();
}

class _HistoryScreenState extends State<HistoryScreen> {
  // Get.put creates the controller, registers it in GetX, and calls its onInit()
  // (which loads the history). Other widgets could now use Get.find<HistoryController>().
  final _controller = Get.put(HistoryController());

  static const _avgColor = Color(0xFF38BDF8);

  @override
  void dispose() {
    // We navigate with go_router, not GetX, so GetX can't tell when this page closes.
    // Delete the controller ourselves, or it stays in memory with old data.
    Get.delete<HistoryController>();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      drawer: const AppDrawer(),
      body: SafeArea(
        // Obx redraws everything inside when any .value read inside it changes
        // (range, data, isLoading, errorMessage, showAll). No listenable to pass.
        child: Obx(
          () => RefreshIndicator(
            onRefresh: _controller.load,
            child: ListView(
              padding: const EdgeInsets.fromLTRB(16, 8, 16, 32),
              children: [
                const PageHeader(),
                const Padding(
                  padding: EdgeInsets.fromLTRB(8, 4, 8, 16),
                  child: PageTitle('History', subtitle: 'Track your focus trends over time'),
                ),
                Padding(
                  padding: const EdgeInsets.symmetric(horizontal: 8),
                  child: FilterPills<HistoryRange>(
                    options: {for (final r in HistoryRange.values) r: r.label},
                    selected: _controller.range.value,
                    onSelected: _controller.setRange,
                  ),
                ),
                const SizedBox(height: 16),
                Padding(
                  padding: const EdgeInsets.symmetric(horizontal: 8),
                  child: _buildContent(),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }

  Widget _buildContent() {
    // Called inside the Obx above, so these .value reads are tracked too.
    final data = _controller.data.value;

    if (_controller.isLoading.value && data == null) {
      return const Padding(
        padding: EdgeInsets.only(top: 60),
        child: Center(child: CircularProgressIndicator()),
      );
    }
    if (_controller.errorMessage.value != null && data == null) {
      return SizedBox(
        height: 260,
        child: ErrorView(message: _controller.errorMessage.value!, onRetry: _controller.load),
      );
    }
    if (data == null) return const SizedBox.shrink();

    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        // Totals
        Row(
          children: [
            Expanded(
              child: StatBox(
                value: HistoryFormat.duration(data.totalFocusSeconds),
                label: 'Total focus',
              ),
            ),
            const SizedBox(width: 10),
            Expanded(
              child: StatBox(value: '${data.sessionCount}', label: 'Sessions'),
            ),
            const SizedBox(width: 10),
            Expanded(
              child: StatBox(
                value: data.sessionCount == 0 ? '–' : '${data.avgFocusPercentage.round()}%',
                label: 'Avg focus',
                valueColor: _avgColor,
              ),
            ),
          ],
        ),
        const SizedBox(height: 14),

        // Chart
        FocusBarChart(bars: data.bars, caption: _controller.range.value.chartCaption),
        const SizedBox(height: 20),

        // Past sessions
        Row(
          children: [
            const Text(
              'Past sessions',
              style: TextStyle(fontSize: 15, fontWeight: FontWeight.w600),
            ),
            const Spacer(),
            if (_controller.hasMoreSessions)
              TextButton(
                onPressed: _controller.toggleShowAll,
                child: Text(
                  _controller.showAll.value ? 'Show less' : 'See all',
                  style: const TextStyle(fontWeight: FontWeight.w600),
                ),
              ),
          ],
        ),
        const SizedBox(height: 8),
        if (data.sessions.isEmpty)
          const Padding(
            padding: EdgeInsets.symmetric(vertical: 24),
            child: Text(
              'No finished sessions here yet.\nStart one from Home!',
              textAlign: TextAlign.center,
              style: TextStyle(color: AppColors.textMuted),
            ),
          )
        else
          for (final session in _controller.visibleSessions)
            PastSessionTile(session: session),
      ],
    );
  }
}