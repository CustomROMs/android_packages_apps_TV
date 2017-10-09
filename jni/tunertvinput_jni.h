/* DO NOT EDIT THIS FILE - it is machine generated */
#include <jni.h>
/* Header for class com_android_tv_tuner_TunerHal */

#ifndef _Included_com_android_tv_tuner_TunerHal
#define _Included_com_android_tv_tuner_TunerHal
#ifdef __cplusplus
extern "C" {
#endif
#undef com_android_tv_tuner_TunerHal_DEBUG
#define com_android_tv_tuner_TunerHal_DEBUG 0L
#undef com_android_tv_tuner_TunerHal_FILTER_TYPE_OTHER
#define com_android_tv_tuner_TunerHal_FILTER_TYPE_OTHER 0L
#undef com_android_tv_tuner_TunerHal_FILTER_TYPE_AUDIO
#define com_android_tv_tuner_TunerHal_FILTER_TYPE_AUDIO 1L
#undef com_android_tv_tuner_TunerHal_FILTER_TYPE_VIDEO
#define com_android_tv_tuner_TunerHal_FILTER_TYPE_VIDEO 2L
#undef com_android_tv_tuner_TunerHal_FILTER_TYPE_PCR
#define com_android_tv_tuner_TunerHal_FILTER_TYPE_PCR 3L
#undef com_android_tv_tuner_TunerHal_PID_PAT
#define com_android_tv_tuner_TunerHal_PID_PAT 0L
#undef com_android_tv_tuner_TunerHal_PID_ATSC_SI_BASE
#define com_android_tv_tuner_TunerHal_PID_ATSC_SI_BASE 8187L
#undef com_android_tv_tuner_TunerHal_DEFAULT_VSB_TUNE_TIMEOUT_MS
#define com_android_tv_tuner_TunerHal_DEFAULT_VSB_TUNE_TIMEOUT_MS 2000L
#undef com_android_tv_tuner_TunerHal_DEFAULT_QAM_TUNE_TIMEOUT_MS
#define com_android_tv_tuner_TunerHal_DEFAULT_QAM_TUNE_TIMEOUT_MS 4000L
#undef com_android_tv_tuner_TunerHal_DELIVERY_SYSTEM_UNDEFINED
#define com_android_tv_tuner_TunerHal_DELIVERY_SYSTEM_UNDEFINED 0L
#undef com_android_tv_tuner_TunerHal_DELIVERY_SYSTEM_ATSC
#define com_android_tv_tuner_TunerHal_DELIVERY_SYSTEM_ATSC 1L
#undef com_android_tv_tuner_TunerHal_DELIVERY_SYSTEM_DVBC
#define com_android_tv_tuner_TunerHal_DDELIVERY_SYSTEM_DVBC 2L
#undef com_android_tv_tuner_TunerHal_DELIVERY_SYSTEM_DVBS
#define com_android_tv_tuner_TunerHal_DELIVERY_SYSTEM_DVBS 3L
#undef com_android_tv_tuner_TunerHal_DELIVERY_SYSTEM_DVBS2
#define com_android_tv_tuner_TunerHal_DELIVERY_SYSTEM_DVBS2 4L
#undef com_android_tv_tuner_TunerHal_DELIVERY_SYSTEM_DVBT
#define com_android_tv_tuner_TunerHal_DELIVERY_SYSTEM_DVBT 5L
#undef com_android_tv_tuner_TunerHal_DELIVERY_SYSTEM_DVBT2
#define com_android_tv_tuner_TunerHal_DELIVERY_SYSTEM_DVBT2 6L
/*
 * Class:     com_android_tv_tuner_TunerHal
 * Method:    nativeFinalize
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_com_android_tv_tuner_TunerHal_nativeFinalize
  (JNIEnv *, jobject, jlong);

/*
 * Class:     com_android_tv_tuner_TunerHal
 * Method:    nativeTune
 * Signature: (JILjava/lang/String;I)Z
 */
JNIEXPORT jboolean JNICALL Java_com_android_tv_tuner_TunerHal_nativeTune
  (JNIEnv *, jobject, jlong, jint, jstring, jint);

/*
 * Class:     com_android_tv_tuner_TunerHal
 * Method:    nativeAddPidFilter
 * Signature: (JII)V
 */
JNIEXPORT void JNICALL Java_com_android_tv_tuner_TunerHal_nativeAddPidFilter
  (JNIEnv *, jobject, jlong, jint, jint);

/*
 * Class:     com_android_tv_tuner_TunerHal
 * Method:    nativeCloseAllPidFilters
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_com_android_tv_tuner_TunerHal_nativeCloseAllPidFilters
  (JNIEnv *, jobject, jlong);

/*
 * Class:     com_android_tv_tuner_TunerHal
 * Method:    nativeStopTune
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_com_android_tv_tuner_TunerHal_nativeStopTune
  (JNIEnv *, jobject, jlong);

/*
 * Class:     com_android_tv_tuner_TunerHal
 * Method:    nativeWriteInBuffer
 * Signature: (J[BI)I
 */
JNIEXPORT jint JNICALL Java_com_android_tv_tuner_TunerHal_nativeWriteInBuffer
  (JNIEnv *, jobject, jlong, jbyteArray, jint);

/*
 * Class:     com_android_tv_tuner_TunerHal
 * Method:    nativeSetHasPendingTune
 * Signature: (JZ)V
 */
JNIEXPORT void JNICALL Java_com_android_tv_tuner_TunerHal_nativeSetHasPendingTune
  (JNIEnv *, jobject, jlong, jboolean);

/*
 * Class:     com_android_tv_tuner_TunerHal
 * Method:    nativeGetDeliverySystemType
 * Signature: (J)I
 */
JNIEXPORT int JNICALL Java_com_android_tv_tuner_TunerHal_nativeGetDeliverySystemType
  (JNIEnv *, jobject, jlong);

#ifdef __cplusplus
}
#endif
#endif
/* Header for class com_android_tv_tuner_TunerHal_FilterType */

#ifndef _Included_com_android_tv_tuner_TunerHal_FilterType
#define _Included_com_android_tv_tuner_TunerHal_FilterType
#ifdef __cplusplus
extern "C" {
#endif
#ifdef __cplusplus
}
#endif
#endif
