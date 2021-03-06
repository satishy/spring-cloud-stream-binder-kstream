/*
 * Copyright 2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.stream.kstream.config;

import java.util.Properties;

import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.kstream.KStreamBuilder;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.UnsatisfiedDependencyException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.config.BindingServiceProperties;
import org.springframework.cloud.stream.converter.CompositeMessageConverterFactory;
import org.springframework.cloud.stream.kstream.KStreamBoundElementFactory;
import org.springframework.cloud.stream.kstream.KStreamListenerParameterAdapter;
import org.springframework.cloud.stream.kstream.KStreamStreamListenerResultAdapter;
import org.springframework.context.annotation.Bean;
import org.springframework.integration.codec.Codec;
import org.springframework.kafka.annotation.EnableKafkaStreams;
import org.springframework.kafka.annotation.KafkaStreamsDefaultConfiguration;
import org.springframework.kafka.core.KStreamBuilderFactoryBean;
import org.springframework.util.ObjectUtils;

import static org.springframework.kafka.annotation.KafkaStreamsDefaultConfiguration.DEFAULT_KSTREAM_BUILDER_BEAN_NAME;
import static org.springframework.kafka.annotation.KafkaStreamsDefaultConfiguration.DEFAULT_STREAMS_CONFIG_BEAN_NAME;

/**
 * @author Marius Bogoevici
 */

@EnableBinding
@EnableConfigurationProperties(KStreamBinderProperties.class)
@EnableKafkaStreams
public class KStreamBinderSupportAutoConfiguration {

	@Bean(name = DEFAULT_KSTREAM_BUILDER_BEAN_NAME)
	public KStreamBuilderFactoryBean defaultKStreamBuilder(
			@Qualifier(DEFAULT_STREAMS_CONFIG_BEAN_NAME) ObjectProvider<StreamsConfig> streamsConfigProvider) {
		StreamsConfig streamsConfig = streamsConfigProvider.getIfAvailable();
		if (streamsConfig != null) {
			KStreamBuilderFactoryBean kStreamBuilderFactoryBean = new KStreamBuilderFactoryBean(streamsConfig);
			kStreamBuilderFactoryBean.setPhase(Integer.MAX_VALUE - 500);
			return kStreamBuilderFactoryBean;
		}
		else {
			throw new UnsatisfiedDependencyException(KafkaStreamsDefaultConfiguration.class.getName(),
					DEFAULT_KSTREAM_BUILDER_BEAN_NAME, "streamsConfig",
					"There is no '" + DEFAULT_STREAMS_CONFIG_BEAN_NAME
							+ "' StreamsConfig bean in the application context.\n"
							+ "Consider to declare one or don't use @EnableKafkaStreams.");
		}
	}

	@Bean(KafkaStreamsDefaultConfiguration.DEFAULT_STREAMS_CONFIG_BEAN_NAME)
	public StreamsConfig streamsConfig(KStreamBinderProperties kStreamBinderProperties) {
		Properties props = new Properties();
		props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, kStreamBinderProperties.getKafkaConnectionString());
		props.put(StreamsConfig.KEY_SERDE_CLASS_CONFIG, Serdes.ByteArraySerde.class.getName());
		props.put(StreamsConfig.VALUE_SERDE_CLASS_CONFIG, Serdes.ByteArraySerde.class.getName());
		props.put(StreamsConfig.APPLICATION_ID_CONFIG, "default");
		if (!ObjectUtils.isEmpty(kStreamBinderProperties.getStreamConfiguration())) {
			props.putAll(kStreamBinderProperties.getStreamConfiguration());
		}
		return new StreamsConfig(props);
	}

	@Bean
	public KStreamStreamListenerResultAdapter kStreamStreamListenerResultAdapter() {
		return new KStreamStreamListenerResultAdapter();
	}

	@Bean
	public KStreamListenerParameterAdapter kStreamListenerParameterAdapter(
			CompositeMessageConverterFactory compositeMessageConverterFactory) {
		return new KStreamListenerParameterAdapter(
				compositeMessageConverterFactory.getMessageConverterForAllRegistered());
	}

	@Bean
	public KStreamBoundElementFactory kStreamBindableTargetFactory(KStreamBuilder kStreamBuilder,
			BindingServiceProperties bindingServiceProperties, Codec codec,
			CompositeMessageConverterFactory compositeMessageConverterFactory) {
		return new KStreamBoundElementFactory(kStreamBuilder, bindingServiceProperties, codec,
				compositeMessageConverterFactory);
	}

}
