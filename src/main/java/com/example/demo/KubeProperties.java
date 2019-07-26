package com.example.demo;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties("my.k8s")
public class KubeProperties {

	private String kubeConfigPath = System.getProperty("user.home") +"/.kube/config";

	public String getKubeConfigPath() {
		return kubeConfigPath;
	}

	public void setKubeConfigPath(String kubeConfigPath) {
		this.kubeConfigPath = kubeConfigPath;
	}

}
