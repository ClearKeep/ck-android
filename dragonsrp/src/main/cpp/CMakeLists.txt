cmake_minimum_required(VERSION 3.10.2)

project("dragonsrp")

set(DRAGON_SRP DragonSRP)

INCLUDE_DIRECTORIES (${CMAKE_SOURCE_DIR}/include ${CMAKE_SOURCE_DIR}/${DRAGON_SRP})

add_library(crypto STATIC IMPORTED)
set_target_properties(crypto
        PROPERTIES IMPORTED_LOCATION ${CMAKE_SOURCE_DIR}/libs/${ANDROID_ABI}/libcrypto.a)

add_library(ssl STATIC IMPORTED)
set_target_properties(ssl
        PROPERTIES IMPORTED_LOCATION ${CMAKE_SOURCE_DIR}/libs/${ANDROID_ABI}/libssl.a)

file (GLOB_RECURSE SOURCE_FILES_DSRP ${CMAKE_SOURCE_DIR}/${DRAGON_SRP}/dsrp/*.c ${CMAKE_SOURCE_DIR}/${DRAGON_SRP}/dsrp/*.cpp)
file(GLOB_RECURSE SOURCE_FILES_OSSL ${CMAKE_SOURCE_DIR}/${DRAGON_SRP}/ossl/*.c ${CMAKE_SOURCE_DIR}/${DRAGON_SRP}/ossl/*.cpp)

add_library(dragonsrp SHARED dragonsrp.cpp)

add_library(ossl SHARED ${SOURCE_FILES_OSSL})

add_library(dsrp SHARED ${SOURCE_FILES_DSRP})

find_library(log-lib log)

target_link_libraries(ossl ssl crypto dsrp)

target_link_libraries(dsrp ssl crypto)

target_link_libraries(dragonsrp ${log-lib} ssl crypto dsrp ossl)