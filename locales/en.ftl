# English translations
app-title = Chat Application
app-subtitle = Real-time messaging with Axum and HTMX

# Login page
login-title = Welcome to Chat
login-prompt = Enter your username to start chatting
username-label = Username
username-placeholder = Choose a username...
join-button = Join Chat
username-required = Username is required
username-too-short = Username must be at least 3 characters
username-too-long = Username must be less than 50 characters

# Chat interface
chat-title = Chat Room
message-placeholder = Type your message...
send-button = Send
online-users = Online Users
no-users-online = No users online
you-indicator = (you)

# Connection status
connected = Connected
disconnected = Disconnected
reconnecting = Reconnecting...

# Typing indicator
is-typing = {$username} is typing...
multiple-typing = {$count} people are typing...

# Language switcher
language-label = Language
change-language = Change Language

# Errors
error-404 = Page Not Found
error-404-message = The page you're looking for doesn't exist.
error-500 = Internal Server Error
error-500-message = Something went wrong on our end.
go-home = Go to Home

# Time formats
just-now = just now
minutes-ago = {$count ->
    [one] 1 minute ago
    *[other] {$count} minutes ago
}
hours-ago = {$count ->
    [one] 1 hour ago
    *[other] {$count} hours ago
}
