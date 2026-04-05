import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../../viewmodels/intellinflate_viewmodel.dart';

class AdminUsersScreen extends StatefulWidget {
  const AdminUsersScreen({super.key});

  @override
  State<AdminUsersScreen> createState() => _AdminUsersScreenState();
}

class _AdminUsersScreenState extends State<AdminUsersScreen> {
  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      final viewModel = context.read<IntelliInflateViewModel>();
      viewModel.loadManagedUsers();
    });
  }

  @override
  Widget build(BuildContext context) {
    final viewModel = context.watch<IntelliInflateViewModel>();
    final users = viewModel.managedUsers;

    return Scaffold(
      body: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Text(
              'Admin User Management',
              style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold),
            ),
            const SizedBox(height: 8),
            const Text('Create, update, and delete user accounts.'),
            const SizedBox(height: 10),
            Align(
              alignment: Alignment.centerRight,
              child: OutlinedButton.icon(
                onPressed: viewModel.isLoading ? null : () => viewModel.loadManagedUsers(),
                icon: const Icon(Icons.refresh),
                label: const Text('Refresh'),
              ),
            ),
            const SizedBox(height: 8),
            Expanded(
              child: users.isEmpty
                  ? const Center(child: Text('No users found.'))
                  : ListView.separated(
                      itemCount: users.length,
                      separatorBuilder: (_, __) => const SizedBox(height: 8),
                      itemBuilder: (context, index) {
                        final user = users[index];
                        final userId = user['id']?.toString() ?? '';
                        final role = user['role']?.toString() ?? 'user';
                        final subtitle = '${user['email'] ?? ''}\nPlate: ${user['numberPlate'] ?? ''}';

                        return Card(
                          child: ListTile(
                            leading: CircleAvatar(
                              child: Text(role == 'admin' ? 'A' : 'U'),
                            ),
                            title: Text('${user['username'] ?? 'Unknown'} (${role.toUpperCase()})'),
                            subtitle: Text(subtitle),
                            isThreeLine: true,
                            trailing: PopupMenuButton<String>(
                              onSelected: (value) async {
                                if (value == 'edit') {
                                  await _showUserDialog(context, viewModel, existing: user);
                                  return;
                                }
                                if (value == 'delete') {
                                  final shouldDelete = await showDialog<bool>(
                                    context: context,
                                    builder: (context) => AlertDialog(
                                      title: const Text('Delete user?'),
                                      content: Text('Delete ${user['username']} permanently?'),
                                      actions: [
                                        TextButton(
                                          onPressed: () => Navigator.pop(context, false),
                                          child: const Text('Cancel'),
                                        ),
                                        FilledButton(
                                          onPressed: () => Navigator.pop(context, true),
                                          child: const Text('Delete'),
                                        ),
                                      ],
                                    ),
                                  );
                                  if (shouldDelete == true && userId.isNotEmpty) {
                                    await viewModel.deleteManagedUser(userId);
                                  }
                                }
                              },
                              itemBuilder: (context) => const [
                                PopupMenuItem(value: 'edit', child: Text('Edit')),
                                PopupMenuItem(value: 'delete', child: Text('Delete')),
                              ],
                            ),
                          ),
                        );
                      },
                    ),
            ),
          ],
        ),
      ),
      floatingActionButton: FloatingActionButton.extended(
        onPressed: viewModel.isLoading ? null : () => _showUserDialog(context, viewModel),
        icon: const Icon(Icons.person_add),
        label: const Text('Add User'),
      ),
    );
  }

  Future<void> _showUserDialog(
    BuildContext context,
    IntelliInflateViewModel viewModel, {
    Map<String, dynamic>? existing,
  }) async {
    final formKey = GlobalKey<FormState>();
    final usernameController = TextEditingController(text: existing?['username']?.toString() ?? '');
    final emailController = TextEditingController(text: existing?['email']?.toString() ?? '');
    final plateController = TextEditingController(text: existing?['numberPlate']?.toString() ?? '');
    final modelController = TextEditingController(text: existing?['vehicleModel']?.toString() ?? '');
    final phoneController = TextEditingController(text: existing?['phone']?.toString() ?? '');
    final passwordController = TextEditingController();
    String role = (existing?['role']?.toString() ?? 'user').toLowerCase();

    await showDialog<void>(
      context: context,
      builder: (context) {
        return StatefulBuilder(
          builder: (context, setState) {
            return AlertDialog(
              title: Text(existing == null ? 'Create User' : 'Edit User'),
              content: SizedBox(
                width: 420,
                child: Form(
                  key: formKey,
                  child: SingleChildScrollView(
                    child: Column(
                      mainAxisSize: MainAxisSize.min,
                      children: [
                        TextFormField(
                          controller: usernameController,
                          decoration: const InputDecoration(labelText: 'Username'),
                          validator: (value) => value == null || value.trim().isEmpty ? 'Required' : null,
                        ),
                        TextFormField(
                          controller: emailController,
                          decoration: const InputDecoration(labelText: 'Email'),
                          validator: (value) => value == null || value.trim().isEmpty ? 'Required' : null,
                        ),
                        TextFormField(
                          controller: plateController,
                          decoration: const InputDecoration(labelText: 'Number Plate'),
                          validator: (value) => value == null || value.trim().isEmpty ? 'Required' : null,
                        ),
                        TextFormField(
                          controller: modelController,
                          decoration: const InputDecoration(labelText: 'Vehicle Model (optional)'),
                        ),
                        TextFormField(
                          controller: phoneController,
                          decoration: const InputDecoration(labelText: 'Phone (optional)'),
                        ),
                        DropdownButtonFormField<String>(
                          value: role,
                          decoration: const InputDecoration(labelText: 'Role'),
                          items: const [
                            DropdownMenuItem(value: 'user', child: Text('User')),
                            DropdownMenuItem(value: 'admin', child: Text('Admin')),
                          ],
                          onChanged: (value) {
                            if (value == null) return;
                            setState(() {
                              role = value;
                            });
                          },
                        ),
                        TextFormField(
                          controller: passwordController,
                          obscureText: true,
                          decoration: InputDecoration(
                            labelText: existing == null
                                ? 'Password'
                                : 'Password (leave blank to keep current)',
                          ),
                          validator: (value) {
                            if (existing == null && (value == null || value.trim().isEmpty)) {
                              return 'Required';
                            }
                            return null;
                          },
                        ),
                      ],
                    ),
                  ),
                ),
              ),
              actions: [
                TextButton(
                  onPressed: () => Navigator.pop(context),
                  child: const Text('Cancel'),
                ),
                FilledButton(
                  onPressed: () async {
                    if (!(formKey.currentState?.validate() ?? false)) {
                      return;
                    }

                    final success = existing == null
                        ? await viewModel.createManagedUser(
                            username: usernameController.text.trim(),
                            email: emailController.text.trim(),
                            password: passwordController.text,
                            numberPlate: plateController.text.trim(),
                            vehicleModel: modelController.text.trim().isEmpty ? null : modelController.text.trim(),
                            phone: phoneController.text.trim().isEmpty ? null : phoneController.text.trim(),
                            role: role,
                          )
                        : await viewModel.updateManagedUser(
                            userId: existing['id'].toString(),
                            username: usernameController.text.trim(),
                            email: emailController.text.trim(),
                            numberPlate: plateController.text.trim(),
                            vehicleModel: modelController.text.trim().isEmpty ? null : modelController.text.trim(),
                            phone: phoneController.text.trim().isEmpty ? null : phoneController.text.trim(),
                            role: role,
                            password: passwordController.text.trim().isEmpty ? null : passwordController.text,
                          );

                    if (!mounted) return;
                    if (success) {
                      Navigator.pop(context);
                    }
                  },
                  child: Text(existing == null ? 'Create' : 'Save'),
                ),
              ],
            );
          },
        );
      },
    );

    usernameController.dispose();
    emailController.dispose();
    plateController.dispose();
    modelController.dispose();
    phoneController.dispose();
    passwordController.dispose();
  }
}
