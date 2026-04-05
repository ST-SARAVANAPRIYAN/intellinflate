
import 'package:json_annotation/json_annotation.dart';

part 'intellinflate_models.g.dart';

@JsonSerializable()
class User {
  final String id;
  final String username;
  final String email;
  final String? role;
  final String numberPlate;
  final String? vehicleModel;
  final String? phone;

  User({
    required this.id,
    required this.username,
    required this.email,
    this.role,
    required this.numberPlate,
    this.vehicleModel,
    this.phone,
  });

  factory User.fromJson(Map<String, dynamic> json) => _$UserFromJson(json);
  Map<String, dynamic> json() => _$UserToJson(this);
}

enum VehicleType {
  @JsonValue("MOTORCYCLE") motorcycle,
  @JsonValue("HATCHBACK") hatchback,
  @JsonValue("SEDAN") sedan,
  @JsonValue("SUV") suv,
  @JsonValue("PICKUP_TRUCK") pickup_truck,
  @JsonValue("VAN") van,
  @JsonValue("TRUCK") truck,
  @JsonValue("BUS") bus,
  @JsonValue("UNKNOWN") unknown,
}

enum TirePosition {
  @JsonValue("FRONT_LEFT") frontLeft,
  @JsonValue("FRONT_RIGHT") frontRight,
  @JsonValue("REAR_LEFT") rearLeft,
  @JsonValue("REAR_RIGHT") rearRight,
}

extension TirePositionExtension on TirePosition {
  String get displayName {
    switch (this) {
      case TirePosition.frontLeft: return "Front Left";
      case TirePosition.frontRight: return "Front Right";
      case TirePosition.rearLeft: return "Rear Left";
      case TirePosition.rearRight: return "Rear Right";
    }
  }
}

@JsonSerializable()
class TireScanResult {
  final String scanId;
  final int timestamp;
  final TirePosition tirePosition;
  final List<TireImage> images;
  final CrackDetectionResult crackDetection;
  final WearAnalysisResult wearAnalysis;
  final SidewallAnalysisResult sidewallAnalysis;
  final TreadAnalysisResult treadAnalysis;
  final TireConditionAssessment overallCondition;
  final int processingTime;
  final String aiModelVersion;

  TireScanResult({
    required this.scanId,
    required this.timestamp,
    required this.tirePosition,
    required this.images,
    required this.crackDetection,
    required this.wearAnalysis,
    required this.sidewallAnalysis,
    required this.treadAnalysis,
    required this.overallCondition,
    required this.processingTime,
    required this.aiModelVersion,
  });

  factory TireScanResult.fromJson(Map<String, dynamic> json) => _$TireScanResultFromJson(json);
}

@JsonSerializable()
class TireImage {
  final String imageId;
  final String? imageUrl;
  final String? imageBase64;
  final String captureAngle;
  final String imageType;
  final int width;
  final int height;
  final int timestamp;

  TireImage({
    required this.imageId,
    this.imageUrl,
    this.imageBase64,
    required this.captureAngle,
    required this.imageType,
    required this.width,
    required this.height,
    required this.timestamp,
  });

  factory TireImage.fromJson(Map<String, dynamic> json) => _$TireImageFromJson(json);
}

@JsonSerializable()
class CrackDetectionResult {
  final bool hasCracks;
  final List<DetectedCrack> detectedCracks;
  final String crackSeverity;
  final double totalCrackLength;
  final double crackDensity;
  final double confidence;

  CrackDetectionResult({
    required this.hasCracks,
    required this.detectedCracks,
    required this.crackSeverity,
    required this.totalCrackLength,
    required this.crackDensity,
    required this.confidence,
  });

  factory CrackDetectionResult.fromJson(Map<String, dynamic> json) => _$CrackDetectionResultFromJson(json);
}

@JsonSerializable()
class DetectedCrack {
  final String crackId;
  final String crackType;
  final String location;
  final double length;
  final double width;
  final String severity;
  final double confidence;

  DetectedCrack({
    required this.crackId,
    required this.crackType,
    required this.location,
    required this.length,
    required this.width,
    required this.severity,
    required this.confidence,
  });

  factory DetectedCrack.fromJson(Map<String, dynamic> json) => _$DetectedCrackFromJson(json);
}

@JsonSerializable()
class WearAnalysisResult {
  final String wearPattern;
  final String wearLevel;
  final double averageTreadDepth;
  final double minimumTreadDepth;
  final double wearUniformity;
  final Map<String, dynamic> estimatedRemainingLife;
  final double confidence;

  WearAnalysisResult({
    required this.wearPattern,
    required this.wearLevel,
    required this.averageTreadDepth,
    required this.minimumTreadDepth,
    required this.wearUniformity,
    required this.estimatedRemainingLife,
    required this.confidence,
  });

  factory WearAnalysisResult.fromJson(Map<String, dynamic> json) => _$WearAnalysisResultFromJson(json);
}

@JsonSerializable()
class SidewallAnalysisResult {
  final bool hasDamage;
  final List<dynamic> detectedAnomalies;
  final String sidewallCondition;
  final double confidence;

  SidewallAnalysisResult({
    required this.hasDamage,
    required this.detectedAnomalies,
    required this.sidewallCondition,
    required this.confidence,
  });

  factory SidewallAnalysisResult.fromJson(Map<String, dynamic> json) => _$SidewallAnalysisResultFromJson(json);
}

@JsonSerializable()
class TreadAnalysisResult {
  final bool hasForeignObjects;
  final List<dynamic> detectedObjects;
  final Map<String, dynamic> treadPattern;
  final double confidence;

  TreadAnalysisResult({
    required this.hasForeignObjects,
    required this.detectedObjects,
    required this.treadPattern,
    required this.confidence,
  });

  factory TreadAnalysisResult.fromJson(Map<String, dynamic> json) => _$TreadAnalysisResultFromJson(json);
}

@JsonSerializable()
class TireConditionAssessment {
  final double overallScore;
  final String overallStatus;
  final String safetyRating;
  final List<dynamic> recommendations;
  final String criticality;

  TireConditionAssessment({
    required this.overallScore,
    required this.overallStatus,
    required this.safetyRating,
    required this.recommendations,
    required this.criticality,
  });

  factory TireConditionAssessment.fromJson(Map<String, dynamic> json) => _$TireConditionAssessmentFromJson(json);
}

@JsonSerializable()
class VehicleDetectionResult {
  final String? stationId;
  final String? sessionId;
  final bool success;
  final String? detectedPlate;
  final double? confidence;
  final Map<String, dynamic>? user;
  final String? imageUrl;

  VehicleDetectionResult({
    this.stationId,
    this.sessionId,
    required this.success,
    this.detectedPlate,
    this.confidence,
    this.user,
    this.imageUrl,
  });

  factory VehicleDetectionResult.fromJson(Map<String, dynamic> json) => _$VehicleDetectionResultFromJson(json);
}
