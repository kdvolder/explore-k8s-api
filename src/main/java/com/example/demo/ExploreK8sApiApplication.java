package com.example.demo;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import com.google.common.collect.ImmutableMap;

import io.kubernetes.client.ApiClient;
import io.kubernetes.client.apis.ExtensionsV1beta1Api;
import io.kubernetes.client.models.ExtensionsV1beta1Deployment;
import io.kubernetes.client.models.ExtensionsV1beta1DeploymentBuilder;
import io.kubernetes.client.models.ExtensionsV1beta1DeploymentSpecBuilder;
import io.kubernetes.client.models.V1ContainerPortBuilder;
import io.kubernetes.client.models.V1ObjectMeta;
import io.kubernetes.client.models.V1PodSecurityContextBuilder;
import io.kubernetes.client.models.V1PodTemplateSpecBuilder;
import io.kubernetes.client.models.V1VolumeMountBuilder;

@SpringBootApplication
public class ExploreK8sApiApplication implements CommandLineRunner {

	public static void main(String[] args) {
		SpringApplication.run(ExploreK8sApiApplication.class, args);
	}

	@Autowired
	ApiClient client;
	
	@Override
	public void run(String... args) throws Exception {
		//kubectl create deployment hello-node --image=gcr.io/hello-minikube-zero-install/hello-node

		client.setDebugging(true);
		ExtensionsV1beta1Deployment r = deployTheiaEnvironment("https://github.com/spring-projects/spring-petclinic.git");
		System.out.println("Deploy success!");
		System.out.println(r);
	}

	private ExtensionsV1beta1Deployment deployTheiaEnvironment(String gitRepoUrl) throws Exception {
		String appName = "theia-spring-boot";
		String dockerImage = "kdvolder/theia-spring-boot";
		String deploymentName = appName+"-"+UUID.randomUUID();
		ExtensionsV1beta1Deployment body = new ExtensionsV1beta1DeploymentBuilder()
		.withMetadata(new V1ObjectMeta().name(deploymentName)
			.labels(ImmutableMap.of("app", appName))
		)
		.withSpec(new ExtensionsV1beta1DeploymentSpecBuilder()
			.withTemplate(new V1PodTemplateSpecBuilder()
				.withMetadata(new V1ObjectMeta().name(appName)
						.labels(ImmutableMap.of("app", appName))
				)
				.withNewSpec()
					.withSecurityContext(new V1PodSecurityContextBuilder()
						.withRunAsUser(0L) //Not sure why this is necessary, as far I understand, it shouldn't be. 
											// But for some reason, if we don't force to run as 'root' then git clone is run
											// as another user which causes permission issues for the cloned files.
						.build()
					)
					.addNewContainer()
						.withName(appName)
						.withImage(dockerImage)
						.withPorts(new V1ContainerPortBuilder()
							.withContainerPort(3000)
							.build()
						)
						.withVolumeMounts(new V1VolumeMountBuilder()
							.withName("source")
							.withMountPath("/home/project")
							.build()
						)
//						.withResources(new V1ResourceRequirementsBuilder()
//							.withRequests(ImmutableMap.of(
//								"cpu", Quantity.fromString("2000m"),
//								"memory",  Quantity.fromString("1000M")
//							))
//							.build()
//						)
					.endContainer()
					.addNewInitContainer()
						.withName("git-clone")
						.withImage("alpine/git")
						.withWorkingDir("/tmp/git")
						.withCommand(
							"git", "clone", gitRepoUrl
						)
						.withVolumeMounts(new V1VolumeMountBuilder()
							.withName("source")
							.withMountPath("/tmp/git")
							.build()
						)
					.endInitContainer()
					.addNewVolume()
						.withName("source")
						.withNewEmptyDir()
						.endEmptyDir()
					.endVolume()
				.endSpec()
				.build()
			)
			.build()
		)
		.build();
		
		ExtensionsV1beta1Api api = new ExtensionsV1beta1Api(client);
		ExtensionsV1beta1Deployment r = api.createNamespacedDeployment(
				/*namespace*/ "default", 
				body, 
				/*includeUninitialized*/ true, 
				/*pretty*/ null, 
				/*dryRun*/ null
		);
		return r;
	}
}
