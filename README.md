<h2 id="jenkins-split-plugin" class="header-anchor">jenkins-split-plugin</h2>
<p>Jenkins plugin that uses Split Admin REST API to allow creating,  updating and deleting Splits as part of Test automation and build workflow.<br />
Reference to Split Admin API can be found <a href="https://docs.split.io/reference">here</a></p>
<h3 id="installation" class="header-anchor">Installation</h3>
<p>In Jenkins Plugin Manager page, under "Available" tab, type "Split Jenkins" to find the plugin and install it.</p>
<p>[[[ADD IMAGE HERE]]]</p>
<h3 id="how-to-use" class="header-anchor">How to Use</h3>
<ol>
<li>Store the Split Admin API Key in "Split Admin API Key" edit box found in "Manage Jenkins-&gt;Configure System" page</li>
</ol>
<p>[[[ADD SCREENSHOT]]]</p>
<ol start="2">
<li>To use the plugin in Jenkins Jobs, under the Configure section of the job, click "Add build step" and select "Split Admin Task"</li>
</ol>
<p>[[[ADD SCREENSHOT]]]</p>
<ol start="3">
<li>Select a task from the available options, see below for supported tasks, fill out the fields that are required for each task and click "Save".</li>
</ol>
<p class="callout info">If the Admin API call return code is 429 (throttled), the plugin will sleep for 5 seconds then retry the request, max retried are capped at 10.</p>
<ol start="4">
<li>Plugin use Apache Log4j class for logging. Debug level will log every HTTP request headers and body and response payload returned.<br />
To enable Debug logging, create a new logger in <strong><em>Manage Jenkins</em></strong>-&gt;<strong><em>System Log</em></strong>-&gt;<strong><em>New Log Recorder</em></strong>, set the logger class to below, and set the Log level to <strong>ALL</strong></li>
</ol>
<pre class="prettyprint"><code>io.split.jenkins.plugins</code></pre>
<h3 id="supported-tasks" class="header-anchor">Supported Tasks</h3>
<ul>
<li><strong>Create Split From YAML File</strong>: This task will read a given YAML file populated with Splits containing treatments, whitelisted keys, dynamic configs and percentages for default rule, see YAML format section below. The plugin will check if Split and Split definitions exists in the given workspace/environment, if not, the subtask is skipped, the fields below are required:<br />
<code>Workspace Name</code><br />
<code>Environment Name</code><br />
<code>Traffic Type Name</code></li>
</ul>
<p class="callout info">If there are missing fields for Splits in YAML file, the plugin will attempt to complete the definitions automatically, see examples in YAML format section.</p>
<ul>
<li><p><strong>Create Split</strong>: This task will create a new Split, the fields below are required, the plugin will check if Split does not exist in the given workspace, if it exist, the task is skipped, no errors returned: <br />
<code>Workspace Name</code><br />
<code>Split Name</code><br />
<code>Traffic Type Name</code></p></li>
<li><p><strong>Add Split To Environment</strong>: This task will add new definitions for an existing Split to an Environment, the fields below are required, the plugin will check if Split definitions does not exist in the given environment, if it exist, the task is skipped, no errors returned:  <br />
<code>Workspace Name</code><br />
<code>Environment Name</code><br />
<code>Split Name</code><br />
<code>Split Definitions</code><br />
The Split Definitions field should match the JSON structure accepted by Split Admin API, see the <a href="https://docs.split.io/reference#create-split-definition-in-environment">API reference</a> for more info.</p></li>
<li><p><strong>Add Key To Whitelist</strong>: This task will add new key to a treatment's whitelist section of an existing Split definitions, the fields below are required:<br />
<code>Workspace Name</code><br />
<code>Environment Name</code><br />
<code>Split Name</code><br />
<code>Treatment Name</code><br />
<code>Whitelist Key</code></p></li>
<li><p><strong>Kill Split</strong>: This task will perform "Kill" action on a given Split in an Environment,  the fields below are required:<br />
<code>Workspace Name</code><br />
<code>Environment Name</code><br />
<code>Split Name</code></p></li>
<li><p><strong>Remove Split Definition</strong>: This task will delete Split definition in a given Environment, the fields below are required, the plugin will check if Split definitions exists in the given environment, if it does not, the task is skipped, no errors returned:<br />
<code>Workspace Name</code><br />
<code>Environment Name</code><br />
<code>Split Name</code></p></li>
<li><p><strong>Delete Split</strong>: This task will delete a split that does not have any definitions in any environment, the fields below are required, the plugin will check if Split exists in the given workspace, if it does not, the task is skipped, no errors returned:<br />
<code>Workspace Name</code><br />
<code>Split Name</code></p></li>
</ul>
<h4 id="yaml-format" class="header-anchor">YAML format</h4>
<pre class="prettyprint"><code class="yaml language-yaml">- split_name:
     treatment: "treatment_applied_to_this_entry"
     keys: "single_key_or_list"
     percentage: "integer between (0-100)"</code></pre>
<h4 id="examples" class="header-anchor">Examples</h4>
<ul>
<li>Split below has 3 treatments (on/off/unallocated), only "on" treatment contains whitelisted id "key1". For default rule, "on" is set to 80%, "off" is set to 10%, the plugin will assign the last 10% to "unallocated" treatment. </li>
</ul>
<pre class="prettyprint"><code class="yaml language-yaml">- percentage_less_100_split:
    treatment: "on"
    keys: ["key1"]
    percentage: "80"
- percentage_less_100_split:
    treatment: "off"
    percentage: "10"
- percentage_less_100_split:
    treatment: "unallocated"</code></pre>
<pre class="prettyprint"><code></code></pre>
<ul>
<li>Split below contains only treatments, no percentages specified for default rule, the plugin will set the first treatment to 100% for default rule.</li>
</ul>
<pre class="prettyprint"><code class="yaml language-yaml">- no_rule_split:
    treatment: "on"
- no_rule_split:
    treatment: "off"</code></pre>
<pre class="prettyprint"><code></code></pre>
<ul>
<li>Split below contains only one treatments, no percentages specified for default rule, the plugin will add second treatment with name "SecondTreatment", and set the first treatment to 100% for default rule.</li>
</ul>
<pre class="prettyprint"><code class="yaml language-yaml">- one_treatment_split:
    treatment: "one"</code></pre>
<pre class="prettyprint"><code></code></pre>
<ul>
<li>Split below has two treatments with their dynamic configs, treatment "off" has two keys in whitelist section, default rule has only "on" treatment set to 50%, the plugin will add "off" with 50%.</li>
</ul>
<pre class="prettyprint"><code class="yaml language-yaml">- correct_data_split:
    treatment: "on"
    config: "this applies only to ON treatment"
    percentage: "50"
- correct_data_split:
    treatment: "off"
    keys: ["key1", "key2"]
    config: "this applies only to OFF treatment"</code></pre>
<ul>
<li>Split below only has the name, no other information is given, the plugin will add "on" and "off" treatments, and set default rule to "off" with 100%</li>
</ul>
<pre class="prettyprint"><code class="yaml language-yaml">- no_treatment_split:</code></pre>
