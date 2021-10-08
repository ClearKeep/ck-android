#include <jni.h>
#include <string>
#include <android/log.h>
#include <iomanip>
#include <sstream>
#include <srp.h>
#include <srp.c>

using namespace std;

string jstringToString(JNIEnv *env, jstring jStr) {
    if (!jStr)
        return "";

    const jclass stringClass = env->GetObjectClass(jStr);
    const jmethodID getBytes = env->GetMethodID(stringClass, "getBytes", "(Ljava/lang/String;)[B");
    const jbyteArray stringJbytes = (jbyteArray) env->CallObjectMethod(jStr, getBytes, env->NewStringUTF("UTF-8"));

    size_t length = (size_t) env->GetArrayLength(stringJbytes);
    jbyte* pBytes = env->GetByteArrayElements(stringJbytes, NULL);

    std::string ret = std::string((char *)pBytes, length);
    env->ReleaseByteArrayElements(stringJbytes, pBytes, JNI_ABORT);

    env->DeleteLocalRef(stringJbytes);
    env->DeleteLocalRef(stringClass);
    return ret;
}

jstring stringToJstring(JNIEnv *env, string str) {
    int byteCount = str.length();
    jbyte* pNativeMessage = const_cast<jbyte *>(reinterpret_cast<const jbyte *>(str.c_str()));
    jbyteArray bytes = env->NewByteArray(byteCount);
    env->SetByteArrayRegion(bytes, 0, byteCount, pNativeMessage);

    // find the Charset.forName method:
    //   javap -s java.nio.charset.Charset | egrep -A2 "forName"
    jclass charsetClass = env->FindClass("java/nio/charset/Charset");
    jmethodID forName = env->GetStaticMethodID(
            charsetClass, "forName", "(Ljava/lang/String;)Ljava/nio/charset/Charset;");
    jstring utf8 = env->NewStringUTF("UTF-8");
    jobject charset = env->CallStaticObjectMethod(charsetClass, forName, utf8);

    // find a String constructor that takes a Charset:
    //   javap -s java.lang.String | egrep -A2 "String\(.*charset"
    jclass stringClass = env->FindClass("java/lang/String");
    jmethodID ctor = env->GetMethodID(
            stringClass, "<init>", "([BLjava/nio/charset/Charset;)V");

    jstring jMessage = reinterpret_cast<jstring>(
            env->NewObject(stringClass, ctor, bytes, charset));

    return jMessage;
}

unsigned char* as_unsigned_char_array(JNIEnv *env, jbyteArray array) {
    int len = env->GetArrayLength (array);
    auto* buf = new unsigned char[len];
    env->GetByteArrayRegion (array, 0, len, reinterpret_cast<jbyte*>(buf));
    return buf;
}

jfieldID getHashPtrFieldId(JNIEnv *env, jobject obj) {
    static jfieldID hashPtrId = 0;

    if (!hashPtrId) {
        jclass c = env->GetObjectClass(obj);
        hashPtrId = env->GetFieldID(c, "hashPtr", "J");
        env->DeleteLocalRef(c);
    }

    return hashPtrId;
}

jfieldID getSrpClientPtrFieldId(JNIEnv *env, jobject obj) {
    static jfieldID srpClientPtrId = 0;

    if (!srpClientPtrId) {
        jclass c = env->GetObjectClass(obj);
        srpClientPtrId = env->GetFieldID(c, "srpClientPtr", "J");
        env->DeleteLocalRef(c);
    }

    return srpClientPtrId;
}

jfieldID getSrpcClientAuthenticatorPtr(JNIEnv *env, jobject obj) {
    static jfieldID srpcClientAuthenticatorPtrId = 0;

    if (!srpcClientAuthenticatorPtrId) {
        jclass c = env->GetObjectClass(obj);
        srpcClientAuthenticatorPtrId = env->GetFieldID(c, "srpClientAuthenticatorPtr", "J");
        env->DeleteLocalRef(c);
    }

    return srpcClientAuthenticatorPtrId;
}

jfieldID getSrpServerPtrFieldId(JNIEnv *env, jobject obj) {
    static jfieldID srpServerPtrId = 0;

    if (!srpServerPtrId) {
        jclass c = env->GetObjectClass(obj);
        srpServerPtrId = env->GetFieldID(c, "srpServerPtr", "J");
        env->DeleteLocalRef(c);
    }

    return srpServerPtrId;
}

jfieldID getSrpVerificatorPtrFieldId(JNIEnv *env, jobject obj) {
    static jfieldID srpVerificatorPtrId = 0;

    if (!srpVerificatorPtrId) {
        jclass c = env->GetObjectClass(obj);
        srpVerificatorPtrId = env->GetFieldID(c, "srpVerificatorPtr", "J");
        env->DeleteLocalRef(c);
    }

    return srpVerificatorPtrId;
}

jfieldID getVerificatorPtrFieldId(JNIEnv *env, jobject obj) {
    static jfieldID verificatorPtrId = 0;

    if (!verificatorPtrId) {
        jclass c = env->GetObjectClass(obj);
        verificatorPtrId = env->GetFieldID(c, "verificatorPtr", "J");
        env->DeleteLocalRef(c);
    }

    return verificatorPtrId;
}

jfieldID getUsrPtrFieldId(JNIEnv *env, jobject obj) {
    static jfieldID usrPtrId = 0;

    if (!usrPtrId) {
        jclass c = env->GetObjectClass(obj);
        usrPtrId = env->GetFieldID(c, "usrPtr", "J");
        env->DeleteLocalRef(c);
    }

    return usrPtrId;
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_clearkeep_dragonsrp_NativeLib_stringFromJNI(
        JNIEnv *env,
        jobject /* this */) {
    std::string hello = "Hello from C++";
    return env->NewStringUTF(hello.c_str());
}


//region Create new user
extern "C" JNIEXPORT jbyteArray JNICALL
Java_com_clearkeep_dragonsrp_NativeLib_getSalt(JNIEnv *env,
                                               jobject obj /* this */,
                                               jstring username,
                                               jstring rawPassword) {
    SRP_HashAlgorithm alg = SRP_SHA256;
    SRP_NGType ng_type = SRP_NG_2048;

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
        __android_log_print(ANDROID_LOG_DEBUG, "JNI", "getSalt salt iter %d", (int) *testPtr);
        testPtr++;
    }

    env->SetLongField(obj, getVerificatorPtrFieldId(env, obj), (jlong) bytes_v);

    __android_log_print(ANDROID_LOG_DEBUG, "JNI", "getSalt verificator length %d", len_v);

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
    SRP_HashAlgorithm alg = SRP_SHA1;
    SRP_NGType ng_type = SRP_NG_2048;

    struct SRPUser     * usr;

    const unsigned char * bytes_A = nullptr;
    int len_A   = 0;

    string password = jstringToString(env, rawPassword);

    const char * auth_username = nullptr;

    usr = srp_user_new( alg, ng_type, jstringToString(env, username).c_str(), (const unsigned char *) password.c_str(), password.length(), nullptr, nullptr);
    env->SetLongField(obj, getUsrPtrFieldId(env, obj), (jlong) usr);

    srp_user_start_authentication( usr, &auth_username, &bytes_A, &len_A);

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

    const unsigned char * bytes_M = nullptr;
    const unsigned char * bytes_s = as_unsigned_char_array(env, salt);
    const unsigned char * bytes_B = as_unsigned_char_array(env, b);
    int len_M   = 0;

    srp_user_process_challenge(usr, bytes_s, 4, bytes_B, 256, &bytes_M, &len_M);

    const unsigned char *testPtr = bytes_s;
    const unsigned char *endPtr = bytes_s + 4;

    while (testPtr < endPtr) {
        __android_log_print(ANDROID_LOG_DEBUG, "JNI", "getSalt after convert salt iter %d", (int) *testPtr);
        testPtr++;
    }

    jbyteArray myJByteArray = env->NewByteArray(len_M);
    env->SetByteArrayRegion(myJByteArray, 0, len_M, (jbyte *) bytes_M);

    const unsigned char *testBPtr = bytes_B;
    const unsigned char *endBPtr = bytes_B + 256;
    while (testBPtr < endBPtr) {
        __android_log_print(ANDROID_LOG_DEBUG, "JNI", "getM after convert m iter %d", (int) *testBPtr);
        testBPtr++;
    }

    return myJByteArray;
}

//extern "C" JNIEXPORT jstring JNICALL
//Java_com_clearkeep_dragonsrp_NativeLib_getK(
//        JNIEnv *env,
//        jobject obj /* this */,
//        jstring m2
//) {
//    try {
//        auto *sca = (SrpClientAuthenticator *) env->GetLongField(obj,
//                                                                 getSrpcClientAuthenticatorPtr(env,
//                                                                                               obj));
//
//        bytes m2Bytes = Conversion::hexstring2bytes(jstringToString(env, m2));
//
//        bytes K = sca->getSessionKey(m2Bytes);
//        string kString = bytesToString(K);
//
//        __android_log_print(ANDROID_LOG_DEBUG, "JNI", "getK from client success %s", kString.c_str());
//
//        return stringToJstring(env, kString);
//    } catch (UserNotFoundException e) {
//        __android_log_print(ANDROID_LOG_DEBUG, "JNI", "getM1 exception %s", e.what().c_str());
//        return env->NewStringUTF(e.what().c_str());
//    } catch (DsrpException e) {
//        __android_log_print(ANDROID_LOG_DEBUG, "JNI", "getM1 exception %s", e.what().c_str());
//        return env->NewStringUTF(e.what().c_str());
//    } catch (...) {
//        __android_log_print(ANDROID_LOG_DEBUG, "JNI", "getM1 unknown exception");
//        string error = "Unknown exception";
//        return env->NewStringUTF(error.c_str());
//    }
//}
//endregion