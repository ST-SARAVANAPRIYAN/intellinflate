
import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:image_picker/image_picker.dart';
import '../../viewmodels/intellinflate_viewmodel.dart';
import '../../models/intellinflate_models.dart';

class TireScanScreen extends StatefulWidget {
  const TireScanScreen({super.key});

  @override
  State<TireScanScreen> createState() => _TireScanScreenState();
}

class _TireScanScreenState extends State<TireScanScreen> {
  TirePosition? _selectedPosition;
  final ImagePicker _picker = ImagePicker();

  @override
  Widget build(BuildContext context) {
    final viewModel = Provider.of<IntelliInflateViewModel>(context);

    return SingleChildScrollView(
      padding: const EdgeInsets.all(16.0),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Text("Select Tire to Scan", style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold)),
          const SizedBox(height: 16),
          _buildTireGrid(viewModel),
          const SizedBox(height: 24),
          AnimatedSwitcher(
            duration: const Duration(milliseconds: 220),
            child: _selectedPosition != null
                ? _buildScanDetail(viewModel, _selectedPosition!)
                : const Center(child: Text("Select a tire to see details")),
          ),
        ],
      ),
    );
  }

  Widget _buildTireGrid(IntelliInflateViewModel viewModel) {
    return Column(
      children: [
        Row(
          mainAxisAlignment: MainAxisAlignment.spaceEvenly,
          children: [
            _buildTireButton(viewModel, TirePosition.frontLeft),
            _buildTireButton(viewModel, TirePosition.frontRight),
          ],
        ),
        const SizedBox(height: 16),
        Row(
          mainAxisAlignment: MainAxisAlignment.spaceEvenly,
          children: [
            _buildTireButton(viewModel, TirePosition.rearLeft),
            _buildTireButton(viewModel, TirePosition.rearRight),
          ],
        ),
      ],
    );
  }

  Widget _buildTireButton(IntelliInflateViewModel viewModel, TirePosition pos) {
    bool hasResult = viewModel.tireScanResults.containsKey(pos);
    bool isSelected = _selectedPosition == pos;

    return InkWell(
      onTap: () => setState(() => _selectedPosition = pos),
      child: Card(
        color: isSelected ? Theme.of(context).colorScheme.primaryContainer : null,
        elevation: isSelected ? 4 : 1,
        child: Container(
          width: 150,
          padding: const EdgeInsets.symmetric(vertical: 20),
          child: Column(
            children: [
              Icon(
                Icons.circle,
                size: 48,
                color: hasResult ? Colors.greenAccent.shade400 : Colors.grey,
              ),
              const SizedBox(height: 8),
              Text(pos.displayName, style: const TextStyle(fontWeight: FontWeight.bold)),
              if (hasResult)
                const Text("Scanned", style: TextStyle(color: Colors.green, fontSize: 12))
              else
                const Text("Pending", style: TextStyle(color: Colors.grey, fontSize: 12)),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildScanDetail(IntelliInflateViewModel viewModel, TirePosition pos) {
    final result = viewModel.tireScanResults[pos];

    if (result == null) {
      return Center(
        child: Column(
          children: [
            const Icon(Icons.scanner, size: 64, color: Colors.blue),
            const SizedBox(height: 16),
            const Text("No scan data for this tire"),
            const SizedBox(height: 16),
            ElevatedButton.icon(
              onPressed: viewModel.isLoading ? null : () => _pickAndScan(viewModel, pos),
              icon: const Icon(Icons.camera_alt),
              label: const Text("Start Scanning"),
            ),
          ],
        ),
      );
    }

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        _buildOverallCondition(result.overallCondition),
        const SizedBox(height: 16),
        _buildHealthMetricCard("Crack Detection", result.crackDetection.hasCracks ? "Detected" : "None Found", result.crackDetection.confidence),
        const SizedBox(height: 12),
        _buildHealthMetricCard("Wear Analysis", result.wearAnalysis.wearLevel, result.wearAnalysis.confidence),
        const SizedBox(height: 12),
        _buildHealthMetricCard("Sidewall", result.sidewallAnalysis.sidewallCondition, result.sidewallAnalysis.confidence),
        const SizedBox(height: 24),
        SizedBox(
          width: double.infinity,
          child: OutlinedButton.icon(
            onPressed: () => _pickAndScan(viewModel, pos),
            icon: const Icon(Icons.refresh),
            label: const Text("Scan Again"),
          ),
        ),
      ],
    );
  }

  Widget _buildOverallCondition(TireConditionAssessment assessment) {
    return Card(
      color: Colors.blue.shade900,
      child: Padding(
        padding: const EdgeInsets.all(16.0),
        child: Row(
          mainAxisAlignment: MainAxisAlignment.spaceBetween,
          children: [
            Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                const Text("Overall Health", style: TextStyle(color: Colors.white70)),
                Text(assessment.overallStatus, style: const TextStyle(color: Colors.white, fontSize: 24, fontWeight: FontWeight.bold)),
              ],
            ),
            Container(
              padding: const EdgeInsets.all(12),
              decoration: BoxDecoration(color: Colors.white, shape: BoxShape.circle),
              child: Text(
                assessment.overallScore.toInt().toString(),
                style: const TextStyle(fontSize: 24, fontWeight: FontWeight.bold, color: Colors.blue),
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildHealthMetricCard(String title, String status, double confidence) {
    return Card(
      child: ListTile(
        title: Text(title, style: const TextStyle(fontWeight: FontWeight.bold)),
        subtitle: Text("Status: $status"),
        trailing: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            const Text("Conf.", style: TextStyle(fontSize: 10)),
            Text("${(confidence * 100).toInt()}%", style: const TextStyle(fontWeight: FontWeight.bold)),
          ],
        ),
      ),
    );
  }

  Future<void> _pickAndScan(IntelliInflateViewModel viewModel, TirePosition pos) async {
    final XFile? image = await _picker.pickImage(source: ImageSource.camera, imageQuality: 75);
    if (image != null) {
      final success = await viewModel.scanTire(pos, image);
      if (!mounted) return;
      if (!success) {
        showDialog<void>(
          context: context,
          builder: (context) => AlertDialog(
            title: const Text('Scan failed'),
            content: Text(viewModel.error ?? 'Unable to scan this tire now. Please retry.'),
            actions: [
              TextButton(onPressed: () => Navigator.pop(context), child: const Text('OK')),
            ],
          ),
        );
      }
    }
  }
}
