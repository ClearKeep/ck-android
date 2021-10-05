#include <jni.h>
#include <string>
#include <android/log.h>
#include <iomanip>
#include <sstream>
#include <dsrp/memorylookup.hpp>

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
        bHex << std::hex << (int) *from;
    }

    return bHex.str();
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

    std::vector<unsigned char>::const_iterator from = salt.begin();
    std::vector<unsigned char>::const_iterator to = salt.end();

    std::stringstream saltHex;

    for ( ; from!=to; ++from ) {
//        __android_log_print(ANDROID_LOG_DEBUG, "JNI", "%02X", *from);
        saltHex << std::hex << (int) *from;
    }

    __android_log_print(ANDROID_LOG_DEBUG, "JNI", "getSalt saltHex %s", saltHex.str().c_str());
    printf("getSalt converted to jstring");
    __android_log_print(ANDROID_LOG_DEBUG, "JNI","getSalt converted to jstring");

    return stringToJstring(env, saltHex.str());
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
        jobject /* this */,
        jstring username,
        jstring rawPassword
) {
    try {
        OsslSha1 hash;
        OsslRandom random;
        Ng ng = Ng::predefined(1024);
        OsslMathImpl math(hash, ng);

        SrpClient srpclient(math, random, false);

        string convertedUsername = jstringToString(env, username);
        string convertedPassword = jstringToString(env, rawPassword);

        bytes usernameBytes = DragonSRP::Conversion::string2bytes(convertedUsername);
        bytes passwordBytes = DragonSRP::Conversion::string2bytes(convertedPassword);

        SrpClientAuthenticator sca = srpclient.getAuthenticator(usernameBytes, passwordBytes);

        string aString = bytesToString(sca.getA());
        __android_log_print(ANDROID_LOG_DEBUG, "JNI", "getA %s", aString.c_str());

        return stringToJstring(env, aString);
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
//extern "C" JNIEXPORT jstring JNICALL
//Java_com_clearkeep_dragonsrp_NativeLib_testVerify(
//        JNIEnv *env,
//        jobject /* this */,
//        jstring username,
//        jstring rawPassword
//) {
//    try {
//        OsslSha1 hash; // We will use OpenSSL SHA1 implementation
//        OsslRandom random; // We will use OpenSSL random number generator
//        MemoryLookup lookup; // This stores users in memory (linked-list)
//
//        // Load predefined N,g 1024bit RFC values
//        Ng ng = Ng::predefined(1024);
//
//        OsslMathImpl math(hash, ng);
//        SrpServer srpserver(lookup, math, random, false);
//
//        // Begin user creation
//        std::string strUsername;
//        cout << "username: ";
//        cin >> strUsername;
//        cin.ignore();
//
//        bytes username = Conversion::string2bytes(strUsername);
//        bytes verificator = Conversion::readBytesHexForce("verificator");
//        bytes salt = Conversion::readBytesHexForce("salt");
//
//        User u(username, verificator, salt);
//
//        if (!lookup.userAdd(u)) {
//            cout << "Error: user already exists" << endl;
//        }
//        // End of user creation
//
//
//        // Receive A from client
//        bytes A = Conversion::readBytesHexForce("A(from client)");
//
//        // verifivator is used to authenticate one user(one session)
//        SrpVerificator ver = srpserver.getVerificator(username, A);
//
//        // Send salt and B to client
//        cout << "salt(send to client): ";
//        Conversion::printBytes(ver.getSalt());
//        cout << endl;
//
//        cout << "B(send to client): ";
//        Conversion::printBytes(ver.getB());
//        cout << endl;
//
//        // receive M1 from client
//        bytes M1_fc = Conversion::readBytesHexForce("M1(from client)");
//
//        bytes M2_to_client;
//        bytes K; // secret session key
//
//        // if M1 is OK we get M2 and K otherwise exception is thrown
//        ver.authenticate(M1_fc, M2_to_client, K);
//
//        // send M2 to client
//        cout << "M2 (send to client): ";
//        Conversion::printBytes(M2_to_client);
//        cout << endl;
//
//        // display shared secret
//        cout << "shared secret session key is: ";
//        Conversion::printBytes(K);
//        cout << endl;
//
//        // if we get here, no exception was thrown
//        // if auth fails DsrpException is thrown
//        cout << "authentification successful" << endl;
//        return 0;
//    }
//    catch (UserNotFoundException e) {
//        return env->NewStringUTF(e.what().c_str());
//    } catch (DsrpException e) {
//        return env->NewStringUTF(e.what().c_str());
//    } catch (...) {
//        string error = "Unknown exception";
//        return env->NewStringUTF(error.c_str());
//    }
//}