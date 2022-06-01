package com.rh.client;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.kie.server.api.marshalling.MarshallingFormat;
import org.kie.server.api.model.instance.TaskSummary;
import org.kie.server.client.KieServicesClient;
import org.kie.server.client.KieServicesConfiguration;
import org.kie.server.client.KieServicesFactory;
import org.kie.server.client.ProcessServicesClient;
import org.kie.server.client.UserTaskServicesClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KieClient {

	final static Logger log = LoggerFactory.getLogger(KieClient.class);

	// private static final String URL = "http://localhost:8080/kie-server/services/rest/server";
	private static final String URL = "http://localhost:8090/rest/server";
	private static final String loggeduser = System.getProperty("username", "rhpamAdmin");
	private static final String otherUser = System.getProperty("otheruser", "kieserver");
	private static final String password = System.getProperty("password", "Pa$$w0rd");

	// CONSTANTS
	private static final String CONTAINER = "base-work-flow-v2";
	private static final String PROCESS_ID = "IPOS.base-workflow-v2";

	private KieServicesClient client;

	public static void main(String[] args) {
		KieClient clientApp = new KieClient(loggeduser, password);
		System.setProperty("org.drools.server.filter.classes", "true");
		// System.setProperty("org.kie.server.bypass.auth.user", "true");
		log.info("begin");

		Long piid = clientApp.launchProcess();
		// Long piid = 1l;
		log.info("piid {}", piid);
		
		Long taskId = clientApp.getTaskAsPotentialOwner(loggeduser);
		log.info("{} as potential user for task, {}", loggeduser, taskId);
		
		
		KieClient clientAppUser = new KieClient(otherUser, password);
		taskId = clientAppUser.getTaskAsPotentialOwner(otherUser);
		log.info("{} as potential user for task {}", otherUser, taskId);
		
		// For Reassignment 
//		clientApp.sendSignal(piid, "Reassign", "team:Lloyds,");
//		taskId = clientAppUser.getTaskAsPotentialOwner(otherUser);
//		log.info("Post Reassignment: {} as potential user for task {}", otherUser, taskId);
		
		// For Allocation
		clientApp.sendSignal(piid, "Allocate", "userId:kieserver,");
		
		
		taskId = clientAppUser.getTaskAsPotentialOwner(otherUser);
		log.info("Post Allocation: {} as potential user for task {}", otherUser, taskId);
		
		taskId = clientAppUser.getTasksOwned(otherUser);
		log.info("{} as owner for the task {}", otherUser, taskId);
		
		// clientAppUser.completeTask(taskId, "user");

		log.info("end");
	}

	public KieClient(String user, String password) {
		client = getClient(user, password);
	}
	

	public Long launchProcess() {
		try {
			ProcessServicesClient processClient = client.getServicesClient(ProcessServicesClient.class);
			Map<String, Object> inputData = new HashMap<>();

			setInputData(inputData);
			return processClient.startProcess(CONTAINER, PROCESS_ID, inputData);

		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
	private void setInputData(Map<String, Object> inputData) {
	}

	public long getTaskAsPotentialOwner(String userName) {
		UserTaskServicesClient userTaskServicesClient = client.getServicesClient(UserTaskServicesClient.class);
		List<TaskSummary> tasks = userTaskServicesClient.findTasksAssignedAsPotentialOwner(userName, 0, 10);
		if (!tasks.isEmpty()) {
			TaskSummary ts = (TaskSummary)tasks.get(0);
			return ts.getId();
		}
		return 0;
	}
	
	public long getTasksOwned(String userName) {
		UserTaskServicesClient userTaskServicesClient = client.getServicesClient(UserTaskServicesClient.class);
		List<TaskSummary> tasks = userTaskServicesClient.findTasksOwned(userName, 0, 10);
		if (!tasks.isEmpty()) {
			TaskSummary ts = (TaskSummary)tasks.get(0);
			return ts.getId();
		}
		System.out.println("1 [" + tasks);
		tasks = userTaskServicesClient.findTasksOwned(userName, 1, 10);
		if (!tasks.isEmpty()) {
			TaskSummary ts = (TaskSummary)tasks.get(0);
			return ts.getId();
		}
		System.out.println("1 [" + tasks);
		tasks = userTaskServicesClient.findTasksOwned(userName, 0, 10, "TASKID", true);
		if (!tasks.isEmpty()) {
			TaskSummary ts = (TaskSummary)tasks.get(0);
			return ts.getId();
		}
		System.out.println("1 [" + tasks);
		return 0;
	}
	
	public void completeTask(Long taskId, String userName) {
		UserTaskServicesClient userTaskServicesClient = client.getServicesClient(UserTaskServicesClient.class);
		userTaskServicesClient.startTask(CONTAINER, taskId, userName);
		Map<String, Object> inputData = new HashMap<>();
		userTaskServicesClient.completeAutoProgress(CONTAINER, taskId, userName, inputData);
	}
	
	private void sendSignal(Long processInstanceId, String signalName, String signalPayload) {
		ProcessServicesClient processClient = client.getServicesClient(ProcessServicesClient.class);
		processClient.signalProcessInstance(CONTAINER, processInstanceId, signalName, signalPayload);
	}
	
	private KieServicesClient getClient(String user, String password) {
		KieServicesConfiguration config = KieServicesFactory.newRestConfiguration(URL, user, password);

		// Marshalling
		config.setMarshallingFormat(MarshallingFormat.JSON);
		KieServicesClient client = KieServicesFactory.newKieServicesClient(config);

		return client;
	}
}
