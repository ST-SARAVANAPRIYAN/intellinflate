
import 'dart:async';
import 'package:flutter/foundation.dart';
import 'package:dio/dio.dart';
import 'package:image_picker/image_picker.dart';
import 'package:path/path.dart' as p;
import '../models/intellinflate_models.dart';

class ApiService {
  final Dio _dio = Dio();
  static const String _envApiBaseUrl = String.fromEnvironment('API_BASE_URL', defaultValue: '');
  late final List<String> _baseUrls;
  int _activeBaseUrlIndex = 0;

  ApiService() {
    _baseUrls = _buildBaseUrls();
    _dio.options.baseUrl = _baseUrls.first;
    _dio.options.connectTimeout = const Duration(seconds: 15);
    _dio.options.receiveTimeout = const Duration(seconds: 30);
    _dio.options.sendTimeout = const Duration(seconds: 30);
    _dio.options.headers['Accept'] = 'application/json';
  }

  List<String> _buildBaseUrls() {
    final values = <String>[];
    void addUrl(String value) {
      if (value.trim().isEmpty) return;
      final normalized = value.trim().replaceAll(RegExp(r'/+$'), '');
      if (!values.contains(normalized)) {
        values.add(normalized);
      }
    }

    addUrl(_envApiBaseUrl);

    final browserHost = Uri.base.host.trim();
    if (kIsWeb && browserHost.isNotEmpty && browserHost != 'localhost' && browserHost != '127.0.0.1') {
      addUrl('http://$browserHost:3000');
    }

    addUrl('http://localhost:3000');
    addUrl('http://127.0.0.1:3000');

    if (!kIsWeb) {
      addUrl('http://10.0.2.2:3000');
      addUrl('http://10.0.3.2:3000');
      addUrl('http://192.168.1.100:3000');
    }

    return values;
  }

  bool _isFallbackCandidate(DioException e) {
    final statusCode = e.response?.statusCode ?? 0;
    return _isTransientError(e) || statusCode == 502 || statusCode == 503 || statusCode == 504;
  }

  Future<T> _requestWithFallback<T>(Future<T> Function() task) async {
    DioException? lastError;
    for (int i = 0; i < _baseUrls.length; i++) {
      final candidateIndex = (_activeBaseUrlIndex + i) % _baseUrls.length;
      _dio.options.baseUrl = _baseUrls[candidateIndex];

      try {
        final result = await _withRetry(task);
        _activeBaseUrlIndex = candidateIndex;
        return result;
      } on DioException catch (e) {
        lastError = e;
        if (!_isFallbackCandidate(e)) {
          rethrow;
        }
      }
    }

    throw lastError ?? DioException(
      requestOptions: RequestOptions(path: ''),
      message: 'Unable to reach server using configured fallbacks',
      type: DioExceptionType.connectionError,
    );
  }

  Future<T> _withRetry<T>(Future<T> Function() task) async {
    try {
      return await task();
    } on DioException catch (e) {
      if (_isTransientError(e)) {
        await Future.delayed(const Duration(milliseconds: 500));
        return task();
      }
      rethrow;
    }
  }

  bool _isTransientError(DioException e) {
    return e.type == DioExceptionType.connectionTimeout ||
        e.type == DioExceptionType.receiveTimeout ||
        e.type == DioExceptionType.connectionError;
  }

  Future<MultipartFile> _toMultipartFile(XFile imageFile) async {
    final bytes = await imageFile.readAsBytes();
    final fileName = p.basename(imageFile.path.isEmpty ? 'upload.jpg' : imageFile.path);
    return MultipartFile.fromBytes(bytes, filename: fileName);
  }

  Future<Map<String, dynamic>> login(String identifier, String password) async {
    try {
      final response = await _requestWithFallback(() => _dio.post('/api/login', data: {
            'identifier': identifier,
            'password': password,
          }));
      return response.data;
    } on DioException catch (e) {
      throw _handleError(e);
    }
  }

  Future<Map<String, dynamic>> register({
    required String username,
    required String email,
    required String password,
    required String numberPlate,
    String? vehicleModel,
    String? phone,
  }) async {
    try {
      final response = await _requestWithFallback(() => _dio.post('/api/register', data: {
            'username': username,
            'email': email,
            'password': password,
            'numberPlate': numberPlate,
            'vehicleModel': vehicleModel,
            'phone': phone,
          }));
      return response.data;
    } on DioException catch (e) {
      throw _handleError(e);
    }
  }

  Future<VehicleDetectionResult> detectVehicle(XFile imageFile) async {
    try {
      FormData formData = FormData.fromMap({
        "image": await _toMultipartFile(imageFile),
      });

      final response = await _requestWithFallback(() => _dio.post('/api/vehicle/detect', data: formData));
      return VehicleDetectionResult.fromJson(response.data);
    } on DioException catch (e) {
      throw _handleError(e);
    }
  }

  Future<TireScanResult> analyzeTire(XFile imageFile, String mode) async {
    try {
      FormData formData = FormData.fromMap({
        "image": await _toMultipartFile(imageFile),
        "mode": mode,
      });

      final response = await _requestWithFallback(() => _dio.post('/api/analyze-tire', data: formData));
      return TireScanResult.fromJson(response.data);
    } on DioException catch (e) {
      throw _handleError(e);
    }
  }

  Future<Map<String, dynamic>> getStationInfo() async {
    try {
      final response = await _requestWithFallback(() => _dio.get('/api/station/info'));
      return response.data;
    } on DioException catch (e) {
      throw _handleError(e);
    }
  }

  String _handleError(DioException e) {
    if (e.response != null) {
      final data = e.response?.data;
      if (data is Map<String, dynamic>) {
        return data['error']?.toString() ?? data['message']?.toString() ?? "Server error";
      }
      return "Server error (${e.response?.statusCode ?? 'unknown'})";
    }
    return "Network error: ${e.message ?? 'Unable to reach server'}";
  }
}
