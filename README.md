
#jenkins-split-plugin
Jenkins plugin that uses Split Admin REST API to allow creating,  updating and deleting Splits as part of Test automation and build workflow.
Reference to Split Admin API can be found [here](https://docs.split.io/reference)

##Installation##
In Jenkins Plugin Manager page, under "Available" tab, type "Split Jenkins" to find the plugin and install it.

[[[ADD IMAGE HERE]]]

##How to Use##

1. Store the Split Admin API Key in "Split Admin API Key" edit box found in "Manage Jenkins->Configure System" page

[[[ADD SCREENSHOT]]]

2. To use the plugin in Jenkins Jobs, under the Configure section of the job, click "Add build step" and select "Split Admin Task"

[[[ADD SCREENSHOT]]]

3. Select a task from the available options, see below for supported tasks, fill out the fields that are required for each task and click "Save".

<p class="callout info">If the Admin API call return code is 429 (throttled), the plugin will sleep for 5 seconds then retry the request, max retried are capped at 10.</p>

4. Plugin use Apache Log4j class for logging. Debug level will log every HTTP request headers and body and response payload returned.
To enable Debug logging, create a new logger in ***Manage Jenkins***->***System Log***->***New Log Recorder***, set the logger class to below, and set the Log level to **ALL**
```
io.split.jenkins.plugins
```

##Supported Tasks##

 * **Create Split From YAML File**: This task will read a given YAML file populated with Splits containing treatments, whitelisted keys, dynamic configs and percentages for default rule, see YAML format section below. The plugin will check if Split and Split definitions exists in the given workspace/environment, if not, the subtask is skipped, the fields below are required:
    `Workspace Name`
    `Environment Name`
    `Traffic Type Name`
<p class="callout info">If there are missing fields for Splits in YAML file, the plugin will attempt to complete the definitions automatically, see examples in YAML format section.</p>

 * **Create Split**: This task will create a new Split, the fields below are required, the plugin will check if Split does not exist in the given workspace, if it exist, the task is skipped, no errors returned: 
    `Workspace Name`
    `Split Name`
    `Traffic Type Name`
    
 * **Add Split To Environment**: This task will add new definitions for an existing Split to an Environment, the fields below are required, the plugin will check if Split definitions does not exist in the given environment, if it exist, the task is skipped, no errors returned:  
    `Workspace Name`
    `Environment Name`
    `Split Name`
    `Split Definitions`
The Split Definitions field should match the JSON structure accepted by Split Admin API, see the [API reference](https://docs.split.io/reference#create-split-definition-in-environment) for more info.

 * **Add Key To Whitelist**: This task will add new key to a treatment's whitelist section of an existing Split definitions, the fields below are required:
    `Workspace Name`
    `Environment Name`
    `Split Name`
    `Treatment Name`
    `Whitelist Key`
    
 * **Kill Split**: This task will perform "Kill" action on a given Split in an Environment,  the fields below are required:
    `Workspace Name`
    `Environment Name`
    `Split Name`
    
 * **Remove Split Definition**: This task will delete Split definition in a given Environment, the fields below are required, the plugin will check if Split definitions exists in the given environment, if it does not, the task is skipped, no errors returned:
    `Workspace Name`
    `Environment Name`
    `Split Name`
    
 * **Delete Split**: This task will delete a split that does not have any definitions in any environment, the fields below are required, the plugin will check if Split exists in the given workspace, if it does not, the task is skipped, no errors returned:
    `Workspace Name`
    `Split Name`
    
###YAML format###
```yaml
- split_name:
     treatment: "treatment_applied_to_this_entry"
     keys: "single_key_or_list"
     percentage: "integer between (0-100)"
```

###Examples###

 * Split below has 3 treatments (on/off/unallocated), only "on" treatment contains whitelisted id "key1". For default rule, "on" is set to 80%, "off" is set to 10%, the plugin will assign the last 10% to "unallocated" treatment. 
```yaml
- percentage_less_100_split:
    treatment: "on"
    keys: ["key1"]
    percentage: "80"
- percentage_less_100_split:
    treatment: "off"
    percentage: "10"
- percentage_less_100_split:
    treatment: "unallocated"
```    
 * Split below contains only treatments, no percentages specified for default rule, the plugin will set the first treatment to 100% for default rule.
```yaml
- no_rule_split:
    treatment: "on"
- no_rule_split:
    treatment: "off"
```    
 * Split below contains only one treatments, no percentages specified for default rule, the plugin will add second treatment with name "SecondTreatment", and set the first treatment to 100% for default rule.
```yaml
- one_treatment_split:
    treatment: "one"
```    
 * Split below has two treatments with their dynamic configs, treatment "off" has two keys in whitelist section, default rule has only "on" treatment set to 50%, the plugin will add "off" with 50%.
```yaml
- correct_data_split:
    treatment: "on"
    config: "this applies only to ON treatment"
    percentage: "50"
- correct_data_split:
    treatment: "off"
    keys: ["key1", "key2"]
    config: "this applies only to OFF treatment"
```
 * Split below only has the name, no other information is given, the plugin will add "on" and "off" treatments, and set default rule to "off" with 100%
```yaml
- no_treatment_split:
```
