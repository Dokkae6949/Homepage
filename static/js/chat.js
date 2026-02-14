// Auto-scroll messages to bottom
function scrollToBottom() {
    const messages = document.getElementById('messages');
    if (messages) {
        messages.scrollTop = messages.scrollHeight;
    }
}

// Scroll on load and new messages
document.addEventListener('DOMContentLoaded', () => {
    scrollToBottom();
    
    // Observer for new messages
    const messages = document.getElementById('messages');
    if (messages) {
        const observer = new MutationObserver(scrollToBottom);
        observer.observe(messages, { childList: true });
    }
});

// Connection status indicator
document.addEventListener('DOMContentLoaded', () => {
    const statusIndicator = document.getElementById('status-indicator');
    const statusText = document.getElementById('status-text');
    
    if (!statusIndicator || !statusText) return;
    
    // Listen for SSE connection events
    document.body.addEventListener('htmx:sseOpen', () => {
        statusIndicator.style.background = 'var(--success)';
        statusText.textContent = 'Connected';
    });
    
    document.body.addEventListener('htmx:sseClose', () => {
        statusIndicator.style.background = 'var(--error)';
        statusText.textContent = 'Disconnected';
    });
    
    document.body.addEventListener('htmx:sseError', () => {
        statusIndicator.style.background = 'var(--error)';
        statusText.textContent = 'Connection Error';
    });
});

// Mobile sidebar toggle (optional enhancement)
document.addEventListener('DOMContentLoaded', () => {
    const sidebar = document.getElementById('sidebar');
    if (sidebar && window.innerWidth <= 768) {
        // Add a button to toggle sidebar on mobile if needed
        // For now, sidebar is always visible on desktop and can be enhanced later
    }
});
