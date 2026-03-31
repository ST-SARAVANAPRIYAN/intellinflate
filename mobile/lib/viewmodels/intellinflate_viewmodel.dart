
import 'package:flutter/material.dart';
import 'package:image_picker/image_picker.dart';
import '../models/intellinflate_models.dart';
import '../services/api_service.dart';

enum NavigationTab { dashboard, vehicleDetection, tireScan, history }

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
  
  final Map<TirePosition, TireScanResult> _tireScanResults = {};
  Map<TirePosition, TireScanResult> get tireScanResults => _tireScanResults;

  final List<String> _operationLogs = [];
  List<String> get operationLogs => List.unmodifiable(_operationLogs);

  void selectTab(NavigationTab tab) {
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
      _currentUser = User.fromJson(response['user']);
      _log('User ${_currentUser?.username ?? 'unknown'} logged in');
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
    _tireScanResults.clear();
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

  Future<bool> scanTire(TirePosition position, XFile imageFile) async {
    _setLoading(true);
    _clearError();
    try {
      // mode could be 'SIDE' or 'FRONT' based on position or user choice
      // Typically sidewall for cracks, front for tread
      String mode = (position == TirePosition.frontLeft || position == TirePosition.frontRight) ? 'FRONT' : 'SIDE';
      final result = await _apiService.analyzeTire(imageFile, mode);
      _tireScanResults[position] = result;
      _log('Tire scan completed for ${position.displayName} (${result.overallCondition.overallStatus})');
      _setLoading(false);
      return true;
    } catch (e) {
      _error = _formatError(e);
      _log('Tire scan failed for ${position.displayName}: $_error');
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
