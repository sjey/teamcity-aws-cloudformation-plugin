/*
 * Copyright 2000-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package jetbrains.buildServer.runner.cloudformation;

import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.cloudformation.AmazonCloudFormationClient;
import com.amazonaws.services.cloudformation.model.CreateStackRequest;
import com.amazonaws.services.cloudformation.model.DeleteStackRequest;
import com.amazonaws.services.cloudformation.model.DescribeStackEventsRequest;
import com.amazonaws.services.cloudformation.model.DescribeStackEventsResult;
import com.amazonaws.services.cloudformation.model.DescribeStacksRequest;
import com.amazonaws.services.cloudformation.model.Stack;
import com.amazonaws.services.cloudformation.model.StackEvent;
import com.amazonaws.services.cloudformation.model.StackStatus;
import com.amazonaws.services.cloudformation.model.UpdateStackRequest;
import com.amazonaws.services.cloudformation.model.ValidateTemplateRequest;

import jetbrains.buildServer.util.amazon.AWSClients;
import jetbrains.buildServer.util.amazon.AWSException;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class AWSClient {

	@NotNull
	private AmazonCloudFormationClient myCloudFormationClient;
	@Nullable
	private String myDescription;
	@NotNull
	private Listener myListener = new Listener();

	public AWSClient(@NotNull AWSClients clients) {
		myCloudFormationClient = clients.createCloudFormationClient();
	}

	@NotNull
	public AWSClient withDescription(@NotNull String description) {
		myDescription = description;
		return this;
	}

	@NotNull
	public AWSClient withListener(@NotNull Listener listener) {
		myListener = listener;
		return this;
	}

	/**
	 * Uploads application revision archive to S3 bucket named s3BucketName with
	 * the provided key and bundle type.
	 * <p>
	 * For performing this operation target AWSClient must have corresponding S3
	 * permissions.
	 *
	 * @param s3BucketName
	 *            valid S3 bucket name
	 * @param s3ObjectKey
	 *            valid S3 object key
	 */
	public void initiateCFN(@NotNull String stackName, @NotNull String region, @NotNull String s3BucketName,
			@NotNull String s3ObjectKey, @NotNull String cfnAction, @NotNull String onFailure) {
		try {
			String templateURL;
			Region reg = Region.getRegion(Regions.fromName(region));
			myCloudFormationClient.setRegion(reg);
			templateURL = getTemplateUrl(reg, s3BucketName, s3ObjectKey);
			System.out.println("The template url is " + templateURL);

			if (cfnAction.equalsIgnoreCase("Create")) {
				System.out.println("The CFN action is " + cfnAction);
				myListener.createStackStarted(stackName, region, s3BucketName, s3ObjectKey, cfnAction);
				CreateStackRequest createRequest = new CreateStackRequest();
				createRequest.setStackName(stackName);
				if (!onFailure.equalsIgnoreCase("null"))
					createRequest.setOnFailure(onFailure);
				createRequest.setTemplateURL(templateURL);
				myCloudFormationClient.createStack(createRequest);
				waitForCompletion(myCloudFormationClient, stackName);

			} else if (cfnAction.equalsIgnoreCase("Delete")) {
				myListener.deleteStarted(stackName, region);
				DeleteStackRequest deleteStackRequest = new DeleteStackRequest();
				deleteStackRequest.setStackName(stackName);
				myCloudFormationClient.deleteStack(deleteStackRequest);
				waitForDelete(myCloudFormationClient, stackName);

			} else if (cfnAction.equalsIgnoreCase("Validate")) {
				myListener.validateStarted(stackName);
				ValidateTemplateRequest validatetempRequest = new ValidateTemplateRequest();
				validatetempRequest.setTemplateURL(templateURL);
				myListener.validateFinished(
						myCloudFormationClient.validateTemplate(validatetempRequest).getParameters().toString());

			} else if (cfnAction.equalsIgnoreCase("Update")) {
				myListener.updateInProgress(stackName);
				UpdateStackRequest updateStackRequest = new UpdateStackRequest();
				updateStackRequest.setStackName(stackName);
				updateStackRequest.setTemplateURL(templateURL);
				myCloudFormationClient.updateStack(updateStackRequest);
				waitForCompletion(myCloudFormationClient, stackName);
			}
		} catch (Throwable t) {
			processFailure(t);
		}
	}

	public void waitForCompletion(AmazonCloudFormationClient stackbuilder, String stackName)
			throws InterruptedException {
		DescribeStacksRequest wait = new DescribeStacksRequest();
		wait.setStackName(stackName);
		Boolean completed = false;
		String action = "CREATE";
		String stackStatus = "Waiting";
		String stackReason = "";
		String stackId = "";
		List<String> events;
		int len, first, last;

		first = 0;

		myListener.waitForStack(stackStatus);

		while (!completed) {
			List<Stack> stacks = stackbuilder.describeStacks(wait).getStacks();
			if (stacks.isEmpty()) {
				completed = true;
				stackStatus = "NO_SUCH_STACK";
				stackReason = "Stack has been deleted";
			} else {
				for (Stack stack : stacks) {
					if (stack.getStackStatus().equals(StackStatus.CREATE_COMPLETE.toString())
							|| stack.getStackStatus().equals(StackStatus.CREATE_FAILED.toString())
							|| stack.getStackStatus().equals(StackStatus.ROLLBACK_FAILED.toString())
							|| stack.getStackStatus().equals(StackStatus.DELETE_FAILED.toString())) {
						completed = true;
						stackStatus = stack.getStackStatus();
						if (stack.getStackStatus().equals(StackStatus.CREATE_COMPLETE.toString())) {
							stackReason = "Success";
						} else {
							stackReason = "Failure";
						}
						stackId = stack.getStackId();
					}
				}
			}
			//sleep for 10 seconds
			Thread.sleep(10000);
		}
		
		if (completed) {
			events = describeStackEvents(stackbuilder, stackName, action);
			for (String event : events) {
				myListener.waitForStack(event.toString());
			}
			events.clear();

		}
		myListener.waitForStack(stackStatus);
		if (stackReason.contains("Failure")) {
			myListener.createStackFailed(stackName, stackStatus, stackReason);
		} else {
			myListener.createStackFinished(stackName, stackStatus);
		}
	}

	public void waitForDelete(AmazonCloudFormationClient stackbuilder, String stackName) throws InterruptedException {
		DescribeStacksRequest wait = new DescribeStacksRequest();
		wait.setStackName(stackName);
		String stackStatus;
		String stackReason;
		String action = "DELETE";
		Boolean delete = false;
		List<String> events;

		while (!delete) {

			List<Stack> stacks = stackbuilder.describeStacks(wait).getStacks();
			if (stacks.isEmpty()) {
				delete = true;
				stackStatus = "NO_SUCH_STACK";
				stackReason = "Stack has been deleted";
			} else {
				myListener.debugLog("From the wait for delete");
				events = describeStackEvents(stackbuilder, stackName, action);
				for (String event : events) {
					myListener.waitForStack(event.toString());
				}
				Thread.sleep(10000);
				events.clear();
			}
		}
		stackStatus = "done";
		stackReason = "Delete Complete";
		myListener.waitForStack(stackStatus);
		myListener.createStackFinished(stackName, stackStatus);
	}

	public List<String> describeStackEvents(AmazonCloudFormationClient stackbuilder, String stackName, String ACTION) {
		List<String> output = new ArrayList<String>();
		DescribeStackEventsRequest request = new DescribeStackEventsRequest();
		request.setStackName(stackName);
		DescribeStackEventsResult results = stackbuilder.describeStackEvents(request);
		for (StackEvent event : results.getStackEvents()) {
			if (event.getEventId().contains(ACTION)) {

				output.add(event.getEventId());
				// myListener.debugLog(event.toString());
			}
		}
		return output;
	}

	public String getTemplateUrl(Region region, String s3Bucket, String s3Object) {
		// "https://s3-us-west-2.amazonaws.com/" + s3BucketName + "/" +
		// s3ObjectKey;
		String templateUrl;
		// hard coded s3 bucket location
		// templateUrl = "https://s3-ap-southeast-2.amazonaws.com" + "/" + s3Bucket + "/" + s3Object;
		templateUrl = "https://" + region.getServiceEndpoint("s3") + "/" +  s3Bucket + "/" + s3Object;
		return templateUrl;
	}

	public Boolean isStackExists(@NotNull String stackName) {
		Boolean exists;
		DescribeStacksRequest wait = new DescribeStacksRequest();
		wait.setStackName(stackName);
		return true;
	}

	// public void updateEnvironmentAndWait(@NotNull String environmentName,
	// @NotNull String versionLabel,
	// int waitTimeoutSec, int waitIntervalSec) {
	// doUpdateAndWait(environmentName, versionLabel, true, waitTimeoutSec,
	// waitIntervalSec);
	// }
	//
	// /**
	// * The same as {@link #updateEnvironmentAndWait} but without waiting
	// */
	// public void updateEnvironment(@NotNull String environmentName, @NotNull
	// String versionLabel) {
	// doUpdateAndWait(environmentName, versionLabel, false, null, null);
	// }

	// @SuppressWarnings("ConstantConditions")
	// private void doUpdateAndWait(@NotNull String environmentName, @NotNull
	// String versionLabel,
	// boolean wait, @Nullable Integer waitTimeoutSec, @Nullable Integer
	// waitIntervalSec) {
	// try {
	// UpdateEnvironmentRequest request = new UpdateEnvironmentRequest()
	// .withEnvironmentName(environmentName)
	// .withVersionLabel(versionLabel);
	// UpdateEnvironmentResult result =
	// myCloudFormationClient.updateEnvironment(request);
	//
	// String environmentId = result.getEnvironmentId();
	//
	// myListener.deploymentStarted(environmentId, environmentName,
	// versionLabel);
	//
	// if (wait) {
	// waitForDeployment(environmentId, versionLabel, waitTimeoutSec,
	// waitIntervalSec);
	// }
	// } catch (Throwable t) {
	// processFailure(t);
	// }
	// }

	// private void waitForDeployment(@NotNull String environmentId, String
	// versionLabel, int waitTimeoutSec, int waitIntervalSec) {
	// myListener.deploymentWaitStarted(environmentId);
	//
	// EnvironmentDescription environment = getEnvironment(environmentId);
	// String status = getHumanReadableStatus(environment.getStatus());
	// List<EventDescription> events = getErrorEvents(environmentId,
	// versionLabel);
	// boolean hasError = events.size() > 0;
	//
	// long startTime = System.currentTimeMillis();
	//
	// while (status.equals("updating") && !hasError) {
	// myListener.deploymentInProgress(environmentId);
	//
	// if (System.currentTimeMillis() - startTime > waitTimeoutSec * 1000) {
	// myListener.deploymentFailed(environmentId,
	// environment.getApplicationName(), versionLabel, true, null);
	// return;
	// }
	//
	// try {
	// Thread.sleep(waitIntervalSec * 1000);
	// } catch (InterruptedException e) {
	// processFailure(e);
	// return;
	// }
	//
	// environment = getEnvironment(environmentId);
	// status = getHumanReadableStatus(environment.getStatus());
	// events = getErrorEvents(environmentId, versionLabel);
	// hasError = events.size() > 0;
	// }
	//
	// if (isSuccess(environment, versionLabel)) {
	// myListener.deploymentSucceeded(environmentId,
	// environment.getApplicationName(), versionLabel);
	// } else {
	// Listener.ErrorInfo errorEvent = events.size() > 0 ?
	// getErrorInfo(events.get(0)) : null;
	// myListener.deploymentFailed(environmentId,
	// environment.getApplicationName(), versionLabel, false, errorEvent);
	// }
	// }

	// public EnvironmentDescription getEnvironment(@NotNull String
	// environmentId) {
	// return myCloudFormationClient.describeEnvironments(new
	// DescribeEnvironmentsRequest().withEnvironmentIds(environmentId))
	// .getEnvironments().get(0);
	// }
	//
	// private List<EventDescription> getErrorEvents(@NotNull String
	// environmentId, String versionLabel) {
	// return myCloudFormationClient.describeEvents(new DescribeEventsRequest()
	// .withEnvironmentId(environmentId)
	// .withMaxRecords(10)
	// .withVersionLabel(versionLabel)
	// .withSeverity(EventSeverity.ERROR))
	// .getEvents();
	// }

	// private boolean isSuccess(@NotNull EnvironmentDescription environment,
	// @NotNull String versionLabel) {
	// return environment.getVersionLabel().equals(versionLabel);
	// }

	private void processFailure(@NotNull Throwable t) {
		myListener.exception(new AWSException(t));
	}

	@NotNull
	private String getHumanReadableStatus(@NotNull String status) {
		if (StackStatus.CREATE_IN_PROGRESS.toString().equals(status))
			return "launching";
		if (StackStatus.UPDATE_IN_PROGRESS.toString().equals(status))
			return "updating";
		if (StackStatus.CREATE_COMPLETE.toString().equals(status))
			return "ready";
		if (StackStatus.DELETE_COMPLETE.toString().equals(status))
			return "terminated";
		if (StackStatus.DELETE_IN_PROGRESS.toString().equals(status))
			return "terminating";
		return CloudFormationConstants.STATUS_IS_UNKNOWN;
	}

	@NotNull
	// private Listener.ErrorInfo getErrorInfo(@NotNull EventDescription event)
	// {
	// final Listener.ErrorInfo errorInfo = new Listener.ErrorInfo();
	// errorInfo.message = removeTrailingDot(event.getMessage());
	// errorInfo.severity = event.getSeverity();
	// return errorInfo;
	// }

	@Contract("null -> null")
	@Nullable
	private String removeTrailingDot(@Nullable String msg) {
		return (msg != null && msg.endsWith(".")) ? msg.substring(0, msg.length() - 1) : msg;
	}

	public static class Listener {

		void createStackStarted(@NotNull String stackName, @NotNull String region, @NotNull String s3BucketName,
				@NotNull String s3ObjectKey, @NotNull String cfnAction) {
		}

		void debugLog(String status) {
		}

		void createStackFailed(@NotNull String stackName, @NotNull String stackStatus, @NotNull String stackReason) {
		}

		void createStackFinished(@NotNull String stackName, @NotNull String stackStatus) {
		}

		void waitForStack(@NotNull String status) {
		}

		void deleteStarted(@NotNull String stackName, @NotNull String region) {
		}

		void deleteSucceeded(@NotNull String stackName) {
		}

		void validateStarted(@NotNull String stackName) {
		}

		void validateFinished(@NotNull String parameters) {
		}

		void updateInProgress(@NotNull String stackName) {
		}

		void exception(@NotNull AWSException exception) {
		}

		void deploymentFailed(@NotNull String environmentId, @NotNull String applicationName,
				@NotNull String versionLabel, @NotNull Boolean hasTimeout, @Nullable ErrorInfo errorInfo) {
		}

		public static class ErrorInfo {
			@Nullable
			String severity;
			@Nullable
			String message;
		}
	}
}
