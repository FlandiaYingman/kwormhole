package top.anagke.kwormhole.controller

import org.springframework.context.ApplicationEvent
import top.anagke.kwormhole.Kfr


class KfrEvent(
    source: Any,
    val record: Kfr
) : ApplicationEvent(source)