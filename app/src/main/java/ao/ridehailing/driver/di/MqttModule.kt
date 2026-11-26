package ao.ridehailing.driver.di

import ao.ridehailing.driver.BuildConfig
import ao.ridehailing.driver.core.mqtt.HiveMqttGateway
import ao.ridehailing.driver.core.mqtt.MqttConnectionManager
import ao.ridehailing.driver.core.mqtt.MqttEndpointStore
import ao.ridehailing.driver.core.mqtt.MqttGateway
import ao.ridehailing.driver.core.mqtt.MqttMessageBus
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object MqttModule {
    @Provides
    @Singleton
    fun gateway(): MqttGateway = HiveMqttGateway()

    @Provides
    @Singleton
    fun bus(): MqttMessageBus = MqttMessageBus()

    @Provides
    @Singleton
    @AppScope
    fun appScope(): CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Provides
    @Singleton
    fun manager(
        gateway: MqttGateway,
        bus: MqttMessageBus,
        @AppScope scope: CoroutineScope,
        endpoints: MqttEndpointStore,
    ): MqttConnectionManager = MqttConnectionManager(
        gateway,
        bus,
        scope,
        brokerHost = { endpoints.current()?.host.orEmpty() },
        brokerPort = { endpoints.current()?.port ?: 0 },
        username = BuildConfig.MQTT_USERNAME,
        password = BuildConfig.MQTT_PASSWORD,
    )
}
