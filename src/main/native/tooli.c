#include "jvmti.h"
#include "tooli.h"
#include <string.h>

typedef struct {
  jvmtiEnv *jvmti;
} GlobalAgentData;
static GlobalAgentData *globalData = NULL;

jint throwException(JNIEnv *env, const char *name, const char *msg) {
  jclass clazz;
  clazz = (*env)->FindClass(env, name);
  return (*env)->ThrowNew(env, clazz, msg);
}

jvmtiEnv *getJvmToolingInterface(JavaVM *jvm) {
  int err;
  jvmtiEnv *jvmti;
  if (!jvm) {
    return NULL;
  }
  err = (*jvm)->GetEnv(jvm, (void **)&jvmti, JVMTI_VERSION_1_0);
  if (err != JNI_OK) {
    return NULL;
  }
  return jvmti;
}

jboolean InitializeAgent(JavaVM *jvm) {
  static GlobalAgentData data;
  if (!globalData) {
    memset((void*)&data, 0, sizeof(data));
    globalData = &data;
    globalData->jvmti = getJvmToolingInterface(jvm);
  }
  if (globalData->jvmti) {
    return JNI_TRUE;
  } else {
    return JNI_FALSE;
  }
}

void initialize(jvmtiCapabilities *capabilities, const char *options)
{
    capabilities->can_tag_objects = 1;
}

jboolean isInitialized(jvmtiCapabilities *capabilities, const char *options)
{
  if (capabilities->can_tag_objects) return JNI_TRUE;
  return JNI_FALSE;
}

JNIEXPORT jint JNICALL
Agent_OnLoad(JavaVM *jvm, char *options, void *reserved)
{
  jvmtiCapabilities tiCapa;
  jvmtiError tiErr;
  if (InitializeAgent(jvm)) {
    memset((void*)&tiCapa, 0, sizeof(tiCapa));
    initialize(&tiCapa, options);
    tiErr = (*(globalData->jvmti))->AddCapabilities(globalData->jvmti, &tiCapa);
    if (tiErr != JVMTI_ERROR_NONE) {
      if (tiErr == JVMTI_ERROR_NOT_AVAILABLE) {
        printf("ERROR: The requested capabilities are not available.");
      } else {
        printf("ERROR: JVMTI error code: 0x%x\n", tiErr);
      }
    }
  } else {
    printf("ERROR: Could not load JVM Tooling Interface.\n");
  }
  return 0;
}

JavaVM *getJavaVM(JNIEnv *env) {
  int err;
  JavaVM *jvm;
  err = (*env)->GetJavaVM(env, &jvm);
  if (err != 0) {
    throwException(env, "java/lang/RuntimeException", // TODO: better exception
                   "Could not access the JavaVM.");
    return NULL;
  }
  return jvm;
}

JNIEXPORT jboolean JNICALL
Java_org_neo4j_deepintrospect_ToolingInterface_setupNative
(JNIEnv *env, jclass cls, jstring options)
{
  jvmtiCapabilities capa;
  jvmtiError tiErr;
  if (!InitializeAgent(getJavaVM(env))) return JNI_FALSE;
  memset((void*)&capa, 0, sizeof(capa));
  tiErr = (*(globalData->jvmti))->GetCapabilities(globalData->jvmti, &capa);
  if (!tiErr) {
    const char *opts = (options == NULL) ? NULL :
      (*env)->GetStringUTFChars(env, options, NULL);
    if (isInitialized(&capa, opts)) {
      return JNI_TRUE;
    }
    memset((void*)&capa, 0, sizeof(capa));
    initialize(&capa, opts);
    (*env)->ReleaseStringUTFChars(env, options, opts);
    tiErr = (*(globalData->jvmti))->AddCapabilities(globalData->jvmti, &capa);
    if (tiErr == JVMTI_ERROR_NONE) {
      return JNI_TRUE;
    } else {
      return JNI_FALSE;
    }
  } else {
    return JNI_FALSE;
  }
}

typedef struct {
  jlong size;
  jlong count;
} InstanceCounter;

jvmtiIterationControl JNICALL sum_object_sizes
(jvmtiObjectReferenceKind reference_kind, jlong class_tag, 
 jlong size, jlong* tag_ptr, jlong referrer_tag, 
 jint referrer_index, void* user_data)
{
  if (reference_kind != JVMTI_REFERENCE_FIELD &&
      reference_kind != JVMTI_REFERENCE_ARRAY_ELEMENT)
    return JVMTI_ITERATION_IGNORE;

  ((InstanceCounter*)user_data)->size += size;
  ((InstanceCounter*)user_data)->count += 1;

  return JVMTI_ITERATION_CONTINUE;
}

JNIEXPORT jobject JNICALL
Java_org_neo4j_deepintrospect_ToolingInterface_getTransitiveSize
(JNIEnv *env, jobject this, jobject obj)
{
  jclass clazz;
  jmethodID init;
  InstanceCounter result;
  result.size = 0;
  result.count = 0;

  (*(globalData->jvmti))->IterateOverObjectsReachableFromObject(
     globalData->jvmti, obj, &sum_object_sizes, (void*)&result );
  
  clazz = (*env)->FindClass(env, "org/neo4j/deepintrospect/SizeCount");
  init = (*env)->GetMethodID(env, clazz, "<init>", "(JJ)V");

  return (*env)->NewObject(env, clazz, init, result.size, result.count);
}
