# Spanish translations
app-title = Aplicación de Chat
app-subtitle = Mensajería en tiempo real con Axum y HTMX

# Login page
login-title = Bienvenido al Chat
login-prompt = Ingresa tu nombre de usuario para comenzar a chatear
username-label = Nombre de usuario
username-placeholder = Elige un nombre de usuario...
join-button = Unirse al Chat
username-required = El nombre de usuario es requerido
username-too-short = El nombre de usuario debe tener al menos 3 caracteres
username-too-long = El nombre de usuario debe tener menos de 50 caracteres

# Chat interface
chat-title = Sala de Chat
message-placeholder = Escribe tu mensaje...
send-button = Enviar
online-users = Usuarios en Línea
no-users-online = No hay usuarios en línea
you-indicator = (tú)

# Connection status
connected = Conectado
disconnected = Desconectado
reconnecting = Reconectando...

# Typing indicator
is-typing = {$username} está escribiendo...
multiple-typing = {$count} personas están escribiendo...

# Language switcher
language-label = Idioma
change-language = Cambiar Idioma

# Errors
error-404 = Página No Encontrada
error-404-message = La página que buscas no existe.
error-500 = Error Interno del Servidor
error-500-message = Algo salió mal de nuestro lado.
go-home = Ir al Inicio

# Time formats
just-now = justo ahora
minutes-ago = {$count ->
    [one] hace 1 minuto
    *[other] hace {$count} minutos
}
hours-ago = {$count ->
    [one] hace 1 hora
    *[other] hace {$count} horas
}
