package flashsalesystem.inventoryservice.config;

import org.springframework.boot.cache.autoconfigure.RedisCacheManagerBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

import java.time.Duration;

@Configuration
public class RedisConfig
{
    @Bean
    public RedisCacheManagerBuilderCustomizer redisCacheManagerBuilderCustomizer()
    {
        return builder -> builder
                .withCacheConfiguration("product",
                        RedisCacheConfiguration.defaultCacheConfig().entryTtl(Duration.ofSeconds(30))
                                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(GenericJacksonJsonRedisSerializer.builder().enableUnsafeDefaultTyping().build())))
                .withCacheConfiguration("productList",
                        RedisCacheConfiguration.defaultCacheConfig().entryTtl(Duration.ofSeconds(60))
                                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(GenericJacksonJsonRedisSerializer.builder().enableUnsafeDefaultTyping().build())));
    }
}
