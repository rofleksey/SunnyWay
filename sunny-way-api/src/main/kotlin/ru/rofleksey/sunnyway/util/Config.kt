package ru.rofleksey.sunnyway.util

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@EnableConfigurationProperties
@ConfigurationProperties
open class Config(var shardCount: Int = 8, var queueSize: Int = 256)