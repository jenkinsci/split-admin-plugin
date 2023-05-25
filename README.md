
# Split Jenkins Plugin

Jenkins plugin that uses Split Admin REST API to allow creating,  updating and deleting Feature flags as part of Test automation and build workflow.
Reference to Split Admin API can be found [here](https://docs.split.io/reference)

## Installation
In Jenkins Plugin Manager page, under "Available" tab, type "Split Admin" to find the plugin and install it.
![MAnage Plugins](images/img3.png?token=AJY6664VJRRJKKIJXVCHYA26T5P24)

## Breaking Change for Version 1.1

The Whitelist terminology is renamed to Targetlist, this will impact existing task.
If you already have Version 1.0 installed, before upgrading to the new version, please do the following:
1. Check all the Jobs that use Split Admin task: AddKeyToWhiteList
2. Write down the details for each "AddKeyToWhiteList" task (workspace, environment, feature flag, treatment and key values)
3. Delete the "AddKeyToWhiteList" task and save the job
4. Upgrade the Split Admin plugin
5. Open the jobs and re-add the task "AddKeyToTargetList" with the previous variables values. 


## How to Use

1. Store the Split Admin API Key in "Split Admin API Key" edit box found in "Manage Jenkins->Configure System" page

![Configure System](images/img1.png?token=AJY6664VJRRJKKIJXVCHYA26T5P24)

2. To use the plugin in Jenkins Jobs, under the Configure section of the job, click "Add build step" and select "Split Admin Task"

![Build Step](images/img2.png?token=AJY666ZLBIL3ERIPQMARFFS6T5QAA)

3. Select a task from the available options, see below for supported tasks, fill out the fields that are required for each task and click "Save".

<p class="callout info">If the Admin API call return code is 429 (throttled), the plugin will sleep for 5 seconds then retry the request, max retried are capped at 10.</p>

4. Plugin use Apache Log4j class for logging. Debug level will log every HTTP request headers and body and response payload returned.
To enable Debug logging, create a new logger in ***Manage Jenkins***->***System Log***->***New Log Recorder***, set the logger class to below, and set the Log level to **ALL**
```
io.split.jenkins.plugins
```

## Supported Tasks

 * **Create Feature flag From YAML File**: This task will read a given YAML file populated with Feature flags containing treatments, individual target keys, dynamic configs and percentages for default rule, see YAML format section below. The plugin will check if Feature flag and Feature flag definitions exists in the given workspace/environment, if not, the subtask is skipped, the fields below are required:
    `Workspace Name`
    `Environment Name`
    `Traffic Type Name`
<p class="callout info">If there are missing fields for Feature flags in YAML file, the plugin will attempt to complete the definitions automatically, see examples in YAML format section.</p>

 * **Create Feature flag**: This task will create a new Feature flag, the fields below are required, the plugin will check if Feature flag does not exist in the given workspace, if it exist, the task is skipped, no errors returned: 
    `Workspace Name`
    `Feature flag Name`
    `Traffic Type Name`
    
 * **Add Feature flag To Environment**: This task will add new definitions for an existing Feature flag to an Environment, the fields below are required, the plugin will check if Feature flag definitions does not exist in the given environment, if it exist, the task is skipped, no errors returned:  
    `Workspace Name`
    `Environment Name`
    `Feature flag Name`
    `Feature flag Definitions`
The Feature flag Definitions field should match the JSON structure accepted by Split Admin API, see the [API reference](https://docs.split.io/reference#create-split-definition-in-environment) for more info.

 * **Add Key To Targetlist**: This task will add new key to a treatment's targetlist section of an existing Feature flag definitions, the fields below are required:
    `Workspace Name`
    `Environment Name`
    `Feature flag Name`
    `Treatment Name`
    `Targetlist Key`
    
 * **Kill Feature flag**: This task will perform "Kill" action on a given Feature flag in an Environment,  the fields below are required:
    `Workspace Name`
    `Environment Name`
    `Feature flag Name`
    
 * **Remove Feature flag Definition**: This task will delete Feature flag definition in a given Environment, the fields below are required, the plugin will check if Feature flag definitions exists in the given environment, if it does not, the task is skipped, no errors returned:
    `Workspace Name`
    `Environment Name`
    `Feature flag Name`
    
 * **Delete Feature flag**: This task will delete a Feature flag that does not have any definitions in any environment, the fields below are required, the plugin will check if Feature flag exists in the given workspace, if it does not, the task is skipped, no errors returned:
    `Workspace Name`
    `Feature flag Name`
    
### YAML format
```yaml
- Feature_flag_name:
     treatment: "treatment_applied_to_this_entry"
     keys: "single_key_or_list"
     percentage: "integer between (0-100)"
```

### Examples

 * Feature flag below has 3 treatments (on/off/unallocated), only "on" treatment contains targetlisted id "key1". For default rule, "on" is set to 80%, "off" is set to 10%, the plugin will assign the last 10% to "unallocated" treatment. 
```yaml
- percentage_less_100_Feature_flag:
    treatment: "on"
    keys: ["key1"]
    percentage: "80"
- percentage_less_100_Feature_flag:
    treatment: "off"
    percentage: "10"
- percentage_less_100_Feature_flag:
    treatment: "unallocated"
```    
 * Feature flag below contains only treatments, no percentages specified for default rule, the plugin will set the first treatment to 100% for default rule.
```yaml
- no_rule_Feature_flag:
    treatment: "on"
- no_rule_Feature_flag:
    treatment: "off"
```    
 * Feature flag below contains only one treatments, no percentages specified for default rule, the plugin will add second treatment with name "SecondTreatment", and set the first treatment to 100% for default rule.
```yaml
- one_treatment_Feature_flag:
    treatment: "one"
```    
 * Feature flag below has two treatments with their dynamic configs, treatment "off" has two keys in targetlist section, default rule has only "on" treatment set to 50%, the plugin will add "off" with 50%.
```yaml
- correct_data_Feature_flag:
    treatment: "on"
    config: "{\\\"desc\\\" : \\\"this applies only to ON treatment\\\"}"
    percentage: "50"
- correct_data_Feature_flag:
    treatment: "off"
    keys: ["key1", "key2"]
    config: "{\\\"desc\\\" : \\\"this applies only to OFF treatment\\\"}"
```
 * Feature flag below only has the name, no other information is given, the plugin will add "on" and "off" treatments, and set default rule to "off" with 100%
```yaml
- no_treatment_Feature_flag:
```
For any questions, comments or issues, please contact [Split Support](mailto:support@split.io)
