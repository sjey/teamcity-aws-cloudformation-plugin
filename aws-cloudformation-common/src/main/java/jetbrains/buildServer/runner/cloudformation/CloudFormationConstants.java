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

public interface CloudFormationConstants {
  String RUNNER_TYPE = "aws.cloudFormation";
  String RUNNER_DISPLAY_NAME = "AWS CloudFormation";
  String RUNNER_DESCR = "Create, update, validate and delete cloudformation stacks using AWS CloudFormation";

  String DEPLOYMENT_ID_BUILD_CONFIG_PARAM = "cloudformation.deployment.id";
  String S3_OBJECT_VERSION_CONFIG_PARAM = "cloudformation.revision.s3.version";

  String EDIT_PARAMS_HTML = "editCloudFormationParams.html";
  String VIEW_PARAMS_HTML = "viewCloudFormationParams.html";
  String EDIT_PARAMS_JSP = "editCloudFormationParams.jsp";
  String VIEW_PARAMS_JSP = "viewCloudFormationParams.jsp";

  String TIMEOUT_BUILD_PROBLEM_TYPE = "CLOUDFORMATION_TIMEOUT";
  String FAILURE_BUILD_PROBLEM_TYPE = "CLOUDFORMATION_FAILURE";

  String S3_BUCKET_NAME_PARAM = "cloudformation_s3_bucket_name";
  String S3_BUCKET_NAME_LABEL = "S3 bucket";

  String S3_OBJECT_KEY_PARAM = "cloudformation_s3_object_key";
  String S3_OBJECT_KEY_LABEL = "S3 object key";

  String STACK_NAME_PARAM = "cloudformation_stack_name";
  String STACK_NAME_LABEL = "Stack Name";

  String APP_NAME_PARAM = "cloudformation_appname_label";
  String APP_NAME_LABEL = "Application Name";
  
  String CLOUDFORMATION_STACK_ACTION_PARAM = "cloudformation_stack_action";
  String CLOUDFORMATION_STACK_ACTION_LABEL = "Action";

  String APP_VERSION_PARAM = "cloudformation_version_label";
  String APP_VERSION_LABEL = "Application Version";

  String ONFAILURE_PARAM = "cloudformation_wait";
  String ONFAILURE_LABEL = "Action on Failure";
  String WAIT_TIMEOUT_SEC_PARAM = "cloudformation_wait_timeout_sec";
  String WAIT_TIMEOUT_SEC_LABEL = "Timeout (seconds)";
  String WAIT_POLL_INTERVAL_SEC_CONFIG_PARAM = "cloudformation.wait.poll.interval.sec";
  int WAIT_POLL_INTERVAL_SEC_DEFAULT = 20;

  String STATUS_IS_UNKNOWN = "status is unknown";
}
