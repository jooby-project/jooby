const connectBtnObserver = new MutationObserver(() => {
    const btn = [...document.querySelectorAll('button')]
        .find(el => el.textContent.trim() === 'Connect');
    if (btn) {
        connectBtnObserver.disconnect();

        setTimeout(() => {
            btn.click();
            console.log('Auto-connecting to MCP server...');
        }, 500);
    }
});

connectBtnObserver.observe(document.body, { childList: true, subtree: true });