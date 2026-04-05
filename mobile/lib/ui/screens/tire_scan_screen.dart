import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:image_picker/image_picker.dart';
import '../../viewmodels/intellinflate_viewmodel.dart';

class TireScanScreen extends StatefulWidget {
  const TireScanScreen({super.key});

  @override
  State<TireScanScreen> createState() => _TireScanScreenState();
}

class _TireScanScreenState extends State<TireScanScreen> {
  final ImagePicker _picker = ImagePicker();

  @override
  Widget build(BuildContext context) {
    final viewModel = Provider.of<IntelliInflateViewModel>(context);
    final plateResult = viewModel.vehicleDetectionResult;

    return SingleChildScrollView(
      padding: const EdgeInsets.all(16.0),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Text("Health Scan Workflow", style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold)),
          const SizedBox(height: 12),
          Card(
            child: Padding(
              padding: const EdgeInsets.all(14.0),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text("Detected Plate: ${plateResult?.detectedPlate ?? 'Not detected'}"),
                  const SizedBox(height: 6),
                  Text("Recognized User: ${plateResult?.user?['username'] ?? 'Not matched'}"),
                  const SizedBox(height: 10),
                  ElevatedButton.icon(
                    onPressed: () => viewModel.selectTab(NavigationTab.vehicleDetection),
                    icon: const Icon(Icons.directions_car),
                    label: const Text("Go To Plate Detection"),
                  ),
                ],
              ),
            ),
          ),
          const SizedBox(height: 16),
          _buildStepCard(
            title: "Step 2: Front Tire Wear + Misalignment",
            subtitle: "Scan front tire after plate detection.",
            enabled: viewModel.canScanFront,
            result: viewModel.frontAnalysis,
            onScan: () => _pickAndRun(viewModel, viewModel.scanFrontTire),
          ),
          const SizedBox(height: 12),
          _buildStepCard(
            title: "Step 3A: Left Side Crack",
            subtitle: "Scan left sidewall crack.",
            enabled: viewModel.canScanSide,
            result: viewModel.sideLeftAnalysis,
            onScan: () => _pickAndRun(viewModel, (file) => viewModel.scanSideTire(file, isLeft: true)),
          ),
          const SizedBox(height: 12),
          _buildStepCard(
            title: "Step 3B: Right Side Crack",
            subtitle: "Scan right sidewall crack.",
            enabled: viewModel.canScanSide,
            result: viewModel.sideRightAnalysis,
            onScan: () => _pickAndRun(viewModel, (file) => viewModel.scanSideTire(file, isLeft: false)),
          ),
          const SizedBox(height: 20),
          SizedBox(
            width: double.infinity,
            child: FilledButton.icon(
              onPressed: viewModel.canSaveReport && !viewModel.isLoading
                  ? () async {
                      final ok = await viewModel.saveCurrentTireHealthReport();
                      if (!mounted) return;
                      ScaffoldMessenger.of(context).showSnackBar(
                        SnackBar(content: Text(ok ? 'Tire health report saved.' : (viewModel.error ?? 'Failed to save report.'))),
                      );
                      if (ok) {
                        viewModel.selectTab(NavigationTab.history);
                      }
                    }
                  : null,
              icon: const Icon(Icons.save),
              label: const Text("Save Tire Health Report"),
            ),
          )
        ],
      ),
    );
  }

  Widget _buildStepCard({
    required String title,
    required String subtitle,
    required bool enabled,
    required Map<String, dynamic>? result,
    required Future<void> Function() onScan,
  }) {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(14.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(title, style: const TextStyle(fontWeight: FontWeight.bold)),
            const SizedBox(height: 4),
            Text(subtitle, style: const TextStyle(color: Colors.white70)),
            const SizedBox(height: 10),
            if (result != null) _buildResultSummary(result),
            const SizedBox(height: 10),
            OutlinedButton.icon(
              onPressed: enabled ? onScan : null,
              icon: const Icon(Icons.camera_alt),
              label: Text(result == null ? 'Capture & Scan' : 'Rescan'),
            )
          ],
        ),
      ),
    );
  }

  Widget _buildResultSummary(Map<String, dynamic> result) {
    final mode = (result['mode'] ?? '').toString().toUpperCase();
    if (mode == 'FRONT') {
      final tread = result['tread']?['status'] ?? 'n/a';
      final wear = result['wear']?['status'] ?? 'n/a';
      final alignment = result['alignment']?['status'] ?? 'n/a';
      final score = result['overall_score']?.toString() ?? '--';
      return Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text('Tread: $tread'),
          Text('Wear: $wear'),
          Text('Alignment: $alignment'),
          Text('Score: $score'),
        ],
      );
    }

    final cracks = result['cracks'] ?? result;
    final count = cracks['count']?.toString() ?? '0';
    final status = cracks['status']?.toString() ?? 'n/a';
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text('Crack Status: $status'),
        Text('Detected Cracks: $count'),
      ],
    );
  }

  Future<void> _pickAndRun(
    IntelliInflateViewModel viewModel,
    Future<bool> Function(XFile file) runner,
  ) async {
    final image = await _picker.pickImage(source: ImageSource.camera, imageQuality: 75);
    if (image == null) return;
    final success = await runner(image);
    if (!mounted || success) return;
    showDialog<void>(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('Scan failed'),
        content: Text(viewModel.error ?? 'Unable to complete scan.'),
        actions: [
          TextButton(onPressed: () => Navigator.pop(context), child: const Text('OK')),
        ],
      ),
    );
  }
}
