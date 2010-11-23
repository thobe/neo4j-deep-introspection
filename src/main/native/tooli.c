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

jboolean setupNative(JNIEnv *env, jstring options)
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

JNIEXPORT jboolean JNICALL
Java_org_neo4j_deepintrospect_ToolingInterface_setupNative
(JNIEnv *env, jclass cls, jstring options)
{
  jclass type;
  jboolean enabled;

  enabled = setupNative(env, options);

  /*
  if (enabled) {
    type = (*env)->FindClass(env,"org/neo4j/kernel/impl/core/NodeImpl");
    (*(globalData->jvmti))->SetTag(globalData->jvmti, type, 1);

    type = (*env)->FindClass(env,"org/neo4j/kernel/impl/core/RelationshipImpl");
    (*(globalData->jvmti))->SetTag(globalData->jvmti, type, 2);
  }
  */

  return enabled;
}

typedef struct {
  jlong tot_size;
  jlong tot_count;
  jlong tag_size;
  jlong tag_count;
  jlong obj_count;
} InstanceCounter;

jvmtiIterationControl JNICALL sum_object_sizes
(jvmtiObjectReferenceKind reference_kind, jlong class_tag, 
 jlong size, jlong* tag_ptr, jlong referrer_tag, 
 jint referrer_index, void* user_data)
{
  if (reference_kind != JVMTI_REFERENCE_FIELD &&
      reference_kind != JVMTI_REFERENCE_ARRAY_ELEMENT)
    return JVMTI_ITERATION_IGNORE;

  ((InstanceCounter*)user_data)->tot_size += size;
  ((InstanceCounter*)user_data)->tot_count += 1;

  if (class_tag) {
    ((InstanceCounter*)user_data)->obj_count += 1;

    ((InstanceCounter*)user_data)->tag_size += size;
    ((InstanceCounter*)user_data)->tag_count += 1;
    (*tag_ptr) = class_tag;
  } else if (referrer_tag) {
    ((InstanceCounter*)user_data)->tag_size += size;
    ((InstanceCounter*)user_data)->tag_count += 1;
    (*tag_ptr) = referrer_tag;
  }

  return JVMTI_ITERATION_CONTINUE;
}

jvmtiIterationControl JNICALL clear_tag
(jlong class_tag, jlong size, jlong* tag_ptr, void* user_data)
{
  (*tag_ptr) = 0;
  return JVMTI_ITERATION_CONTINUE;
}

JNIEXPORT jobject JNICALL
Java_org_neo4j_deepintrospect_ToolingInterface_getTransitiveSize
(JNIEnv *env, jobject this, jobject obj, jclass type)
{
  jlong TAG = 1;
  jclass clazz;
  jmethodID method;
  jobject java_result;
  jobject* tagged_objects = NULL;
  jint n_tagged_objects = 0;
  int i;

  InstanceCounter result;

  if (obj == NULL) return NULL;
  
  (*(globalData->jvmti))->GetObjectSize(globalData->jvmti,
                                        obj, &(result.tot_size));
  result.tot_count = 1;
  result.tag_size = 0;
  result.tag_count = 0;
  result.obj_count = 0;

  if (type != NULL) {
    (*(globalData->jvmti))->SetTag(globalData->jvmti, type, TAG);
    if ((*env)->IsInstanceOf(env, obj, type)) {
      (*(globalData->jvmti))->SetTag(globalData->jvmti, obj, TAG);
      result.tag_count = 1;
      result.obj_count = 1;
      result.tag_size = result.tot_size;
    }
  }

  (*(globalData->jvmti))->IterateOverObjectsReachableFromObject(
     globalData->jvmti, obj, &sum_object_sizes, (void*)&result );

  if (type != NULL) {
    (*(globalData->jvmti))->SetTag(globalData->jvmti, type, 0);
    if ((*env)->IsInstanceOf(env, obj, type)) {
      (*(globalData->jvmti))->GetObjectsWithTags(globalData->jvmti, 1, &TAG,
                                                 &n_tagged_objects,
                                                 &tagged_objects, NULL );
    }
  }

  clazz = (*env)->FindClass(env, "org/neo4j/deepintrospect/SizeCount");
  method = (*env)->GetMethodID(env, clazz, "<init>", "(JJJJJ)V");

  java_result = (*env)->NewObject(env, clazz, method,
                                  result.tot_size, result.tot_count, 
                                  result.tag_size, result.tag_count,
                                  result.obj_count);

  method = (*env)->GetMethodID(env,clazz,"specSize","(Ljava/lang/Object;JJ)V");
    
  (*(globalData->jvmti))->IterateOverHeap(
     globalData->jvmti, JVMTI_HEAP_OBJECT_TAGGED, clear_tag, NULL );

  for ( i = 0; i < n_tagged_objects; i++ ) {
    result.tot_size = 0;
    result.tot_count = 0;
    result.tag_size = 0;
    result.tag_count = 0;
    result.obj_count = 0;

    (*(globalData->jvmti))->IterateOverObjectsReachableFromObject(
       globalData->jvmti, tagged_objects[i], &sum_object_sizes, (void*)&result);

    (*env)->CallVoidMethod(env, java_result, method, tagged_objects[i],
                           result.tot_size, result.tot_count);
  }

  if (tagged_objects)
    (*(globalData->jvmti))->Deallocate( globalData->jvmti,
                                        (unsigned char*)(void*)tagged_objects );

  return java_result;
}
