
import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../../viewmodels/intellinflate_viewmodel.dart';

class HistoryScreen extends StatelessWidget {
  const HistoryScreen({super.key});

  @override
  Widget build(BuildContext context) {
    final viewModel = Provider.of<IntelliInflateViewModel>(context);
    final logs = viewModel.operationLogs;

    return Padding(
      padding: const EdgeInsets.all(16),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Text("Service History", style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold)),
          const SizedBox(height: 8),
          const Text("Recent activity and fallback logs from this session."),
          const SizedBox(height: 16),
          Expanded(
            child: logs.isEmpty
                ? const Center(child: Text("No logs yet. Start scanning to generate activity."))
                : ListView.separated(
                    itemCount: logs.length,
                    separatorBuilder: (_, __) => const SizedBox(height: 8),
                    itemBuilder: (context, index) => Card(
                      child: ListTile(
                        dense: true,
                        leading: const Icon(Icons.receipt_long),
                        title: Text(logs[index]),
                      ),
                    ),
                  ),
          ),
          const SizedBox(height: 12),
          SizedBox(
            width: double.infinity,
            child: ElevatedButton.icon(
              onPressed: () {
                ScaffoldMessenger.of(context).showSnackBar(
                  const SnackBar(content: Text('History sync queued. Server endpoint can be connected next.')),
                );
              },
              icon: const Icon(Icons.sync),
              label: const Text("Sync History"),
            ),
          )
        ],
      ),
    );
  }
}
