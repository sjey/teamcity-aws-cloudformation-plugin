<%--
  ~ Copyright 2000-2016 JetBrains s.r.o.
  ~
  ~ Licensed under the Apache License, Version 2.0 (the "License");
  ~ you may not use this file except in compliance with the License.
  ~ You may obtain a copy of the License at
  ~
  ~ http://www.apache.org/licenses/LICENSE-2.0
  ~
  ~ Unless required by applicable law or agreed to in writing, software
  ~ distributed under the License is distributed on an "AS IS" BASIS,
  ~ WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  ~ See the License for the specific language governing permissions and
  ~ limitations under the License.
  --%>

<%@ taglib prefix="l" tagdir="/WEB-INF/tags/layout" %>

<style type="text/css">
    .runnerFormTable span.facultativeNote {
        display: none;
    }

    .runnerFormTable span.facultativeAsterix {
        display: none;
    }
</style>

<%@include file="paramsConstants.jspf"%>

<tr class="groupingTitle">
    <td colspan="2">CloudFormation Stack</td>
</tr>
<tr>
    <th><label for="${stack_name_param}">${stack_name_label}: <l:star/></label></th>
    <td><props:textProperty name="${stack_name_param}" className="longField" maxlength="256"/><a href="http://console.aws.amazon.com/cloudformation" target="_blank">Open CloudFormation Console</a>
        <span class="smallNote">CloudFormation stack name</span><span class="error" id="error_${stack_name_param}"></span>
    </td>
</tr>
<tr>
    <th><label for="${onfailure_param}">${onfailure_label}: <l:star/></label></th>
    <td><props:selectProperty name="${onfailure_param}"
				className="longField" enableFilter="true">
				<props:option value="null">-- Select Action --</props:option>
				<props:option value="DO_NOTHING">Do nothing</props:option>
				<props:option value="ROLLBACK">Rollback</props:option>
				<props:option value="DELETE">Delete</props:option>
			</props:selectProperty> <span class="smallNote">Select desired action on stack creation failure</span><span class="error" id="error_${wait_onfailure}"></span>
    </td>
</tr>

<l:settingsGroup title="Version Location">
    <tr>
        <th><label for="${bucket_name_param}">${bucket_name_label}: <l:star/></label></th>
        <td><props:textProperty name="${bucket_name_param}" className="longField" maxlength="256"/><a href="http://console.aws.amazon.com/s3" target="_blank">Open S3 Console</a>
            <span class="smallNote">Existing S3 bucket name</span><span class="error" id="error_${bucket_name_param}"></span>
        </td>
    </tr>
    <tr>
        <th><label for="${s3_object_key_param}">${s3_object_key_label}: <l:star/></label></th>
        <td><props:textProperty name="${s3_object_key_param}" className="longField" maxlength="256"/>
            <span id="${s3_object_key_param}_mandatory_note" class="smallNote facultativeNote">Unique path inside the bucket</span>
            <span class="error" id="error_${s3_object_key_param}"></span>
        </td>
    </tr>
    <tr>
		<th><label for="${cloudformation_stack_action_param}">${cloudformation_stack_action_label}: <l:star /></label></th>
		<td><props:selectProperty name="${cloudformation_stack_action_param}"
				className="longField" enableFilter="true">
				<props:option value="null">-- Select Action --</props:option>
				<props:option value="Create">Create</props:option>
				<props:option value="Update">Update</props:option>
				<props:option value="Validate">Validate</props:option>
				<props:option value="Delete">Delete</props:option>
			</props:selectProperty> <span class="smallNote">Select desired action</span><span class="error"
			id="error_cfn_action_param}"></span></td>
	</tr>
</l:settingsGroup>

<jsp:include page="editAWSCommonParams.jsp"/>
