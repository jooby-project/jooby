const INSPECTOR_CONFIG_KEY = "inspectorConfig_v1";

const DEFAULT_INSPECTOR_CONFIG = {
    MCP_SERVER_REQUEST_TIMEOUT: {
        label: "Request Timeout",
        description: "Client-side timeout (ms) - Inspector will cancel requests after this time",
        value: 3e5,
        // 5 minutes - increased to support elicitation and other long-running tools
        is_session_item: false
    },
    MCP_REQUEST_TIMEOUT_RESET_ON_PROGRESS: {
        label: "Reset Timeout on Progress",
        description: "Reset timeout on progress notifications",
        value: true,
        is_session_item: false
    },
    MCP_REQUEST_MAX_TOTAL_TIMEOUT: {
        label: "Maximum Total Timeout",
        description: "Maximum total timeout for requests sent to the MCP server (ms) (Use with progress notifications)",
        value: 6e4,
        is_session_item: false
    },
    MCP_PROXY_FULL_ADDRESS: {
        label: "Inspector Proxy Address",
        description: "Set this if you are running the MCP Inspector Proxy on a non-default address. Example: http://10.1.1.22:5577",
        value: "",
        is_session_item: false
    },
    MCP_PROXY_AUTH_TOKEN: {
        label: "Proxy Session Token",
        description: "Session token for authenticating with the MCP Proxy Server (displayed in proxy console on startup)",
        value: "",
        is_session_item: true
    },
    MCP_TASK_TTL: {
        label: "Task TTL",
        description:
            "Default Time-to-Live (TTL) in milliseconds for newly created tasks",
        value: 60000,
        is_session_item: false
    }
};

function patchMcpProxyAddress(storageKey, newAddress) {
    const config = localStorage.getItem(storageKey);
    if (!config) {
        console.warn(`localStorage key "${storageKey}" not found. Setting default values...`);
        const data = Object.assign({}, DEFAULT_INSPECTOR_CONFIG);
        data.MCP_PROXY_FULL_ADDRESS.value = newAddress;
        localStorage.setItem(storageKey, JSON.stringify(data));
        return;
    }

    try {
        const data = JSON.parse(config);

        if (!data.MCP_PROXY_FULL_ADDRESS) {
            console.warn("MCP_PROXY_FULL_ADDRESS not found in stored object");
            return;
        }

        data.MCP_PROXY_FULL_ADDRESS.value = newAddress;
        localStorage.setItem(storageKey, JSON.stringify(data));
    } catch (e) {
        console.error("Failed to parse localStorage JSON:", e);
    }
}

function patch_mcp_inspector_config() {
    localStorage.setItem("lastConnectionType", "direct");
    const url = location.origin + "/mcp-inspector";
    patchMcpProxyAddress(INSPECTOR_CONFIG_KEY, url);
}

patch_mcp_inspector_config();