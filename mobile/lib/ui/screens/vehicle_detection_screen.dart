
import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:image_picker/image_picker.dart';
import '../../viewmodels/intellinflate_viewmodel.dart';

class VehicleDetectionScreen extends StatefulWidget {
  const VehicleDetectionScreen({super.key});

  @override
  State<VehicleDetectionScreen> createState() => _VehicleDetectionScreenState();
}

class _VehicleDetectionScreenState extends State<VehicleDetectionScreen> {
  final ImagePicker _picker = ImagePicker();

  @override
  Widget build(BuildContext context) {
    final viewModel = Provider.of<IntelliInflateViewModel>(context);
    final result = viewModel.vehicleDetectionResult;

    return SingleChildScrollView(
      padding: const EdgeInsets.all(16.0),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Text("Vehicle Identification", style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold)),
          const SizedBox(height: 16),
          AnimatedSwitcher(
            duration: const Duration(milliseconds: 250),
            child: result == null
                ? _buildEmptyState(viewModel)
                : _buildResultView(viewModel, result),
          ),
        ],
      ),
    );
  }

  Widget _buildEmptyState(IntelliInflateViewModel viewModel) {
    return Center(
      child: Column(
        children: [
          const Icon(Icons.camera_alt, size: 80, color: Colors.blue),
          const SizedBox(height: 24),
          const Text("Identify vehicle via Number Plate", style: TextStyle(fontSize: 16)),
          const SizedBox(height: 24),
          ElevatedButton.icon(
            onPressed: viewModel.isLoading ? null : () => _pickAndDetect(viewModel),
            icon: const Icon(Icons.search),
            label: viewModel.isLoading
                ? const SizedBox(width: 16, height: 16, child: CircularProgressIndicator(strokeWidth: 2))
                : const Text("Capture & Detect"),
          ),
        ],
      ),
    );
  }

  Widget _buildResultView(IntelliInflateViewModel viewModel, result) {
    return Column(
      children: [
        Card(
          elevation: 4,
          child: Padding(
            padding: const EdgeInsets.all(16.0),
            child: Column(
              children: [
                const Icon(Icons.check_circle, size: 48, color: Colors.green),
                const SizedBox(height: 16),
                Text(
                  result.detectedPlate ?? "NO PLATE DETECTED",
                  style: const TextStyle(fontSize: 28, fontWeight: FontWeight.bold, letterSpacing: 2),
                ),
                Text("Confidence: ${(((result.confidence ?? 0) * 100).clamp(0, 100)).toStringAsFixed(0)}%"),
                const Divider(height: 32),
                if (result.user != null) ...[
                  ListTile(
                    leading: const Icon(Icons.person),
                    title: const Text("Owner"),
                    subtitle: Text(result.user['username'] ?? "Unknown"),
                  ),
                  ListTile(
                    leading: const Icon(Icons.car_rental),
                    title: const Text("Model"),
                    subtitle: Text(result.user['vehicleModel'] ?? "Unknown"),
                  ),
                ] else
                  const Text("No registered user found for this plate.", style: TextStyle(color: Colors.orange)),
              ],
            ),
          ),
        ),
        const SizedBox(height: 24),
        Row(
          mainAxisAlignment: MainAxisAlignment.spaceEvenly,
          children: [
            OutlinedButton(onPressed: () => _pickAndDetect(viewModel), child: const Text("RE-SCAN")),
            ElevatedButton(onPressed: () => viewModel.selectTab(NavigationTab.tireScan), child: const Text("PROCEED TO HEALTH SCAN")),
          ],
        )
      ],
    );
  }

  Future<void> _pickAndDetect(IntelliInflateViewModel viewModel) async {
    final XFile? image = await _picker.pickImage(source: ImageSource.camera, imageQuality: 75);
    if (image != null) {
      final success = await viewModel.detectVehicle(image);
      if (!mounted) return;
      if (!success) {
        showDialog<void>(
          context: context,
          builder: (context) => AlertDialog(
            title: const Text('Detection failed'),
            content: Text(viewModel.error ?? 'Unable to detect vehicle. Please retry with a clearer image.'),
            actions: [
              TextButton(onPressed: () => Navigator.pop(context), child: const Text('OK')),
            ],
          ),
        );
      }
    }
  }
}
