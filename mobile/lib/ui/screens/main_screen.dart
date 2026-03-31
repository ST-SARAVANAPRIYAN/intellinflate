
import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../../viewmodels/intellinflate_viewmodel.dart';
import 'login_screen.dart';
import 'dashboard_screen.dart';
import 'vehicle_detection_screen.dart';
import 'tire_scan_screen.dart';
import 'history_screen.dart';

class IntelliInflateMainScreen extends StatelessWidget {
  const IntelliInflateMainScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return Consumer<IntelliInflateViewModel>(
      builder: (context, viewModel, child) {
        if (viewModel.error != null) {
          WidgetsBinding.instance.addPostFrameCallback((_) {
            final messenger = ScaffoldMessenger.of(context);
            messenger.hideCurrentSnackBar();
            messenger.showSnackBar(SnackBar(content: Text(viewModel.error!)));
            viewModel.clearError();
          });
        }

        if (viewModel.currentUser == null) {
          return const LoginScreen();
        }

        return Scaffold(
          appBar: AppBar(
            title: const Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text("IntelliInflate", style: TextStyle(fontWeight: FontWeight.bold)),
                Text("Tire Health Monitoring", style: TextStyle(fontSize: 12)),
              ],
            ),
            backgroundColor: Theme.of(context).colorScheme.primaryContainer,
            actions: [
              IconButton(
                icon: const Icon(Icons.logout),
                onPressed: () async {
                  final shouldLogout = await showDialog<bool>(
                    context: context,
                    builder: (context) => AlertDialog(
                      title: const Text('Sign out?'),
                      content: const Text('Your active session will be ended on this device.'),
                      actions: [
                        TextButton(onPressed: () => Navigator.pop(context, false), child: const Text('Cancel')),
                        FilledButton(onPressed: () => Navigator.pop(context, true), child: const Text('Sign out')),
                      ],
                    ),
                  );
                  if (shouldLogout == true) {
                    viewModel.logout();
                  }
                },
              ),
            ],
          ),
          body: AnimatedSwitcher(
            duration: const Duration(milliseconds: 250),
            child: KeyedSubtree(
              key: ValueKey(viewModel.selectedTab),
              child: _buildBody(viewModel),
            ),
          ),
          bottomNavigationBar: NavigationBar(
            selectedIndex: viewModel.selectedTab.index,
            onDestinationSelected: (index) {
              viewModel.selectTab(NavigationTab.values[index]);
            },
            destinations: const [
              NavigationDestination(icon: Icon(Icons.dashboard), label: 'Dashboard'),
              NavigationDestination(icon: Icon(Icons.directions_car), label: 'Vehicle'),
              NavigationDestination(icon: Icon(Icons.scanner), label: 'Health Scan'),
              NavigationDestination(icon: Icon(Icons.history), label: 'History'),
            ],
          ),
        );
      },
    );
  }

  Widget _buildBody(IntelliInflateViewModel viewModel) {
    switch (viewModel.selectedTab) {
      case NavigationTab.dashboard:
        return const DashboardScreen();
      case NavigationTab.vehicleDetection:
        return const VehicleDetectionScreen();
      case NavigationTab.tireScan:
        return const TireScanScreen();
      case NavigationTab.history:
        return const HistoryScreen();
    }
  }
}
