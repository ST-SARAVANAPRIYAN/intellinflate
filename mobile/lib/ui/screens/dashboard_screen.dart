
import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../../viewmodels/intellinflate_viewmodel.dart';

class DashboardScreen extends StatelessWidget {
  const DashboardScreen({super.key});

  @override
  Widget build(BuildContext context) {
    final viewModel = Provider.of<IntelliInflateViewModel>(context);
    final user = viewModel.currentUser;

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
          const Text("Recent Health Overview", style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold)),
          const SizedBox(height: 12),
          _buildSummaryCard(viewModel),
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

  Widget _buildSummaryCard(IntelliInflateViewModel viewModel) {
    final scanCount = viewModel.tireScanResults.length;
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(16.0),
        child: Column(
          children: [
            ListTile(
              leading: const Icon(Icons.analytics, color: Colors.orange),
              title: const Text("Tire Scans Completed"),
              trailing: Text("$scanCount / 4", style: const TextStyle(fontSize: 18, fontWeight: FontWeight.bold)),
            ),
            const Divider(),
            if (scanCount == 0)
              const Padding(
                padding: EdgeInsets.all(8.0),
                child: Text("No scan data available. Start a new scan to see health reports."),
              )
            else
              ...viewModel.tireScanResults.entries.map((e) => ListTile(
                dense: true,
                title: Text(e.key.toString().split('.').last),
                subtitle: Text("Score: ${e.value.overallCondition.overallScore}"),
                trailing: Icon(Icons.check_circle, color: Colors.greenAccent.shade400),
              )).toList(),
          ],
        ),
      ),
    );
  }
}

class RoundedCornerShape extends RoundedRectangleBorder {
  RoundedCornerShape(double radius) : super(borderRadius: BorderRadius.all(Radius.circular(radius)));
}
