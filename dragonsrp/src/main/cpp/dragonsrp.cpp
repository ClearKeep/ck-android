#include <jni.h>
#include <string>
#include <android/log.h>
#include <iomanip>
#include <sstream>
#include <dsrp/memorylookup.hpp>
#include <dsrp/srpserver.hpp>

#include "dsrp/srpclient.hpp"
#include "dsrp/srpclientauthenticator.hpp"
#include "dsrp/user.hpp"
#include "dsrp/ng.hpp"

#include "dsrp/dsrpexception.hpp"
#include "dsrp/conversionexception.hpp"
#include "dsrp/usernotfoundexception.hpp"
#include "dsrp/conversion.hpp"

#include "ossl/osslsha1.hpp"
#include "ossl/osslmathimpl.hpp"
#include "ossl/osslrandom.hpp"

#define SALTLEN 32

using namespace DragonSRP;
using namespace DragonSRP::Ossl;
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

string bytesToString(bytes b) {
    std::vector<unsigned char>::const_iterator from = b.begin();
    std::vector<unsigned char>::const_iterator to = b.end();

    std::stringstream bHex;

    for (; from != to; ++from) {
//        __android_log_print(ANDROID_LOG_DEBUG, "JNI", "%02X", *from);
        bHex << std::hex << std::setw(2) << std::setfill('0') << (int) *from;
    }

    return bHex.str();
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
    OsslSha1 hash;
    OsslRandom random;

    printf("getSalt init");
    __android_log_print(ANDROID_LOG_DEBUG, "JNI","getSalt init");

    bytes salt;
    if (salt.size() == 0) salt = random.getRandom(SALTLEN);
    printf("getSalt gen salt complete");
    __android_log_print(ANDROID_LOG_DEBUG, "JNI","getSalt gen salt complete");

    __android_log_print(ANDROID_LOG_DEBUG, "JNI", "getSalt saltHex %s", bytesToString(salt).c_str());
    printf("getSalt converted to jstring");
    __android_log_print(ANDROID_LOG_DEBUG, "JNI","getSalt converted to jstring");

    return stringToJstring(env, bytesToString(salt));
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
        OsslSha1 hash;
        OsslRandom random;
        Ng ng = Ng::predefined(2048);
        OsslMathImpl math(hash, ng);

        string convertedUsername = jstringToString(env, username);
        string convertedPassword = jstringToString(env, rawPassword);
        string convertedSalt = jstringToString(env, salt);

        bytes usernameBytes = DragonSRP::Conversion::string2bytes(convertedUsername);
        bytes passwordBytes = DragonSRP::Conversion::string2bytes(convertedPassword);
        bytes saltBytes = DragonSRP::Conversion::hexstring2bytes(convertedSalt);

        bytes verificator = math.calculateVerificator(usernameBytes, passwordBytes, saltBytes);

        string verificatorString = bytesToString(verificator);
        __android_log_print(ANDROID_LOG_DEBUG, "JNI", "getVerificator %s",
                            verificatorString.c_str());
        return stringToJstring(env, verificatorString);
    } catch (UserNotFoundException e) {
        return env->NewStringUTF(e.what().c_str());
    } catch (DsrpException e) {
        return env->NewStringUTF(e.what().c_str());
    } catch (...) {
        string error = "Unknown exception";
        return env->NewStringUTF(error.c_str());
    }
}
//endregion

//region Authenticate
extern "C" JNIEXPORT jstring JNICALL
Java_com_clearkeep_dragonsrp_NativeLib_getA(
        JNIEnv *env,
        jobject obj /* this */,
        jstring username,
        jstring rawPassword
) {
    try {
        auto* hash = new OsslSha1();
        auto* random = new OsslRandom();
        Ng ng = Ng::predefined(2048);
        auto* math = new OsslMathImpl(*hash, ng);

        auto* srpclientPtr = new SrpClient(*math, *random, false);
        env->SetLongField(obj, getSrpClientPtrFieldId(env, obj), (jlong) srpclientPtr);

        string convertedUsername = jstringToString(env, username);
        string convertedPassword = jstringToString(env, rawPassword);

        bytes usernameBytes = DragonSRP::Conversion::string2bytes(convertedUsername);
        bytes passwordBytes = DragonSRP::Conversion::string2bytes(convertedPassword);

        SrpClientAuthenticator sca = srpclientPtr->getAuthenticator(usernameBytes, passwordBytes);
        auto *scaPtr = new SrpClientAuthenticator(sca);
        env->SetLongField(obj, getSrpcClientAuthenticatorPtr(env, obj), (jlong) scaPtr);

        string aString = bytesToString(sca.getA());
        return stringToJstring(env, aString);
    } catch (UserNotFoundException e) {
        __android_log_print(ANDROID_LOG_DEBUG, "JNI", "getA exception %s", e.what().c_str());
        return env->NewStringUTF(e.what().c_str());
    } catch (DsrpException e) {
        __android_log_print(ANDROID_LOG_DEBUG, "JNI", "getA exception %s", e.what().c_str());
        return env->NewStringUTF(e.what().c_str());
    } catch (...) {
        __android_log_print(ANDROID_LOG_DEBUG, "JNI", "unknown exception");
        string error = "Unknown exception";
        return env->NewStringUTF(error.c_str());
    }
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_clearkeep_dragonsrp_NativeLib_getM1(
        JNIEnv *env,
        jobject obj /* this */,
        jstring salt,
        jstring b
) {
    try {
        auto *sca = (SrpClientAuthenticator *) env->GetLongField(obj,
                                                                 getSrpcClientAuthenticatorPtr(env,
                                                                                               obj));
        auto *srpclient = (SrpClient *) env->GetLongField(obj, getSrpClientPtrFieldId(env, obj));
        __android_log_print(ANDROID_LOG_DEBUG, "JNI",
                            "getM1 pointer init success sca %ld srpcclient %ld", (long) sca,
                            (long) srpclient);

        bytes saltBytes = Conversion::hexstring2bytes(jstringToString(env, salt));
        bytes B = Conversion::hexstring2bytes(jstringToString(env, b));

        __android_log_print(ANDROID_LOG_DEBUG, "JNI", "getM1 salt %s B %s",
                            bytesToString(saltBytes).c_str(), bytesToString(B).c_str());

        bytes m1Bytes = srpclient->getM1(saltBytes, B, *sca);
        string m1String = bytesToString(m1Bytes);
        __android_log_print(ANDROID_LOG_DEBUG, "JNI", "getM1 %s", m1String.c_str());

        return stringToJstring(env, m1String);
    } catch (UserNotFoundException e) {
        __android_log_print(ANDROID_LOG_DEBUG, "JNI", "getM1 exception %s", e.what().c_str());
        return env->NewStringUTF(e.what().c_str());
    } catch (DsrpException e) {
        __android_log_print(ANDROID_LOG_DEBUG, "JNI", "getM1 exception %s", e.what().c_str());
        return env->NewStringUTF(e.what().c_str());
    } catch (...) {
        __android_log_print(ANDROID_LOG_DEBUG, "JNI", "getM1 unknown exception");
        string error = "Unknown exception";
        return env->NewStringUTF(error.c_str());
    }
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_clearkeep_dragonsrp_NativeLib_getK(
        JNIEnv *env,
        jobject obj /* this */,
        jstring m2
) {
    try {
        auto *sca = (SrpClientAuthenticator *) env->GetLongField(obj,
                                                                 getSrpcClientAuthenticatorPtr(env,
                                                                                               obj));

        bytes m2Bytes = Conversion::hexstring2bytes(jstringToString(env, m2));

        bytes K = sca->getSessionKey(m2Bytes);
        string kString = bytesToString(K);

        __android_log_print(ANDROID_LOG_DEBUG, "JNI", "getK from client success %s", kString.c_str());

        return stringToJstring(env, kString);
    } catch (UserNotFoundException e) {
        __android_log_print(ANDROID_LOG_DEBUG, "JNI", "getM1 exception %s", e.what().c_str());
        return env->NewStringUTF(e.what().c_str());
    } catch (DsrpException e) {
        __android_log_print(ANDROID_LOG_DEBUG, "JNI", "getM1 exception %s", e.what().c_str());
        return env->NewStringUTF(e.what().c_str());
    } catch (...) {
        __android_log_print(ANDROID_LOG_DEBUG, "JNI", "getM1 unknown exception");
        string error = "Unknown exception";
        return env->NewStringUTF(error.c_str());
    }
}
//endregion

//region Test server code
extern "C" JNIEXPORT jstring JNICALL
Java_com_clearkeep_dragonsrp_NativeLib_testVerifyGetSalt(
        JNIEnv *env,
        jobject obj /* this */,
        jstring username,
        jstring verificator,
        jstring salt,
        jstring a
) {
    try {
        OsslSha1* hash = new OsslSha1(); // We will use OpenSSL SHA1 implementation
        OsslRandom* random = new OsslRandom(); // We will use OpenSSL random number generator
        MemoryLookup* lookup = new MemoryLookup(); // This stores users in memory (linked-list)

        // Load predefined N,g 1024bit RFC values
        Ng ng = Ng::predefined(2048);

        OsslMathImpl* math = new OsslMathImpl(*hash, ng);
        SrpServer* srpserver = new SrpServer(*lookup, *math, *random, false);
        __android_log_print(ANDROID_LOG_DEBUG, "JNI", "testVerifyGetSalt srpserverPtr %ld", (long) srpserver);
        env->SetLongField(obj, getSrpServerPtrFieldId(env, obj), (jlong) srpserver);

        // Begin user creation

        string usernameStr = jstringToString(env, username);
        string saltStr = jstringToString(env, salt);
        string verificatorStr = jstringToString(env, verificator);

        bytes usernameBytes = Conversion::string2bytes(usernameStr);
        bytes saltBytes = Conversion::hexstring2bytes(saltStr);
        bytes verificatorBytes = Conversion::hexstring2bytes(verificatorStr);

        __android_log_print(ANDROID_LOG_DEBUG, "JNI", "testVerifyGetSalt usernameStr %s", bytesToString(usernameBytes).c_str());
        __android_log_print(ANDROID_LOG_DEBUG, "JNI", "testVerifyGetSalt saltStr %s", bytesToString(saltBytes).c_str());
        __android_log_print(ANDROID_LOG_DEBUG, "JNI", "testVerifyGetSalt verificatorStr %s", bytesToString(verificatorBytes).c_str());

        User u(usernameBytes, verificatorBytes, saltBytes);

        if (!lookup->userAdd(u)) {
            __android_log_print(ANDROID_LOG_DEBUG, "JNI", "testVerifyA Error: user already exists");
        }
        // End of user creation


        // Receive A from client
        bytes A = Conversion::hexstring2bytes(jstringToString(env, a));

        // verificator is used to authenticate one user(one session)
        SrpVerificator ver = srpserver->getVerificator(usernameBytes, A);
        SrpVerificator* srpVerificator = new SrpVerificator(ver);
        env->SetLongField(obj, getSrpVerificatorPtrFieldId(env, obj), (jlong) srpVerificator);
        __android_log_print(ANDROID_LOG_DEBUG, "JNI", "testVerifyGetSalt srpVerificator %ld", (long) srpVerificator);

        return stringToJstring(env, bytesToString(ver.getSalt()));
    }
    catch (UserNotFoundException e) {
        __android_log_print(ANDROID_LOG_DEBUG, "JNI", "testVerifyGetSalt exception! UserNotFoundException");
        return env->NewStringUTF(e.what().c_str());
    } catch (DsrpException e) {
        __android_log_print(ANDROID_LOG_DEBUG, "JNI", "testVerifyGetSalt exception! DsrpException");
        return env->NewStringUTF(e.what().c_str());
    } catch (...) {
        string error = "Unknown exception";
        __android_log_print(ANDROID_LOG_DEBUG, "JNI", "testVerifyGetSalt exception! Unknown exception");
        return env->NewStringUTF(error.c_str());
    }
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_clearkeep_dragonsrp_NativeLib_testVerifyGetB(
        JNIEnv *env,
        jobject obj /* this */
) {
    try {
        __android_log_print(ANDROID_LOG_DEBUG, "JNI", "testVerifyGetB init");
        SrpVerificator* ver = (SrpVerificator*) env->GetLongField(obj, getSrpVerificatorPtrFieldId(env, obj));
        __android_log_print(ANDROID_LOG_DEBUG, "JNI", "testVerifyGetB init ptr ver %ld", (long) ver);

        bytes slt = ver->getSalt();
        bytes byteB = ver->getB();
        __android_log_print(ANDROID_LOG_DEBUG, "JNI", "testVerifyGetB got B %s", bytesToString(byteB).c_str());
        // Send salt and B to client
        return stringToJstring(env, bytesToString(byteB));
    }
    catch (UserNotFoundException e) {
        __android_log_print(ANDROID_LOG_DEBUG, "JNI", "testVerifyGetB exception! UserNotFoundException");
        return env->NewStringUTF(e.what().c_str());
    } catch (DsrpException e) {
        __android_log_print(ANDROID_LOG_DEBUG, "JNI", "testVerifyGetB exception! DsrpException");
        return env->NewStringUTF(e.what().c_str());
    } catch (...) {
        string error = "Unknown exception";
        __android_log_print(ANDROID_LOG_DEBUG, "JNI", "testVerifyGetB exception! Unknown exception");
        return env->NewStringUTF(error.c_str());
    }
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_clearkeep_dragonsrp_NativeLib_testVerifyGetM2(
        JNIEnv *env,
        jobject obj /* this */,
        jstring m1
) {
    try {
        __android_log_print(ANDROID_LOG_DEBUG, "JNI", "testVerifyGetM2 init");
        SrpVerificator* ver = (SrpVerificator*) env->GetLongField(obj, getSrpVerificatorPtrFieldId(env, obj));
        __android_log_print(ANDROID_LOG_DEBUG, "JNI", "testVerifyGetM2 init ptr ver %ld", (long) ver);

        bytes slt = ver->getSalt();
        bytes byteB = ver->getB();
        __android_log_print(ANDROID_LOG_DEBUG, "JNI", "testVerifyGetM2 got M1 %s", bytesToString(byteB).c_str());

        // receive M1 from client
        bytes M1_fc = Conversion::hexstring2bytes(jstringToString(env, m1));

        bytes M2_to_client;
        bytes K; // secret session key

        // if M1 is OK we get M2 and K otherwise exception is thrown
        ver->authenticate(M1_fc, M2_to_client, K);

        string m2String = bytesToString(M2_to_client);
        return stringToJstring(env, m2String);
    }
    catch (UserNotFoundException e) {
        __android_log_print(ANDROID_LOG_DEBUG, "JNI", "testVerifyGetM2 exception! UserNotFoundException");
        return env->NewStringUTF(e.what().c_str());
    } catch (DsrpException e) {
        __android_log_print(ANDROID_LOG_DEBUG, "JNI", "testVerifyGetM2 exception! DsrpException");
        return env->NewStringUTF(e.what().c_str());
    } catch (...) {
        string error = "Unknown exception";
        __android_log_print(ANDROID_LOG_DEBUG, "JNI",
                            "testVerifyGetM2 exception! Unknown exception");
        return env->NewStringUTF(error.c_str());
    }
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_clearkeep_dragonsrp_NativeLib_testVerifyGetK(
        JNIEnv *env,
        jobject  obj/* this */,
        jstring m1
) {
    try {
        __android_log_print(ANDROID_LOG_DEBUG, "JNI", "testVerifyGetM2 init");
        SrpVerificator* ver = (SrpVerificator*) env->GetLongField(obj, getSrpVerificatorPtrFieldId(env, obj));
        __android_log_print(ANDROID_LOG_DEBUG, "JNI", "testVerifyGetM2 init ptr ver %ld", (long) ver);

        bytes slt = ver->getSalt();
        bytes byteB = ver->getB();
        __android_log_print(ANDROID_LOG_DEBUG, "JNI", "testVerifyGetM2 got M1 %s", bytesToString(byteB).c_str());

        // receive M1 from client
        bytes M1_fc = Conversion::hexstring2bytes(jstringToString(env, m1));

        bytes M2_to_client;
        bytes K; // secret session key

        // if M1 is OK we get M2 and K otherwise exception is thrown
        ver->authenticate(M1_fc, M2_to_client, K);

        string kString = bytesToString(K);
        return stringToJstring(env, kString);
    }
    catch (UserNotFoundException e) {
        __android_log_print(ANDROID_LOG_DEBUG, "JNI", "testVerifyGetK exception! UserNotFoundException");
        return env->NewStringUTF(e.what().c_str());
    } catch (DsrpException e) {
        __android_log_print(ANDROID_LOG_DEBUG, "JNI", "testVerifyGetK exception! DsrpException");
        return env->NewStringUTF(e.what().c_str());
    } catch (...) {
        string error = "Unknown exception";
        __android_log_print(ANDROID_LOG_DEBUG, "JNI", "testVerifyGetK exception! Unknown exception");
        return env->NewStringUTF(error.c_str());
    }
}
//endregion