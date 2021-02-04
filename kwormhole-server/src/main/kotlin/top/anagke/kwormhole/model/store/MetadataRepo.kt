package top.anagke.kwormhole.model.store

import org.springframework.data.jpa.repository.JpaRepository

internal interface MetadataRepo : JpaRepository<MetadataEntity, String>