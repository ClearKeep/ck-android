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
        Ng ng = Ng::predefined(1024);
        OsslMathImpl math(hash, ng);

        string convertedUsername = jstringToString(env, username);
        string convertedPassword = jstringToString(env, rawPassword);
        string convertedSalt = jstringToString(env, salt);

        bytes usernameBytes = DragonSRP::Conversion::string2bytes(convertedUsername);
        bytes passwordBytes = DragonSRP::Conversion::string2bytes(convertedPassword);
        bytes saltBytes = DragonSRP::Conversion::string2bytes(convertedSalt);

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

extern "C" JNIEXPORT jstring JNICALL
Java_com_clearkeep_dragonsrp_NativeLib_getA(
        JNIEnv *env,
        jobject obj /* this */,
        jstring username,
        jstring rawPassword
) {
    try {
        OsslSha1* hash = new OsslSha1();
        OsslRandom* random = new OsslRandom();
        Ng ng = Ng::predefined(1024);
        OsslMathImpl* math = new OsslMathImpl(*hash, ng);

        auto* srpclientPtr = new SrpClient(*math, *random, false);
        env->SetLongField(obj, getSrpClientPtrFieldId(env, obj), (jlong) srpclientPtr);

        string convertedUsername = jstringToString(env, username);
        string convertedPassword = jstringToString(env, rawPassword);

        bytes usernameBytes = DragonSRP::Conversion::string2bytes(convertedUsername);
        bytes passwordBytes = DragonSRP::Conversion::string2bytes(convertedPassword);

        SrpClientAuthenticator sca = srpclientPtr->getAuthenticator(usernameBytes, passwordBytes);
        SrpClientAuthenticator* scaPtr = &sca;
        env->SetLongField(obj, getSrpcClientAuthenticatorPtr(env, obj), (jlong) scaPtr);

        __android_log_print(ANDROID_LOG_DEBUG, "JNI", "getA srpclientPtr %ld", (long) srpclientPtr);
        __android_log_print(ANDROID_LOG_DEBUG, "JNI", "getA scaPtr %ld", (long) scaPtr);

        string aString = bytesToString(sca.getA());
        __android_log_print(ANDROID_LOG_DEBUG, "JNI", "getA %s", aString.c_str());

        //TEST pointer, remove after done
        bytes salt = DragonSRP::Conversion::hexstring2bytes("832f55bd03808ce8015f01515f193ebc64fc34e5c8fd312e60bc56774f071bf4");
        bytes B = DragonSRP::Conversion::hexstring2bytes("d0950ed022b048a9ecb43cfe053a024a56a516a76951ed69594efbba56dac0d15770ef96014998123f0c2064ffbfee1043c28590bf1f375fe62d255c120ef2d62d1f3f8c48217ed5ff1069ca54946d13f11ea7487806df54154015e5de21df7fcf93a5158a9fa6e517860e5b999782c3e91d1fa8caec1b61a7d5aae067b88896");
        auto *scaJava = (SrpClientAuthenticator*) env->GetLongField(obj, getSrpcClientAuthenticatorPtr(env, obj));
        auto *srpcJava = (SrpClient*) env->GetLongField(obj, getSrpClientPtrFieldId(env, obj));

        bytes m1Bytes = srpcJava->getM1(salt, B, *scaJava);
        __android_log_print(ANDROID_LOG_DEBUG, "JNI", "getA test pointer success sca %ld srpclient %ld, scaJava %ld srpcJava %ld", (long) scaPtr, (long) srpclientPtr, (long) scaJava, (long) srpcJava);

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
        jstring username,
        jstring rawPassword,
        jstring newSalt,
        jstring b
) {
    try {
        auto *sca = (SrpClientAuthenticator*) env->GetLongField(obj, getSrpcClientAuthenticatorPtr(env, obj));
        auto *srpclient = (SrpClient*) env->GetLongField(obj, getSrpClientPtrFieldId(env, obj));
        __android_log_print(ANDROID_LOG_DEBUG, "JNI", "getM1 pointer init success sca %ld srpcclient %ld", (long) sca, (long) srpclient);

        bytes salt = Conversion::hexstring2bytes(jstringToString(env, newSalt));
        bytes B = Conversion::hexstring2bytes(jstringToString(env, b));

        __android_log_print(ANDROID_LOG_DEBUG, "JNI", "getM1 salt %s B %s", bytesToString(salt).c_str(), bytesToString(B).c_str());

        bytes m1Bytes = srpclient->getM1(salt, B, *sca);
        string m1String = bytesToString(m1Bytes);
        __android_log_print(ANDROID_LOG_DEBUG, "JNI", "getM1 %s", m1String.c_str());

        return stringToJstring(env, m1String);
    } catch (UserNotFoundException e) {
        return env->NewStringUTF(e.what().c_str());
    } catch (DsrpException e) {
        return env->NewStringUTF(e.what().c_str());
    } catch (...) {
        string error = "Unknown exception";
        return env->NewStringUTF(error.c_str());
    }
}

//////////////////////////////////////////////////////////// Test server code
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
        Ng ng = Ng::predefined(1024);

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
        SrpVerificator* srpVerificator = &ver;
        env->SetLongField(obj, getSrpVerificatorPtrFieldId(env, obj), (jlong) srpVerificator);
        __android_log_print(ANDROID_LOG_DEBUG, "JNI", "testVerifyGetSalt srpVerificator %ld", (long) srpVerificator);

        // Send salt and B to client
        __android_log_print(ANDROID_LOG_DEBUG, "JNI", "testVerifyGetSalt B: %s", bytesToString(ver.getB()).c_str());
        return stringToJstring(env, bytesToString(ver.getSalt()) + "," + bytesToString(ver.getB()));
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
        jobject obj /* this */,
        jstring username,
        jstring verificator,
        jstring salt,
        jstring a
) {
    try {
        __android_log_print(ANDROID_LOG_DEBUG, "JNI", "testVerifyGetB init");
        SrpVerificator* ver = (SrpVerificator*) env->GetLongField(obj, getSrpVerificatorPtrFieldId(env, obj));
        __android_log_print(ANDROID_LOG_DEBUG, "JNI", "testVerifyGetB init ptr ver %ld", (long) ver);

        bytes slt = ver->getSalt();
        bytes byteB = ver->getB();
        __android_log_print(ANDROID_LOG_DEBUG, "JNI", "testVerifyGetB got B");
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
        jobject /* this */,
        jstring username,
        jstring verificator,
        jstring salt,
        jstring a,
        jstring m1
) {
    try {
        OsslSha1 hash; // We will use OpenSSL SHA1 implementation
        OsslRandom random; // We will use OpenSSL random number generator
        MemoryLookup lookup; // This stores users in memory (linked-list)

        // Load predefined N,g 1024bit RFC values
        Ng ng = Ng::predefined(1024);

        OsslMathImpl math(hash, ng);
        SrpServer srpserver(lookup, math, random, false);

        // Begin user creation

        bytes usernameBytes = Conversion::string2bytes(jstringToString(env, username));
        bytes saltBytes = Conversion::string2bytes(jstringToString(env, salt));
        bytes verificatorBytes = Conversion::string2bytes(jstringToString(env, verificator));

        User u(usernameBytes, verificatorBytes, saltBytes);

        if (!lookup.userAdd(u)) {
            __android_log_print(ANDROID_LOG_DEBUG, "JNI", "testVerifyA Error: user already exists");
        }
        // End of user creation


        // Receive A from client
        bytes A = Conversion::string2bytes(jstringToString(env, a));

        // verificator is used to authenticate one user(one session)
        SrpVerificator ver = srpserver.getVerificator(usernameBytes, A);

        // receive M1 from client
        bytes M1_fc = Conversion::string2bytes(jstringToString(env, m1));

        bytes M2_to_client;
        bytes K; // secret session key

        // if M1 is OK we get M2 and K otherwise exception is thrown
        ver.authenticate(M1_fc, M2_to_client, K);

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
        jobject /* this */,
        jstring username,
        jstring verificator,
        jstring salt,
        jstring a,
        jstring m1
) {
    try {
        OsslSha1 hash; // We will use OpenSSL SHA1 implementation
        OsslRandom random; // We will use OpenSSL random number generator
        MemoryLookup lookup; // This stores users in memory (linked-list)

        // Load predefined N,g 1024bit RFC values
        Ng ng = Ng::predefined(1024);

        OsslMathImpl math(hash, ng);
        SrpServer srpserver(lookup, math, random, false);

        // Begin user creation

        bytes usernameBytes = Conversion::string2bytes(jstringToString(env, username));
        bytes saltBytes = Conversion::string2bytes(jstringToString(env, salt));
        bytes verificatorBytes = Conversion::string2bytes(jstringToString(env, verificator));

        User u(usernameBytes, verificatorBytes, saltBytes);

        if (!lookup.userAdd(u)) {
            __android_log_print(ANDROID_LOG_DEBUG, "JNI", "testVerifyA Error: user already exists");
        }
        // End of user creation


        // Receive A from client
        bytes A = Conversion::string2bytes(jstringToString(env, a));

        // verificator is used to authenticate one user(one session)
        SrpVerificator ver = srpserver.getVerificator(usernameBytes, A);

        // receive M1 from client
        bytes M1_fc = Conversion::string2bytes(jstringToString(env, m1));

        bytes M2_to_client;
        bytes K; // secret session key

        // if M1 is OK we get M2 and K otherwise exception is thrown
        ver.authenticate(M1_fc, M2_to_client, K);

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

extern "C" JNIEXPORT jstring JNICALL
Java_com_clearkeep_dragonsrp_NativeLib_testCreateUserNative(
        JNIEnv *env,
        jobject /* this */
) {
    try {
        OsslSha1 hash;
        OsslRandom random;
        Ng ng = Ng::predefined(1024);
        __android_log_print(ANDROID_LOG_DEBUG, "JNI", "INFO: using RFC5054 Appendix A 1024 bit N,g pair");
        OsslMathImpl math(hash, ng);

        SrpClient srpclient(math, random, false);

        string strUsername = "linh";
        string strPassword = "12345678";

        bytes username = Conversion::string2bytes(strUsername);
        bytes password = Conversion::string2bytes(strPassword);

        bytes salt;
        if (salt.size() == 0) salt = random.getRandom(SALTLEN);

        bytes verificator = math.calculateVerificator(username, password, salt);

        __android_log_print(ANDROID_LOG_DEBUG, "JNI", "*** RESULT ***");
        __android_log_print(ANDROID_LOG_DEBUG, "JNI", "username: %s", strUsername.c_str());
        __android_log_print(ANDROID_LOG_DEBUG, "JNI", "salt: %s", bytesToString(salt).c_str());

        std::vector<unsigned char>::const_iterator from = salt.begin();
        std::vector<unsigned char>::const_iterator to = salt.end();
        for ( ; from!=to; ++from ) {
            __android_log_print(ANDROID_LOG_DEBUG, "JNI", "%02X", *from);
        }

        __android_log_print(ANDROID_LOG_DEBUG, "JNI", "verificator: %s", bytesToString(verificator).c_str());
        std::vector<unsigned char>::const_iterator from1 = verificator.begin();
        std::vector<unsigned char>::const_iterator to1 = verificator.end();
        for ( ; from1!=to1; ++from1 ) {
            __android_log_print(ANDROID_LOG_DEBUG, "JNI", "%02X", *from1);
        }

        __android_log_print(ANDROID_LOG_DEBUG, "JNI", "ok - you can now use these parameters in server_test program");
        return stringToJstring(env, "");
    }
    catch (DsrpException e)
    {
        __android_log_print(ANDROID_LOG_DEBUG, "JNI", "testVerifyA Error: user already exists");
        return stringToJstring(env, e.what());
    }
    catch (...)
    {
        return stringToJstring(env, "Unknown exception");
    }
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_clearkeep_dragonsrp_NativeLib_testClientFlowNative(
        JNIEnv *env,
        jobject /* this */
) {
    try {
        OsslSha1 hash;
        OsslRandom random;
        Ng ng = Ng::predefined(1024);
        OsslMathImpl math(hash, ng);

        SrpClient srpclient(math, random, false);

        // ask user for credentials
        string strUsername = "linh";
        string strPassword = "12345678";

        bytes username = Conversion::string2bytes(strUsername);
        bytes password = Conversion::string2bytes(strPassword);

        SrpClientAuthenticator sca = srpclient.getAuthenticator(username, password);

        // send username and A to server
        bytes A = sca.getA();
        __android_log_print(ANDROID_LOG_DEBUG, "JNI", "A: %s", bytesToString(A).c_str());

        // receive salt and B from server
        bytes salt = Conversion::readBytesHexForce("salt(from server)");
        bytes B = Conversion::readBytesHexForce("B(from server)");

        // send M1 to server
        bytes M1 = srpclient.getM1(salt, B, sca);
        __android_log_print(ANDROID_LOG_DEBUG, "JNI", "M1(send to server): %s", bytesToString(M1).c_str());

        // receive M2 from server (or nothing if auth on server side not successful!)
        bytes M2 = Conversion::readBytesHexForce("M2(from server)");
        // if M2 matches we get K
        bytes K = sca.getSessionKey(M2); // this throws exception on bad password

        // display shared secret
        __android_log_print(ANDROID_LOG_DEBUG, "JNI", "M1(send to server): %s", bytesToString(M1).c_str());

        // if we get here, no exception was thrown
        // if auth fails DsrpException is thrown
        __android_log_print(ANDROID_LOG_DEBUG, "JNI", "authentification successful");
        return stringToJstring(env, "");
    }
    catch (DsrpException e)
    {
        __android_log_print(ANDROID_LOG_DEBUG, "JNI", "testVerifyA Error: user already exists");
        return stringToJstring(env, e.what());
    }
    catch (...)
    {
        return stringToJstring(env, "Unknown exception");
    }
}

