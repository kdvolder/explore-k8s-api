package com.example.demo;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import com.google.common.collect.ImmutableMap;

import io.kubernetes.client.ApiClient;
import io.kubernetes.client.ApiException;
import io.kubernetes.client.apis.CoreV1Api;
import io.kubernetes.client.apis.ExtensionsV1beta1Api;
import io.kubernetes.client.custom.IntOrString;
import io.kubernetes.client.models.ExtensionsV1beta1Deployment;
import io.kubernetes.client.models.ExtensionsV1beta1DeploymentBuilder;
import io.kubernetes.client.models.ExtensionsV1beta1DeploymentSpecBuilder;
import io.kubernetes.client.models.ExtensionsV1beta1DeploymentStatus;
import io.kubernetes.client.models.V1ContainerPortBuilder;
import io.kubernetes.client.models.V1Node;
import io.kubernetes.client.models.V1NodeAddress;
import io.kubernetes.client.models.V1NodeList;
import io.kubernetes.client.models.V1ObjectMeta;
import io.kubernetes.client.models.V1PodSecurityContextBuilder;
import io.kubernetes.client.models.V1PodTemplateSpecBuilder;
import io.kubernetes.client.models.V1Service;
import io.kubernetes.client.models.V1ServiceBuilder;
import io.kubernetes.client.models.V1ServicePort;
import io.kubernetes.client.models.V1ServicePortBuilder;
import io.kubernetes.client.models.V1ServiceSpecBuilder;
import io.kubernetes.client.models.V1VolumeMountBuilder;

@SpringBootApplication
public class ExploreK8sApiApplication implements CommandLineRunner {

	private static final String namespace = "default";

	public static void main(String[] args) {
		SpringApplication.run(ExploreK8sApiApplication.class, args);
	}

	@Autowired
	ApiClient client;
	
	@Override
	public void run(String... args) throws Exception {
		//kubectl create deployment hello-node --image=gcr.io/hello-minikube-zero-install/hello-node
		client.setDebugging(true);
		V1Service r = deployTheiaEnvironment("https://github.com/spring-projects/spring-petclinic.git");
		System.out.println("Deploy success!");
		System.out.println(r);

		
		for (V1ServicePort p : r.getSpec().getPorts()) {
			if (p.getName().equals("theia")) {
				Integer nodePort = p.getNodePort();
				String nodeIp = getNodeIp();
				System.out.println("Running at: http://"+nodeIp+":"+nodePort);
			}
		}
	}

	private String getNodeIp() throws ApiException {
		//This part here is a bit 'iffy' it works on Linux with 'kind' cluster, because
		//a cluster node's 'InternalIP' are actually visible to the host machine via the 'docker0' network bridge.
		//In other environments we probably need additional machinery to expose services so that
		//they can be visible/accessible to user's browser.
		
		String[] typePreference = {
				"ExternalIP", "InternalIP"
		};
		V1NodeList nodes = new CoreV1Api(client).listNode(null, null, null, null, null, null, null, null, null);
		V1Node node = nodes.getItems().get(0); //don't really care which node
		for (String type : typePreference) {
			for (V1NodeAddress a : node.getStatus().getAddresses()) {
				if (type.equals(a.getType())) {
					return a.getAddress();
				}
			}
		} 
		return null;
	}

	private V1Service deployTheiaEnvironment(String gitRepoUrl) throws Exception {
		String appName = "theia-spring-boot";
		String dockerImage = "kdvolder/theia-spring-boot";
		String id = UUID.randomUUID().toString();
		
		ImmutableMap<String, String> labels = ImmutableMap.of("app", appName, "deploymentId", id);
		
		ExtensionsV1beta1Deployment body = new ExtensionsV1beta1DeploymentBuilder()
		.withMetadata(new V1ObjectMeta().generateName(appName)
			.labels(labels)
		)
		.withSpec(new ExtensionsV1beta1DeploymentSpecBuilder()
			.withTemplate(new V1PodTemplateSpecBuilder()
				.withMetadata(new V1ObjectMeta().name(appName)
						.labels(labels)
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
				namespace, 
				body, 
				/*includeUninitialized*/ true, 
				/*pretty*/ null, 
				/*dryRun*/ null
		);
		
		ExtensionsV1beta1DeploymentStatus status = r.getStatus();
		System.out.println(status);
		while (status.getReadyReplicas()==null || status.getReadyReplicas()<1) {
			status = api.readNamespacedDeployment(r.getMetadata().getName(), namespace, null, true, null).getStatus();
			System.out.println(status);
		}
		return createServiceFor(r);
	}

	private V1Service createServiceFor(ExtensionsV1beta1Deployment deployment) throws ApiException {
		V1Service body = new V1ServiceBuilder()
			.withNewMetadata()
			.withName(deployment.getMetadata().getName())
			.withLabels(deployment.getMetadata().getLabels())
			.endMetadata()
			.withSpec(new V1ServiceSpecBuilder()
				.withType("NodePort")
				.withSelector(ImmutableMap.of("deploymentId", deployment.getMetadata().getLabels().get("deploymentId")))
				.withPorts(new V1ServicePortBuilder()
					.withName("theia")
					.withTargetPort(new IntOrString(3000))
					.withPort(80)
					.build()
				)
				.build()
			)
			.build();
		return new CoreV1Api(client).createNamespacedService(namespace, body, true, null, null);
	}
}
