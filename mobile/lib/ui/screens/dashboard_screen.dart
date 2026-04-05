
import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../../viewmodels/intellinflate_viewmodel.dart';

class DashboardScreen extends StatelessWidget {
  const DashboardScreen({super.key});

  @override
  Widget build(BuildContext context) {
    final viewModel = Provider.of<IntelliInflateViewModel>(context);
    final user = viewModel.currentUser;
    final latestReport = viewModel.tireHealthReports.isNotEmpty ? viewModel.tireHealthReports.first : null;
    final plateDone = viewModel.vehicleDetectionResult?.success == true;
    final frontDone = viewModel.frontAnalysis != null;
    final sideDone = viewModel.sideLeftAnalysis != null || viewModel.sideRightAnalysis != null;

    return SingleChildScrollView(
      padding: const EdgeInsets.all(16.0),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          TweenAnimationBuilder<double>(
            tween: Tween(begin: 0.96, end: 1),
            duration: const Duration(milliseconds: 280),
            builder: (context, value, child) => Transform.scale(scale: value, child: child),
            child: _buildWelcomeCard(user),
          ),
          const SizedBox(height: 24),
          const Text("Quick Actions", style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold)),
          const SizedBox(height: 12),
          Row(
            children: [
              _buildActionCard(
                context,
                "Scan Vehicle",
                Icons.directions_car,
                Colors.blue,
                () => viewModel.selectTab(NavigationTab.vehicleDetection),
              ),
              const SizedBox(width: 16),
              _buildActionCard(
                context,
                "Analyze Tires",
                Icons.tire_repair,
                Colors.green,
                () => viewModel.selectTab(NavigationTab.tireScan),
              ),
            ],
          ),
          const SizedBox(height: 24),
          const Text("Workflow Status", style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold)),
          const SizedBox(height: 12),
          Card(
            child: Padding(
              padding: const EdgeInsets.all(16.0),
              child: Column(
                children: [
                  _buildStepTile('Step 1: Plate Detection', plateDone),
                  _buildStepTile('Step 2: Front Tire Analysis', frontDone),
                  _buildStepTile('Step 3: Side Crack Analysis', sideDone),
                ],
              ),
            ),
          ),
          const SizedBox(height: 24),
          const Text("Latest Tire Health Report", style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold)),
          const SizedBox(height: 12),
          _buildLatestReportCard(latestReport),
        ],
      ),
    );
  }

  Widget _buildWelcomeCard(user) {
    return Card(
      elevation: 4,
      shape: RoundedCornerShape(12),
      child: Container(
        padding: const EdgeInsets.all(20),
        decoration: BoxDecoration(
          gradient: LinearGradient(colors: [Colors.blue.shade800, Colors.blue.shade500]),
          borderRadius: BorderRadius.circular(12),
        ),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text("Welcome, ${user?.username ?? 'User'}", 
              style: const TextStyle(color: Colors.white, fontSize: 22, fontWeight: FontWeight.bold)),
            const SizedBox(height: 8),
            Text("Vehicle: ${user?.vehicleModel ?? 'Not specified'}", style: const TextStyle(color: Colors.white70)),
            Text("Plate: ${user?.numberPlate ?? 'N/A'}", style: const TextStyle(color: Colors.white70)),
          ],
        ),
      ),
    );
  }

  Widget _buildActionCard(BuildContext context, String title, IconData icon, Color color, VoidCallback onTap) {
    return Expanded(
      child: InkWell(
        onTap: onTap,
        child: Card(
          elevation: 2,
          child: Padding(
            padding: const EdgeInsets.all(16.0),
            child: Column(
              children: [
                Icon(icon, size: 40, color: color),
                const SizedBox(height: 8),
                Text(title, textAlign: TextAlign.center, style: const TextStyle(fontWeight: FontWeight.bold)),
              ],
            ),
          ),
        ),
      ),
    );
  }

  Widget _buildStepTile(String title, bool done) {
    return ListTile(
      contentPadding: EdgeInsets.zero,
      leading: Icon(done ? Icons.check_circle : Icons.timelapse, color: done ? Colors.greenAccent.shade400 : Colors.orangeAccent),
      title: Text(title),
      subtitle: Text(done ? 'Completed' : 'Pending'),
    );
  }

  Widget _buildLatestReportCard(Map<String, dynamic>? report) {
    if (report == null) {
      return const Card(
        child: Padding(
          padding: EdgeInsets.all(16.0),
          child: Text("No reports saved yet. Complete the flow and save report from Health Scan."),
        ),
      );
    }

    final overall = report['overallScore'] ?? '--';
    final summary = report['summary']?.toString() ?? 'No summary';
    final generatedAt = report['generatedAt']?.toString() ?? '';
    final assets = _reportAssets(report);
    final scoreValue = _scoreValue(overall);

    return Card(
      clipBehavior: Clip.antiAlias,
      child: Padding(
        padding: const EdgeInsets.all(16.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Container(
                  width: 72,
                  height: 72,
                  decoration: BoxDecoration(
                    color: Colors.blueGrey.shade50,
                    borderRadius: BorderRadius.circular(16),
                  ),
                  child: assets.isNotEmpty && (assets.first['imageUrl']?.toString().isNotEmpty ?? false)
                      ? ClipRRect(
                          borderRadius: BorderRadius.circular(16),
                          child: Image.network(
                            assets.first['imageUrl'].toString(),
                            fit: BoxFit.cover,
                            errorBuilder: (_, __, ___) => const Icon(Icons.image_not_supported_outlined),
                          ),
                        )
                      : const Icon(Icons.tire_repair, size: 34),
                ),
                const SizedBox(width: 14),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text('Score: $overall', style: const TextStyle(fontSize: 22, fontWeight: FontWeight.bold)),
                      const SizedBox(height: 4),
                      Text(summary),
                      const SizedBox(height: 8),
                      Text(generatedAt, style: TextStyle(color: Colors.grey.shade600, fontSize: 12)),
                    ],
                  ),
                ),
              ],
            ),
            const SizedBox(height: 16),
            ClipRRect(
              borderRadius: BorderRadius.circular(999),
              child: LinearProgressIndicator(
                value: scoreValue == null ? null : (scoreValue / 100).clamp(0, 1),
                minHeight: 10,
                backgroundColor: Colors.blueGrey.shade100,
              ),
            ),
            const SizedBox(height: 12),
            _buildAssetPreviewRow(assets),
          ],
        ),
      ),
    );
  }

  List<Map<String, dynamic>> _reportAssets(Map<String, dynamic> report) {
    final assets = report['scanAssets'];
    if (assets is! List) return const [];
    return assets
        .whereType<Map>()
        .map((asset) => Map<String, dynamic>.from(asset))
        .where((asset) => asset['imageUrl']?.toString().isNotEmpty ?? false)
        .toList();
  }

  double? _scoreValue(dynamic value) {
    if (value is num) return value.toDouble();
    final match = RegExp(r'(\d+(?:\.\d+)?)').firstMatch(value?.toString() ?? '');
    return match == null ? null : double.tryParse(match.group(1)!);
  }

  Widget _buildAssetPreviewRow(List<Map<String, dynamic>> assets) {
    if (assets.isEmpty) {
      return const SizedBox.shrink();
    }

    return SizedBox(
      height: 92,
      child: ListView.separated(
        scrollDirection: Axis.horizontal,
        itemCount: assets.length,
        separatorBuilder: (_, __) => const SizedBox(width: 10),
        itemBuilder: (context, index) {
          final asset = assets[index];
          final label = asset['label']?.toString() ?? 'Scan';
          final imageUrl = asset['imageUrl']?.toString() ?? '';

          return SizedBox(
            width: 140,
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Expanded(
                  child: ClipRRect(
                    borderRadius: BorderRadius.circular(12),
                    child: Image.network(
                      imageUrl,
                      width: double.infinity,
                      fit: BoxFit.cover,
                      errorBuilder: (_, __, ___) => Container(
                        color: Colors.blueGrey.shade50,
                        alignment: Alignment.center,
                        child: const Icon(Icons.broken_image_outlined),
                      ),
                    ),
                  ),
                ),
                const SizedBox(height: 6),
                Text(label, maxLines: 1, overflow: TextOverflow.ellipsis, style: const TextStyle(fontSize: 12)),
              ],
            ),
          );
        },
      ),
    );
  }
}

class RoundedCornerShape extends RoundedRectangleBorder {
  RoundedCornerShape(double radius) : super(borderRadius: BorderRadius.all(Radius.circular(radius)));
}
