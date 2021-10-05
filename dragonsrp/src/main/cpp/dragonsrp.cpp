#include <jni.h>
#include <string>

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

using namespace DragonSRP;
using namespace DragonSRP::Ossl;
using namespace std;

extern "C" JNIEXPORT jstring JNICALL
Java_com_clearkeep_dragonsrp_NativeLib_stringFromJNI(
        JNIEnv* env,
        jobject /* this */) {
    std::string hello = "Hello from C++";
    return env->NewStringUTF(hello.c_str());
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_clearkeep_dragonsrp_NativeLib_getA(
        JNIEnv* env,
        jobject /* this */,
        jstring username,
        jstring rawPassword
) {
    try {
        OsslSha1 hash;
        OsslRandom random;
        Ng ng = Ng::predefined(1024);
        cout << "INFO: using RFC5054 Appendix A 1024 bit N,g pair" << endl;
        OsslMathImpl math(hash, ng);
    }
}