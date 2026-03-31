// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'intellinflate_models.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

User _$UserFromJson(Map<String, dynamic> json) => User(
      id: json['id'] as String,
      username: json['username'] as String,
      email: json['email'] as String,
      numberPlate: json['numberPlate'] as String,
      vehicleModel: json['vehicleModel'] as String?,
      phone: json['phone'] as String?,
    );

Map<String, dynamic> _$UserToJson(User instance) => <String, dynamic>{
      'id': instance.id,
      'username': instance.username,
      'email': instance.email,
      'numberPlate': instance.numberPlate,
      'vehicleModel': instance.vehicleModel,
      'phone': instance.phone,
    };

TireScanResult _$TireScanResultFromJson(Map<String, dynamic> json) =>
    TireScanResult(
      scanId: json['scanId'] as String,
      timestamp: (json['timestamp'] as num).toInt(),
      tirePosition: $enumDecode(_$TirePositionEnumMap, json['tirePosition']),
      images: (json['images'] as List<dynamic>)
          .map((e) => TireImage.fromJson(e as Map<String, dynamic>))
          .toList(),
      crackDetection: CrackDetectionResult.fromJson(
          json['crackDetection'] as Map<String, dynamic>),
      wearAnalysis: WearAnalysisResult.fromJson(
          json['wearAnalysis'] as Map<String, dynamic>),
      sidewallAnalysis: SidewallAnalysisResult.fromJson(
          json['sidewallAnalysis'] as Map<String, dynamic>),
      treadAnalysis: TreadAnalysisResult.fromJson(
          json['treadAnalysis'] as Map<String, dynamic>),
      overallCondition: TireConditionAssessment.fromJson(
          json['overallCondition'] as Map<String, dynamic>),
      processingTime: (json['processingTime'] as num).toInt(),
      aiModelVersion: json['aiModelVersion'] as String,
    );

Map<String, dynamic> _$TireScanResultToJson(TireScanResult instance) =>
    <String, dynamic>{
      'scanId': instance.scanId,
      'timestamp': instance.timestamp,
      'tirePosition': _$TirePositionEnumMap[instance.tirePosition]!,
      'images': instance.images,
      'crackDetection': instance.crackDetection,
      'wearAnalysis': instance.wearAnalysis,
      'sidewallAnalysis': instance.sidewallAnalysis,
      'treadAnalysis': instance.treadAnalysis,
      'overallCondition': instance.overallCondition,
      'processingTime': instance.processingTime,
      'aiModelVersion': instance.aiModelVersion,
    };

const _$TirePositionEnumMap = {
  TirePosition.frontLeft: 'FRONT_LEFT',
  TirePosition.frontRight: 'FRONT_RIGHT',
  TirePosition.rearLeft: 'REAR_LEFT',
  TirePosition.rearRight: 'REAR_RIGHT',
};

TireImage _$TireImageFromJson(Map<String, dynamic> json) => TireImage(
      imageId: json['imageId'] as String,
      imageUrl: json['imageUrl'] as String?,
      imageBase64: json['imageBase64'] as String?,
      captureAngle: json['captureAngle'] as String,
      imageType: json['imageType'] as String,
      width: (json['width'] as num).toInt(),
      height: (json['height'] as num).toInt(),
      timestamp: (json['timestamp'] as num).toInt(),
    );

Map<String, dynamic> _$TireImageToJson(TireImage instance) => <String, dynamic>{
      'imageId': instance.imageId,
      'imageUrl': instance.imageUrl,
      'imageBase64': instance.imageBase64,
      'captureAngle': instance.captureAngle,
      'imageType': instance.imageType,
      'width': instance.width,
      'height': instance.height,
      'timestamp': instance.timestamp,
    };

CrackDetectionResult _$CrackDetectionResultFromJson(
        Map<String, dynamic> json) =>
    CrackDetectionResult(
      hasCracks: json['hasCracks'] as bool,
      detectedCracks: (json['detectedCracks'] as List<dynamic>)
          .map((e) => DetectedCrack.fromJson(e as Map<String, dynamic>))
          .toList(),
      crackSeverity: json['crackSeverity'] as String,
      totalCrackLength: (json['totalCrackLength'] as num).toDouble(),
      crackDensity: (json['crackDensity'] as num).toDouble(),
      confidence: (json['confidence'] as num).toDouble(),
    );

Map<String, dynamic> _$CrackDetectionResultToJson(
        CrackDetectionResult instance) =>
    <String, dynamic>{
      'hasCracks': instance.hasCracks,
      'detectedCracks': instance.detectedCracks,
      'crackSeverity': instance.crackSeverity,
      'totalCrackLength': instance.totalCrackLength,
      'crackDensity': instance.crackDensity,
      'confidence': instance.confidence,
    };

DetectedCrack _$DetectedCrackFromJson(Map<String, dynamic> json) =>
    DetectedCrack(
      crackId: json['crackId'] as String,
      crackType: json['crackType'] as String,
      location: json['location'] as String,
      length: (json['length'] as num).toDouble(),
      width: (json['width'] as num).toDouble(),
      severity: json['severity'] as String,
      confidence: (json['confidence'] as num).toDouble(),
    );

Map<String, dynamic> _$DetectedCrackToJson(DetectedCrack instance) =>
    <String, dynamic>{
      'crackId': instance.crackId,
      'crackType': instance.crackType,
      'location': instance.location,
      'length': instance.length,
      'width': instance.width,
      'severity': instance.severity,
      'confidence': instance.confidence,
    };

WearAnalysisResult _$WearAnalysisResultFromJson(Map<String, dynamic> json) =>
    WearAnalysisResult(
      wearPattern: json['wearPattern'] as String,
      wearLevel: json['wearLevel'] as String,
      averageTreadDepth: (json['averageTreadDepth'] as num).toDouble(),
      minimumTreadDepth: (json['minimumTreadDepth'] as num).toDouble(),
      wearUniformity: (json['wearUniformity'] as num).toDouble(),
      estimatedRemainingLife:
          json['estimatedRemainingLife'] as Map<String, dynamic>,
      confidence: (json['confidence'] as num).toDouble(),
    );

Map<String, dynamic> _$WearAnalysisResultToJson(WearAnalysisResult instance) =>
    <String, dynamic>{
      'wearPattern': instance.wearPattern,
      'wearLevel': instance.wearLevel,
      'averageTreadDepth': instance.averageTreadDepth,
      'minimumTreadDepth': instance.minimumTreadDepth,
      'wearUniformity': instance.wearUniformity,
      'estimatedRemainingLife': instance.estimatedRemainingLife,
      'confidence': instance.confidence,
    };

SidewallAnalysisResult _$SidewallAnalysisResultFromJson(
        Map<String, dynamic> json) =>
    SidewallAnalysisResult(
      hasDamage: json['hasDamage'] as bool,
      detectedAnomalies: json['detectedAnomalies'] as List<dynamic>,
      sidewallCondition: json['sidewallCondition'] as String,
      confidence: (json['confidence'] as num).toDouble(),
    );

Map<String, dynamic> _$SidewallAnalysisResultToJson(
        SidewallAnalysisResult instance) =>
    <String, dynamic>{
      'hasDamage': instance.hasDamage,
      'detectedAnomalies': instance.detectedAnomalies,
      'sidewallCondition': instance.sidewallCondition,
      'confidence': instance.confidence,
    };

TreadAnalysisResult _$TreadAnalysisResultFromJson(Map<String, dynamic> json) =>
    TreadAnalysisResult(
      hasForeignObjects: json['hasForeignObjects'] as bool,
      detectedObjects: json['detectedObjects'] as List<dynamic>,
      treadPattern: json['treadPattern'] as Map<String, dynamic>,
      confidence: (json['confidence'] as num).toDouble(),
    );

Map<String, dynamic> _$TreadAnalysisResultToJson(
        TreadAnalysisResult instance) =>
    <String, dynamic>{
      'hasForeignObjects': instance.hasForeignObjects,
      'detectedObjects': instance.detectedObjects,
      'treadPattern': instance.treadPattern,
      'confidence': instance.confidence,
    };

TireConditionAssessment _$TireConditionAssessmentFromJson(
        Map<String, dynamic> json) =>
    TireConditionAssessment(
      overallScore: (json['overallScore'] as num).toDouble(),
      overallStatus: json['overallStatus'] as String,
      safetyRating: json['safetyRating'] as String,
      recommendations: json['recommendations'] as List<dynamic>,
      criticality: json['criticality'] as String,
    );

Map<String, dynamic> _$TireConditionAssessmentToJson(
        TireConditionAssessment instance) =>
    <String, dynamic>{
      'overallScore': instance.overallScore,
      'overallStatus': instance.overallStatus,
      'safetyRating': instance.safetyRating,
      'recommendations': instance.recommendations,
      'criticality': instance.criticality,
    };

VehicleDetectionResult _$VehicleDetectionResultFromJson(
        Map<String, dynamic> json) =>
    VehicleDetectionResult(
      stationId: json['stationId'] as String?,
      sessionId: json['sessionId'] as String?,
      success: json['success'] as bool,
      detectedPlate: json['detectedPlate'] as String?,
      confidence: (json['confidence'] as num?)?.toDouble(),
      user: json['user'] as Map<String, dynamic>?,
    );

Map<String, dynamic> _$VehicleDetectionResultToJson(
        VehicleDetectionResult instance) =>
    <String, dynamic>{
      'stationId': instance.stationId,
      'sessionId': instance.sessionId,
      'success': instance.success,
      'detectedPlate': instance.detectedPlate,
      'confidence': instance.confidence,
      'user': instance.user,
    };
