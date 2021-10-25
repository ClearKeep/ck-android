#include <jni.h>
#include <string>
#include <android/log.h>
#include <iomanip>
#include <sstream>
#include <srp.h>
#include <srp.c>

using namespace std;

SRP_HashAlgorithm alg = SRP_SHA1;
SRP_NGType ng_type = SRP_NG_2048;

string jstringToString(JNIEnv *env, jstring jStr) {
    if (!jStr)
        return "";

    const auto stringClass = env->GetObjectClass(jStr);
    const auto getBytes = env->GetMethodID(stringClass, "getBytes", "(Ljava/lang/String;)[B");
    const auto stringJbytes = (jbyteArray) env->CallObjectMethod(jStr, getBytes,
                                                                 env->NewStringUTF("UTF-8"));

    auto length = (size_t) env->GetArrayLength(stringJbytes);
    jbyte *pBytes = env->GetByteArrayElements(stringJbytes, nullptr);

    std::string ret = std::string((char *) pBytes, length);
    env->ReleaseByteArrayElements(stringJbytes, pBytes, JNI_ABORT);

    env->DeleteLocalRef(stringJbytes);
    env->DeleteLocalRef(stringClass);
    return ret;
}

unsigned char *as_unsigned_char_array(JNIEnv *env, jbyteArray array) {
    int len = env->GetArrayLength(array);
    auto *buf = new unsigned char[len];
    env->GetByteArrayRegion(array, 0, len, reinterpret_cast<jbyte *>(buf));
    return buf;
}

jfieldID getVerificatorPtrFieldId(JNIEnv *env, jobject obj) {
    static jfieldID verificatorPtrId = nullptr;

    if (!verificatorPtrId) {
        jclass c = env->GetObjectClass(obj);
        verificatorPtrId = env->GetFieldID(c, "verificatorPtr", "J");
        env->DeleteLocalRef(c);
    }

    return verificatorPtrId;
}

jfieldID getUsrPtrFieldId(JNIEnv *env, jobject obj) {
    static jfieldID usrPtrId = nullptr;

    if (!usrPtrId) {
        jclass c = env->GetObjectClass(obj);
        usrPtrId = env->GetFieldID(c, "usrPtr", "J");
        env->DeleteLocalRef(c);
    }

    return usrPtrId;
}

//region Create new user
extern "C" JNIEXPORT jbyteArray JNICALL
Java_com_clearkeep_dragonsrp_NativeLib_getSalt(
        JNIEnv *env,
        jobject obj /* this */,
        jstring username,
        jstring rawPassword
) {
    const unsigned char *bytes_s = nullptr;
    const unsigned char *bytes_v = nullptr;

    int len_s = 0;
    int len_v = 0;

    string password = jstringToString(env, rawPassword);

    srp_create_salted_verification_key(alg, ng_type, jstringToString(env, username).c_str(),
                                       (const unsigned char *) password.c_str(),
                                       password.length(),
                                       &bytes_s, &len_s,
                                       &bytes_v, &len_v,
                                       nullptr, nullptr);

    jbyteArray myJByteArray = env->NewByteArray(len_s);
    env->SetByteArrayRegion(myJByteArray, 0, len_s, (jbyte *) bytes_s);

    const unsigned char *testPtr = bytes_s;
    const unsigned char *endPtr = bytes_s + len_s;

    while (testPtr < endPtr) {
        testPtr++;
    }

    env->SetLongField(obj, getVerificatorPtrFieldId(env, obj), (jlong) bytes_v);


    return myJByteArray;
}

extern "C" JNIEXPORT jbyteArray JNICALL
Java_com_clearkeep_dragonsrp_NativeLib_getVerificator(
        JNIEnv *env,
        jobject obj /* this */
) {
    auto *bytes_v = (unsigned char *) env->GetLongField(obj, getVerificatorPtrFieldId(env, obj));

    jbyteArray myJByteArray = env->NewByteArray(256);
    env->SetByteArrayRegion(myJByteArray, 0, 256, (jbyte *) bytes_v);

    return myJByteArray;
}
//endregion

//region Authenticate
extern "C" JNIEXPORT jbyteArray JNICALL
Java_com_clearkeep_dragonsrp_NativeLib_getA(
        JNIEnv *env,
        jobject obj /* this */,
        jstring username,
        jstring rawPassword
) {
    struct SRPUser *usr;

    const unsigned char *bytes_A = nullptr;
    int len_A = 0;

    string password = jstringToString(env, rawPassword);

    const char *auth_username = nullptr;

    usr = srp_user_new(alg, ng_type, jstringToString(env, username).c_str(),
                       (const unsigned char *) password.c_str(), password.length(), nullptr,
                       nullptr);
    env->SetLongField(obj, getUsrPtrFieldId(env, obj), (jlong) usr);

    srp_user_start_authentication(usr, &auth_username, &bytes_A, &len_A);

    jbyteArray myJByteArray = env->NewByteArray(len_A);
    env->SetByteArrayRegion(myJByteArray, 0, len_A, (jbyte *) bytes_A);

    return myJByteArray;
}

extern "C" JNIEXPORT jbyteArray JNICALL
Java_com_clearkeep_dragonsrp_NativeLib_getM(
        JNIEnv *env,
        jobject obj /* this */,
        jbyteArray salt,
        jbyteArray b
) {
    auto *usr = (SRPUser *) env->GetLongField(obj, getUsrPtrFieldId(env, obj));

    const unsigned char *bytes_M = nullptr;
    const unsigned char *bytes_s = as_unsigned_char_array(env, salt);
    const unsigned char *bytes_B = as_unsigned_char_array(env, b);
    int len_M = 0;

    srp_user_process_challenge(usr, bytes_s, 4, bytes_B, 256, &bytes_M, &len_M);

    const unsigned char *testPtr = bytes_s;
    const unsigned char *endPtr = bytes_s + 4;

    while (testPtr < endPtr) {
        testPtr++;
    }

    jbyteArray myJByteArray = env->NewByteArray(len_M);
    env->SetByteArrayRegion(myJByteArray, 0, len_M, (jbyte *) bytes_M);

    const unsigned char *testBPtr = bytes_B;
    const unsigned char *endBPtr = bytes_B + 256;
    while (testBPtr < endBPtr) {
        testBPtr++;
    }

    return myJByteArray;
}
//endregion

extern "C" JNIEXPORT void JNICALL
Java_com_clearkeep_dragonsrp_NativeLib_freeMemoryCreateAccount(
        JNIEnv *env,
        jobject obj /* this */
) {
    auto *bytes_v = (unsigned char *) env->GetLongField(obj, getVerificatorPtrFieldId(env, obj));
    delete bytes_v;
}

extern "C" JNIEXPORT void JNICALL
Java_com_clearkeep_dragonsrp_NativeLib_freeMemoryAuthenticate(
        JNIEnv *env,
        jobject obj /* this */
) {
    auto *usr = (SRPUser *) env->GetLongField(obj, getUsrPtrFieldId(env, obj));
    delete usr;
}