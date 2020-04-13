package io.split.jenkins.plugins.SplitPluginBuilder

f = namespace("/lib/form")
f.section(title: _("Split Admin Plugin")) {
	f.entry(field: "splitAdminApiKey", title: _("Split Admin API Key")) {
    		input(type: "text", class: "setting-input", value: descriptor.splitAdminApiKey, name: "ext_split_admin_api_key")
	}
}