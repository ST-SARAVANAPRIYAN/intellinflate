import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../../viewmodels/intellinflate_viewmodel.dart';

class HistoryScreen extends StatelessWidget {
  const HistoryScreen({super.key});

  @override
  Widget build(BuildContext context) {
    final viewModel = Provider.of<IntelliInflateViewModel>(context);
    final reports = viewModel.tireHealthReports;

    return Padding(
      padding: const EdgeInsets.all(16),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Text("Tire Health Reports", style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold)),
          const SizedBox(height: 8),
          const Text("Saved reports for the logged-in user."),
          const SizedBox(height: 12),
          Align(
            alignment: Alignment.centerRight,
            child: OutlinedButton.icon(
              onPressed: () => viewModel.loadTireHealthReports(),
              icon: const Icon(Icons.refresh),
              label: const Text('Refresh'),
            ),
          ),
          const SizedBox(height: 10),
          Expanded(
            child: reports.isEmpty
                ? const Center(child: Text("No reports available. Save a report from Health Scan."))
                : ListView.separated(
                    itemCount: reports.length,
                    separatorBuilder: (_, __) => const SizedBox(height: 8),
                    itemBuilder: (context, index) {
                      final report = reports[index];
                      final score = report['overallScore']?.toString() ?? '--';
                      final plate = report['numberPlate']?.toString() ?? 'Unknown';
                      final summary = report['summary']?.toString() ?? 'No summary';
                      final date = report['generatedAt']?.toString() ?? '';
                      final assets = _reportAssets(report);
                      final scoreValue = _scoreValue(report['overallScore']);

                      return Card(
                        clipBehavior: Clip.antiAlias,
                        child: Padding(
                          padding: const EdgeInsets.all(12),
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              Row(
                                crossAxisAlignment: CrossAxisAlignment.start,
                                children: [
                                  Container(
                                    width: 64,
                                    height: 64,
                                    decoration: BoxDecoration(
                                      color: Colors.blueGrey.shade50,
                                      borderRadius: BorderRadius.circular(14),
                                    ),
                                    child: assets.isNotEmpty && (assets.first['imageUrl']?.toString().isNotEmpty ?? false)
                                        ? ClipRRect(
                                            borderRadius: BorderRadius.circular(14),
                                            child: Image.network(
                                              assets.first['imageUrl'].toString(),
                                              fit: BoxFit.cover,
                                              errorBuilder: (_, __, ___) => const Icon(Icons.image_not_supported_outlined),
                                            ),
                                          )
                                        : const Icon(Icons.description),
                                  ),
                                  const SizedBox(width: 12),
                                  Expanded(
                                    child: Column(
                                      crossAxisAlignment: CrossAxisAlignment.start,
                                      children: [
                                        Text('Plate $plate • Score $score', style: const TextStyle(fontWeight: FontWeight.bold)),
                                        const SizedBox(height: 4),
                                        Text(summary),
                                        const SizedBox(height: 4),
                                        Text(date, style: TextStyle(fontSize: 12, color: Colors.grey.shade600)),
                                      ],
                                    ),
                                  ),
                                ],
                              ),
                              const SizedBox(height: 12),
                              ClipRRect(
                                borderRadius: BorderRadius.circular(999),
                                child: LinearProgressIndicator(
                                  value: scoreValue == null ? null : (scoreValue / 100).clamp(0, 1),
                                  minHeight: 8,
                                  backgroundColor: Colors.blueGrey.shade100,
                                ),
                              ),
                              const SizedBox(height: 10),
                              if (assets.isNotEmpty)
                                SizedBox(
                                  height: 72,
                                  child: ListView.separated(
                                    scrollDirection: Axis.horizontal,
                                    itemCount: assets.length,
                                    separatorBuilder: (_, __) => const SizedBox(width: 8),
                                    itemBuilder: (context, assetIndex) {
                                      final asset = assets[assetIndex];
                                      return Column(
                                        crossAxisAlignment: CrossAxisAlignment.start,
                                        children: [
                                          ClipRRect(
                                            borderRadius: BorderRadius.circular(10),
                                            child: Image.network(
                                              asset['imageUrl'].toString(),
                                              width: 88,
                                              height: 48,
                                              fit: BoxFit.cover,
                                              errorBuilder: (_, __, ___) => Container(
                                                width: 88,
                                                height: 48,
                                                color: Colors.blueGrey.shade50,
                                                alignment: Alignment.center,
                                                child: const Icon(Icons.broken_image_outlined, size: 18),
                                              ),
                                            ),
                                          ),
                                          const SizedBox(height: 4),
                                          SizedBox(
                                            width: 88,
                                            child: Text(
                                              asset['label']?.toString() ?? 'Scan',
                                              maxLines: 1,
                                              overflow: TextOverflow.ellipsis,
                                              style: const TextStyle(fontSize: 11),
                                            ),
                                          ),
                                        ],
                                      );
                                    },
                                  ),
                                ),
                            ],
                          ),
                        ),
                      );
                    },
                  ),
          ),
        ],
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
}
