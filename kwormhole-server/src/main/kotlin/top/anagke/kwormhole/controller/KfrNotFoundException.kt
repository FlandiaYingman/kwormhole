package top.anagke.kwormhole.controller

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

@ResponseStatus(value = HttpStatus.NOT_FOUND, reason = "KFR not found")
class KfrNotFoundException(kfrPath: String) : Exception("$kfrPath not found")