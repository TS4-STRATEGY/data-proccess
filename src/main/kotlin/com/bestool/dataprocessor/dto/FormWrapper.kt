package com.bestool.dataprocessor.dto

import org.springframework.web.multipart.MultipartFile

data class FormWrapper(
    var file: MultipartFile? = null,
    var title: String? = null,
    var description: String? = null
)