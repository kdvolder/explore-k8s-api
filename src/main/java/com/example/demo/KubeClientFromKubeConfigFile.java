package com.example.demo;

import java.io.FileReader;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.kubernetes.client.ApiClient;
import io.kubernetes.client.apis.CoreV1Api;
import io.kubernetes.client.util.ClientBuilder;
import io.kubernetes.client.util.KubeConfig;

/**
 * A simple example of how to use the Java API from an application outside a kubernetes cluster
 *
 * <p>Easiest way to run this: mvn exec:java
 * -Dexec.mainClass="io.kubernetes.client.examples.KubeConfigFileClientExample"
 *
 * <p>From inside $REPO_DIR/examples
 */
@Configuration
public class KubeClientFromKubeConfigFile {

	@Autowired
	KubeProperties props;

	@Bean
	ApiClient k8sApiclient() throws Exception {
		// set the global default api-client to the in-cluster one from above
		return ClientBuilder.kubeconfig(KubeConfig.loadKubeConfig(new FileReader(props.getKubeConfigPath()))).build();
	}
}