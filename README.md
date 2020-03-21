# jenkins-split-plugin
Jenkins plugin that use Split Admin REST API to allow creating, updating and deleting Splits as part of Test automation and build workflow.
Reference to Split Admin API can be found here: https://docs.split.io/reference

Installations:
In Jenkins Plugin Manager page, under "Available" tab, type "Split Admin API" to find the plugin and install it.

How to Use:

1- Store the the Split Admin API Key in "Split Admin API Key" edit box found in "Manage Jenkine->Configure System" page

2- To use the plugin in Jenkins Jobs, under the Configure section of the job, click "Add build step" and select "Split Admin Task"

3- Select a task from the drop down list, see below for supported tasks, fill out the fields that are required for each task and click "Save".

Note: If the Admin API call return code is 429 (throttled), the plugin will sleep for 5 seconds then retry the request, max retired are capped at 10.   

Supported Tasks:

1- Create Split: This task will create a new Split, the fields below are required, the plugin will check if Split does not exist in the given workspace, if it exist, the task is skipped, no errors returned: 
    Workspace Name
    Split Name
    Traffic Type Name
    
2- Add Split To Environment: This task will add new definitions for an existing Split to an Environment, the fields below are required, the plugin will check if Split definitions does not exist in the given environment, if it exist, the task is skipped, no errors returned:  
    Workspace Name
    Environment Name
    Split Name
    Split Definitions
Note: The Split Definitions field should match the JSON structure accepted by Split Admin API, see the API reference for more info: https://docs.split.io/reference#create-split-definition-in-environment

3- Add Key To Whitelist: This task will add new key to a treatment's whitelist section of an existing Split definitions, the fields below are required:
    Workspace Name
    Environment Name
    Split Name
    Treatment Name
    Whitelist Key
    
4- Kill Split: This task will perform "Kill" action on a given Split in an Environment,  the fields below are required:
    Workspace Name
    Environment Name
    Split Name
    
5- Remove Split Deinition: This task will delete Split definition in a given Environment, the fields below are required, the plugin will check if Split definitions exists in the given environment, if it does not, the task is skipped, no errors returned:
    Workspace Name
    Environment Name
    Split Name
    
6- Delete Split: This task will delete a split that does not have any definitions in any environment, the fields below are required, the plugin will check if Split exists in the given workspace, if it does not, the task is skipped, no errors returned:
    Workspace Name
    Split Name
    
7- Create Split From YAML File: This task will read a given YAML file populated with Splits containing treatments, whitelisted keys, dynamic configs and percentages for default rule, see YAML format below. The plugin will check if Split and Split definitions exists in the given workspace/environment, if not, the subtask is skipped, the fields below are required:
    Workspace Name
    Environment Name
    Traffic Type Name
Note: If there are missing fields for Splits in YAML file, the plugin will attempt to complete the definitions automatically, see examples below. 

YAML format:
- split_name:
     treatment: "treatment_applied_to_this_entry"
     keys: "single_key_or_list"
     percentage: "integer between (0-100)"


Examples

1- Split below has 3 treatments (on/off/unallocated), only "on" treatment contains whitelisted id "key1". For default rule, "on" is set to 80%, "off" is set to 10%, the plugin will assign the last 10% to "unallocated" treatment. 
- percentage_less_100_split:
    treatment: "on"
    keys: ["key1"]
    percentage: "80"
- percentage_less_100_split:
    treatment: "off"
    percentage: "10"
- percentage_less_100_split:
    treatment: "unallocated"
    
2- Split below contains only treatments, no percentages specified for default rule, the plugin will set the first treatment to 100% for default rule.
- no_rule_split:
    treatment: "on"
- no_rule_split:
    treatment: "off"
    
3- Split below contains only one treatments, no percentages specified for default rule, the plugin will add second treatment with name "SecondTreatment", and set the first treatment to 100% for default rule.
- one_treatment_split:
    treatment: "one"
    
4- Split below has two treatments with thier dynamic configs, treatment "off" has two keys in whitelist section, default rule has only "on" treatment set to 50%, the plugin will add "off" with 50%.
- correct_data_split:
    treatment: "on"
    config: "this applies only to ON treatment"
    percentage: "50"
- correct_data_split:
    treatment: "off"
    keys: ["key1", "key2"]
    config: "this applies only to OFF treatment"

5- Split below only has the name, no other information is given, the plugin will add "on" and "off" treatments, and set default rule to "off" with 100%
- no_treatment_split:
