package com.clearkeep.utilities

fun isImageMessage(content: String): Boolean {
    return content.contains(remoteImageRegex) || content.contains(tempImageRegex) || content.contains("content://.+/external_files/Pictures/.+".toRegex())
}

fun isFileMessage(content: String): Boolean {
    return content.contains(remoteFileRegex) || content.contains(tempFileRegex)
}

fun isTempMessage(content: String): Boolean {
    return content.contains(tempFileRegex) || content.contains(tempImageRegex) || content.contains(tempImageRegex2)
}

fun getImageUriStrings(content: String): List<String> {
    val temp = remoteImageRegex.findAll(content).map {
        it.value.split(" ")
    }.toMutableList()
    temp.add(tempImageRegex.findAll(content).map { it.value.split(" ") }.toList().flatten())
    temp.add(tempImageRegex2.findAll(content).map { it.value.split(" ").filter{ isImageMessage(it) } }.toList().flatten())
    return temp.flatten().map{ it.trim() }.filter { it.isNotBlank() }
}

fun getFileUriStrings(content: String): List<String> {
    val temp = remoteFileRegex.findAll(content).map {
        it.value.split(" ")
    }.toMutableList()
    temp.add(tempFileRegex.findAll(content).map { it.value.split(" ") }.toList().flatten())
    return temp.flatten()
}

fun getFileNameFromUrl(url: String): String {
    val fileNameRegex = "(?:.(?!\\/))+\$".toRegex()
    val fileName = fileNameRegex.find(url)?.value?.replace(fileSizeRegex, "")
    return fileName?.substring(1 until fileName.length) ?: ""
}

fun getFileUrl(urlWithFileSize: String): String {
    return urlWithFileSize.replace(fileSizeRegex, "")
}

fun getMessageContent(content: String): String {
    val temp = remoteImageRegex.replace(content, "")
    val temp2 = remoteFileRegex.replace(temp, "")
    val temp3 = tempFileRegex.replace(temp2, "")
    return tempImageRegex.replace(temp3, "")
}

val remoteImageRegex =
    "(https://s3.amazonaws.com/storage.clearkeep.io/[a-zA-Z0-9\\/\\_\\-\\.]+(\\.png|\\.jpeg|\\.jpg|\\.gif|\\.PNG|\\.JPEG|\\.JPG|\\.GIF))".toRegex()

val remoteFileRegex =
    "(https://s3.amazonaws.com/storage.clearkeep.io/.+)".toRegex()

val tempImageRegex =
    "content://media/external/images/media/\\d+".toRegex()

val tempFileRegex =
    "content://.+".toRegex()

val tempImageRegex2 =
    "content://.+/external_files/Pictures/.+".toRegex()

val fileSizeRegex = "\\|.+".toRegex()