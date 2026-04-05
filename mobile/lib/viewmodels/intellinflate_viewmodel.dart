
import 'package:flutter/material.dart';
import 'package:image_picker/image_picker.dart';
import '../models/intellinflate_models.dart';
import '../services/api_service.dart';

enum NavigationTab { dashboard, vehicleDetection, tireScan, history, admin }

class IntelliInflateViewModel extends ChangeNotifier {
  final ApiService _apiService = ApiService();
  
  bool _isLoading = false;
  bool get isLoading => _isLoading;
  
  String? _error;
  String? get error => _error;
  
  User? _currentUser;
  User? get currentUser => _currentUser;
  
  String? _token;
  String? get token => _token;
  
  NavigationTab _selectedTab = NavigationTab.dashboard;
  NavigationTab get selectedTab => _selectedTab;
  
  VehicleDetectionResult? _vehicleDetectionResult;
  VehicleDetectionResult? get vehicleDetectionResult => _vehicleDetectionResult;

  Map<String, dynamic>? _frontAnalysis;
  Map<String, dynamic>? get frontAnalysis => _frontAnalysis;

  Map<String, dynamic>? _sideLeftAnalysis;
  Map<String, dynamic>? get sideLeftAnalysis => _sideLeftAnalysis;

  Map<String, dynamic>? _sideRightAnalysis;
  Map<String, dynamic>? get sideRightAnalysis => _sideRightAnalysis;

  final List<Map<String, dynamic>> _tireHealthReports = [];
  List<Map<String, dynamic>> get tireHealthReports => List.unmodifiable(_tireHealthReports);

  final List<Map<String, dynamic>> _managedUsers = [];
  List<Map<String, dynamic>> get managedUsers => List.unmodifiable(_managedUsers);

  final List<String> _operationLogs = [];
  List<String> get operationLogs => List.unmodifiable(_operationLogs);

  bool get isAdmin => (_currentUser?.role ?? '').toLowerCase() == 'admin';

  void selectTab(NavigationTab tab) {
    if (tab == NavigationTab.admin && !isAdmin) {
      return;
    }
    _selectedTab = tab;
    _log('Switched to ${tab.name} tab');
    notifyListeners();
  }

  Future<bool> login(String identifier, String password) async {
    _setLoading(true);
    _clearError();
    try {
      final response = await _apiService.login(identifier, password);
      _token = response['token'];
      _apiService.setAuthToken(_token);
      _currentUser = User.fromJson(response['user']);
      _log('User ${_currentUser?.username ?? 'unknown'} logged in');
      await loadTireHealthReports();
      if (isAdmin) {
        await loadManagedUsers();
      } else {
        _managedUsers.clear();
      }
      _setLoading(false);
      return true;
    } catch (e) {
      _error = _formatError(e);
      _log('Login failed: $_error');
      _setLoading(false);
      return false;
    }
  }

  Future<bool> register({
    required String username,
    required String email,
    required String password,
    required String numberPlate,
    String? vehicleModel,
    String? phone,
  }) async {
    _setLoading(true);
    _clearError();
    try {
      await _apiService.register(
        username: username,
        email: email,
        password: password,
        numberPlate: numberPlate,
        vehicleModel: vehicleModel,
        phone: phone,
      );
      _log('Registration completed for $email');
      _setLoading(false);
      return true;
    } catch (e) {
      _error = _formatError(e);
      _log('Registration failed: $_error');
      _setLoading(false);
      return false;
    }
  }

  void logout() {
    _currentUser = null;
    _token = null;
    _apiService.setAuthToken(null);
    _frontAnalysis = null;
    _sideLeftAnalysis = null;
    _sideRightAnalysis = null;
    _tireHealthReports.clear();
    _managedUsers.clear();
    _vehicleDetectionResult = null;
    _selectedTab = NavigationTab.dashboard;
    _log('User logged out');
    notifyListeners();
  }

  Future<bool> detectVehicle(XFile imageFile) async {
    _setLoading(true);
    _clearError();
    try {
      _vehicleDetectionResult = await _apiService.detectVehicle(imageFile);
      _frontAnalysis = null;
      _sideLeftAnalysis = null;
      _sideRightAnalysis = null;
      _log('Vehicle detection completed: ${_vehicleDetectionResult?.detectedPlate ?? 'not found'}');
      _setLoading(false);
      return true;
    } catch (e) {
      _error = _formatError(e);
      _log('Vehicle detection failed: $_error');
      _setLoading(false);
      return false;
    }
  }

  bool get canScanFront => _vehicleDetectionResult?.success == true;
  bool get canScanSide => canScanFront && _frontAnalysis != null;
  bool get canSaveReport => canScanSide && (_sideLeftAnalysis != null || _sideRightAnalysis != null);

  Future<bool> scanFrontTire(XFile imageFile) async {
    _setLoading(true);
    _clearError();
    try {
      if (!canScanFront) {
        _error = 'Detect number plate first.';
        _setLoading(false);
        return false;
      }

      _frontAnalysis = await _apiService.analyzeTire(imageFile, 'FRONT');
      _log('Front tire analysis completed.');
      _setLoading(false);
      return true;
    } catch (e) {
      _error = _formatError(e);
      _log('Front tire scan failed: $_error');
      _setLoading(false);
      return false;
    }
  }

  Future<bool> scanSideTire(XFile imageFile, {required bool isLeft}) async {
    _setLoading(true);
    _clearError();
    try {
      if (!canScanSide) {
        _error = 'Complete front tire scan first.';
        _setLoading(false);
        return false;
      }

      final result = await _apiService.analyzeTire(imageFile, 'SIDE');
      if (isLeft) {
        _sideLeftAnalysis = result;
      } else {
        _sideRightAnalysis = result;
      }
      _log('Side tire ${isLeft ? 'left' : 'right'} analysis completed.');
      _setLoading(false);
      return true;
    } catch (e) {
      _error = _formatError(e);
      _log('Side tire ${isLeft ? 'left' : 'right'} scan failed: $_error');
      _setLoading(false);
      return false;
    }
  }

  Future<bool> saveCurrentTireHealthReport() async {
    _setLoading(true);
    _clearError();
    try {
      final detectedPlate = _vehicleDetectionResult?.detectedPlate;
      if (!canSaveReport || detectedPlate == null || _frontAnalysis == null) {
        _error = 'Complete plate, front, and at least one side scan before saving report.';
        _setLoading(false);
        return false;
      }

      final sideAnalyses = <Map<String, dynamic>>[];
      if (_sideLeftAnalysis != null) sideAnalyses.add(_sideLeftAnalysis!);
      if (_sideRightAnalysis != null) sideAnalyses.add(_sideRightAnalysis!);

      final scanAssets = <Map<String, dynamic>>[];
      final plateImageUrl = _vehicleDetectionResult?.imageUrl?.toString();
      if (plateImageUrl != null && plateImageUrl.isNotEmpty) {
        scanAssets.add({
          'type': 'plate',
          'label': 'Number Plate',
          'imageUrl': plateImageUrl,
        });
      }
      final frontImageUrl = _frontAnalysis?['imageUrl']?.toString();
      if (frontImageUrl != null && frontImageUrl.isNotEmpty) {
        scanAssets.add({
          'type': 'front',
          'label': 'Front Tire',
          'imageUrl': frontImageUrl,
        });
      }
      if (_sideLeftAnalysis != null) {
        final leftImageUrl = _sideLeftAnalysis?['imageUrl']?.toString();
        if (leftImageUrl != null && leftImageUrl.isNotEmpty) {
          scanAssets.add({
            'type': 'side-left',
            'label': 'Left Side Tire',
            'imageUrl': leftImageUrl,
          });
        }
      }
      if (_sideRightAnalysis != null) {
        final rightImageUrl = _sideRightAnalysis?['imageUrl']?.toString();
        if (rightImageUrl != null && rightImageUrl.isNotEmpty) {
          scanAssets.add({
            'type': 'side-right',
            'label': 'Right Side Tire',
            'imageUrl': rightImageUrl,
          });
        }
      }

      final response = await _apiService.saveTireHealthReport(
        numberPlate: detectedPlate,
        plateDetection: {
          'detectedPlate': detectedPlate,
          'confidence': _vehicleDetectionResult?.confidence,
          'user': _vehicleDetectionResult?.user,
        },
        frontAnalysis: _frontAnalysis!,
        sideAnalyses: sideAnalyses,
        scanAssets: scanAssets,
      );

      final report = response['report'];
      if (report is Map) {
        _tireHealthReports.insert(0, Map<String, dynamic>.from(report));
      }
      _log('Tire health report saved for $detectedPlate');
      _setLoading(false);
      notifyListeners();
      return true;
    } catch (e) {
      _error = _formatError(e);
      _log('Failed to save tire health report: $_error');
      _setLoading(false);
      return false;
    }
  }

  Future<void> loadTireHealthReports() async {
    if (_token == null) return;
    try {
      final reports = await _apiService.fetchTireHealthReports(limit: 30);
      _tireHealthReports
        ..clear()
        ..addAll(reports);
      notifyListeners();
    } catch (e) {
      _error = _formatError(e);
      _log('Failed to load reports: $_error');
    }
  }

  Future<void> loadManagedUsers() async {
    if (!isAdmin || _token == null) return;
    try {
      final users = await _apiService.fetchAdminUsers();
      _managedUsers
        ..clear()
        ..addAll(users);
      notifyListeners();
    } catch (e) {
      _error = _formatError(e);
      _log('Failed to load users: $_error');
      notifyListeners();
    }
  }

  Future<bool> createManagedUser({
    required String username,
    required String email,
    required String password,
    required String numberPlate,
    required String role,
    String? vehicleModel,
    String? phone,
  }) async {
    if (!isAdmin) return false;
    _setLoading(true);
    _clearError();
    try {
      final response = await _apiService.createAdminUser(
        username: username,
        email: email,
        password: password,
        numberPlate: numberPlate,
        role: role,
        vehicleModel: vehicleModel,
        phone: phone,
      );

      final user = response['user'];
      if (user is Map) {
        _managedUsers.insert(0, Map<String, dynamic>.from(user));
      }
      _log('Created user $username');
      _setLoading(false);
      notifyListeners();
      return true;
    } catch (e) {
      _error = _formatError(e);
      _log('Create user failed: $_error');
      _setLoading(false);
      return false;
    }
  }

  Future<bool> updateManagedUser({
    required String userId,
    required String username,
    required String email,
    required String numberPlate,
    required String role,
    String? vehicleModel,
    String? phone,
    String? password,
  }) async {
    if (!isAdmin) return false;
    _setLoading(true);
    _clearError();
    try {
      final response = await _apiService.updateAdminUser(
        userId: userId,
        username: username,
        email: email,
        numberPlate: numberPlate,
        role: role,
        vehicleModel: vehicleModel,
        phone: phone,
        password: password,
      );

      final updatedUser = response['user'];
      if (updatedUser is Map) {
        final payload = Map<String, dynamic>.from(updatedUser);
        final idx = _managedUsers.indexWhere(
          (u) => (u['id']?.toString() ?? '') == (payload['id']?.toString() ?? ''),
        );
        if (idx >= 0) {
          _managedUsers[idx] = payload;
        } else {
          _managedUsers.insert(0, payload);
        }
      }
      _log('Updated user $username');
      _setLoading(false);
      notifyListeners();
      return true;
    } catch (e) {
      _error = _formatError(e);
      _log('Update user failed: $_error');
      _setLoading(false);
      return false;
    }
  }

  Future<bool> deleteManagedUser(String userId) async {
    if (!isAdmin) return false;
    _setLoading(true);
    _clearError();
    try {
      await _apiService.deleteAdminUser(userId);
      _managedUsers.removeWhere((u) => (u['id']?.toString() ?? '') == userId);
      _log('Deleted user $userId');
      _setLoading(false);
      notifyListeners();
      return true;
    } catch (e) {
      _error = _formatError(e);
      _log('Delete user failed: $_error');
      _setLoading(false);
      return false;
    }
  }

  void _setLoading(bool value) {
    _isLoading = value;
    notifyListeners();
  }

  void _clearError() {
    _error = null;
    notifyListeners();
  }

  void _log(String message) {
    _operationLogs.insert(0, '[${DateTime.now().toIso8601String()}] $message');
    if (_operationLogs.length > 100) {
      _operationLogs.removeLast();
    }
  }

  String _formatError(Object error) {
    final raw = error.toString();
    if (raw.startsWith('Exception: ')) {
      return raw.replaceFirst('Exception: ', '');
    }
    return raw;
  }
  
  void clearError() {
    _error = null;
    notifyListeners();
  }
}
