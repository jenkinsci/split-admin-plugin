f = namespace("/lib/form")
f.section(title: _("Split Admin Plugin")) {
	f.entry(field: "splitAdminApiKey", title: _("Split Admin API Key")) {
    		input(type: "text", class: "setting-input", value: descriptor.splitAdminApiKey, name: "ext_split_admin_api_key")
	}
	f.advanced() {
		f.entry(field: "adminBaseURL", title: _("Split Admin API Base URL")) {
    		input(type: "text", class: "setting-input", value: descriptor.splitAdminBaseURL, name: "ext_admin_base_url")
		}
	}
}