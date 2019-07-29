package com.example.demo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import com.google.common.collect.ImmutableMap;

import io.kubernetes.client.ApiClient;
import io.kubernetes.client.apis.CoreV1Api;
import io.kubernetes.client.apis.ExtensionsV1beta1Api;
import io.kubernetes.client.models.ExtensionsV1beta1Deployment;
import io.kubernetes.client.models.ExtensionsV1beta1DeploymentBuilder;
import io.kubernetes.client.models.ExtensionsV1beta1DeploymentSpec;
import io.kubernetes.client.models.ExtensionsV1beta1DeploymentSpecBuilder;
import io.kubernetes.client.models.V1Container;
import io.kubernetes.client.models.V1LabelSelector;
import io.kubernetes.client.models.V1LabelSelectorBuilder;
import io.kubernetes.client.models.V1ObjectMeta;
import io.kubernetes.client.models.V1Pod;
import io.kubernetes.client.models.V1PodList;
import io.kubernetes.client.models.V1PodSpec;
import io.kubernetes.client.models.V1PodTemplateSpec;
import io.kubernetes.client.models.V1PodTemplateSpecBuilder;

@SpringBootApplication
public class ExploreK8sApiApplication implements CommandLineRunner {

	public static void main(String[] args) {
		SpringApplication.run(ExploreK8sApiApplication.class, args);
	}

	@Autowired
	ApiClient client;
	
//	@Override
//	public void run(String... args) throws Exception {
//	    // invokes the CoreV1Api client
//		CoreV1Api api = new CoreV1Api(client);
//	    V1PodList list = api.listPodForAllNamespaces(null, null, null, null, null, null, null, null, null);
//	    for (V1Pod item : list.getItems()) {
//	      System.out.println(item.getMetadata().getName());
//	    }
//
//	}

	@Override
	public void run(String... args) throws Exception {
		//kubectl create deployment hello-node --image=gcr.io/hello-minikube-zero-install/hello-node

		client.setDebugging(true);

		V1ObjectMeta metadata = new V1ObjectMeta()
				.name("hello-node")
				.labels(ImmutableMap.of("app", "hello-node"));
		
		ExtensionsV1beta1Deployment body = new ExtensionsV1beta1DeploymentBuilder()
		.withMetadata(metadata)
		.withSpec(new ExtensionsV1beta1DeploymentSpecBuilder()
			.withTemplate(new V1PodTemplateSpecBuilder()
				.withMetadata(metadata)
				.withNewSpec()
					.addNewContainer()
						.withName("hello-node")
						.withImage("gcr.io/hello-minikube-zero-install/hello-node")
					.endContainer()
				.endSpec()
				.build()
			)
			.build()
		)
		.build();
		
		ExtensionsV1beta1Api api = new ExtensionsV1beta1Api(client);
		try {
			ExtensionsV1beta1Deployment r = api.createNamespacedDeployment(
					/*namespace*/ "default", 
					body, 
					/*includeUninitialized*/ true, 
					/*pretty*/ null, 
					/*dryRun*/ null
			);
			System.out.println(r);
		} catch (Exception e) {
			System.out.println(e);
		}
	}
	
}
