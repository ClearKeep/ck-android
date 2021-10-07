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

extern "C" JNIEXPORT jstring JNICALL
Java_com_clearkeep_dragonsrp_NativeLib_stringFromJNI(
        JNIEnv *env,
        jobject /* this */) {
    std::string hello = "Hello from C++";
    return env->NewStringUTF(hello.c_str());
}


//region Create new user
extern "C" JNIEXPORT jstring JNICALL
Java_com_clearkeep_dragonsrp_NativeLib_getSalt(JNIEnv* env, jobject /* this */) {
//    OsslSha1 hash;
//    OsslRandom random;
//
//    printf("getSalt init");
//    __android_log_print(ANDROID_LOG_DEBUG, "JNI","getSalt init");
//
//    bytes salt;
//    if (salt.size() == 0) salt = random.getRandom(SALTLEN);
//    printf("getSalt gen salt complete");
//    __android_log_print(ANDROID_LOG_DEBUG, "JNI","getSalt gen salt complete");
//
//    __android_log_print(ANDROID_LOG_DEBUG, "JNI", "getSalt saltHex %s", bytesToString(salt).c_str());
//    printf("getSalt converted to jstring");
//    __android_log_print(ANDROID_LOG_DEBUG, "JNI","getSalt converted to jstring");
//
    return stringToJstring(env, "");
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_clearkeep_dragonsrp_NativeLib_getVerificator(
        JNIEnv *env,
        jobject /* this */,
        jstring username,
        jstring rawPassword,
        jstring salt
) {
    try {
        SRP_HashAlgorithm alg = SRP_SHA1;
        SRP_NGType ng_type = SRP_NG_2048;

        struct SRPVerifier *ver;
        struct SRPUser *usr;

        const unsigned char *bytes_s = nullptr;
        const unsigned char *bytes_v = nullptr;
        const unsigned char *bytes_A = nullptr;
        const unsigned char *bytes_B = nullptr;

        const unsigned char *bytes_M = nullptr;
        const unsigned char *bytes_HAMK = nullptr;

        int len_s = 0;
        int len_v = 0;
        int len_A = 0;
        int len_B = 0;
        int len_M = 0;

        string password = jstringToString(env, rawPassword);

        srp_create_salted_verification_key(alg, ng_type, jstringToString(env, username).c_str(),
                                           (const unsigned char *) password.c_str(),
                                           password.length(),
                                           &bytes_s, &len_s,
                                           &bytes_v, &len_v,
                                           nullptr, nullptr);

        string saltStr(reinterpret_cast<char const*>(bytes_s));
        string verificatorStr(reinterpret_cast<char const*>(bytes_v));

        __android_log_print(ANDROID_LOG_DEBUG, "JNI", "getVerificator salt %s len %d", saltStr.c_str(), len_s);
        __android_log_print(ANDROID_LOG_DEBUG, "JNI", "getVerificator verificator %s len %d", verificatorStr.c_str(), len_v);
        return stringToJstring(env, "");
    } catch (...) {
        string error = "Unknown exception";
        return env->NewStringUTF(error.c_str());
    }
}
//endregion

//region Authenticate
//extern "C" JNIEXPORT jstring JNICALL
//Java_com_clearkeep_dragonsrp_NativeLib_getA(
//        JNIEnv *env,
//        jobject obj /* this */,
//        jstring username,
//        jstring rawPassword
//) {
//    try {
//        auto* hash = new OsslSha1();
//        auto* random = new OsslRandom();
//        Ng ng = Ng::predefined(2048);
//        auto* math = new OsslMathImpl(*hash, ng);
//
//        auto* srpclientPtr = new SrpClient(*math, *random, false);
//        env->SetLongField(obj, getSrpClientPtrFieldId(env, obj), (jlong) srpclientPtr);
//
//        string convertedUsername = jstringToString(env, username);
//        string convertedPassword = jstringToString(env, rawPassword);
//
//        bytes usernameBytes = DragonSRP::Conversion::string2bytes(convertedUsername);
//        bytes passwordBytes = DragonSRP::Conversion::string2bytes(convertedPassword);
//
//        SrpClientAuthenticator sca = srpclientPtr->getAuthenticator(usernameBytes, passwordBytes);
//        auto *scaPtr = new SrpClientAuthenticator(sca);
//        env->SetLongField(obj, getSrpcClientAuthenticatorPtr(env, obj), (jlong) scaPtr);
//
//        string aString = bytesToString(sca.getA());
//        return stringToJstring(env, aString);
//    } catch (UserNotFoundException e) {
//        __android_log_print(ANDROID_LOG_DEBUG, "JNI", "getA exception %s", e.what().c_str());
//        return env->NewStringUTF(e.what().c_str());
//    } catch (DsrpException e) {
//        __android_log_print(ANDROID_LOG_DEBUG, "JNI", "getA exception %s", e.what().c_str());
//        return env->NewStringUTF(e.what().c_str());
//    } catch (...) {
//        __android_log_print(ANDROID_LOG_DEBUG, "JNI", "unknown exception");
//        string error = "Unknown exception";
//        return env->NewStringUTF(error.c_str());
//    }
//}

//extern "C" JNIEXPORT jstring JNICALL
//Java_com_clearkeep_dragonsrp_NativeLib_getM1(
//        JNIEnv *env,
//        jobject obj /* this */,
//        jstring salt,
//        jstring b
//) {
//    try {
//        auto *sca = (SrpClientAuthenticator *) env->GetLongField(obj,
//                                                                 getSrpcClientAuthenticatorPtr(env,
//                                                                                               obj));
//        auto *srpclient = (SrpClient *) env->GetLongField(obj, getSrpClientPtrFieldId(env, obj));
//        __android_log_print(ANDROID_LOG_DEBUG, "JNI",
//                            "getM1 pointer init success sca %ld srpcclient %ld", (long) sca,
//                            (long) srpclient);
//
//        bytes saltBytes = Conversion::hexstring2bytes(jstringToString(env, salt));
//        bytes B = Conversion::hexstring2bytes(jstringToString(env, b));
//
//        __android_log_print(ANDROID_LOG_DEBUG, "JNI", "getM1 salt %s B %s",
//                            bytesToString(saltBytes).c_str(), bytesToString(B).c_str());
//
//        bytes m1Bytes = srpclient->getM1(saltBytes, B, *sca);
//        string m1String = bytesToString(m1Bytes);
//        __android_log_print(ANDROID_LOG_DEBUG, "JNI", "getM1 %s", m1String.c_str());
//
//        return stringToJstring(env, m1String);
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