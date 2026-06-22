<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Iniciar Sesión - Nirami</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/styles.css">
    <script>
        function togglePassword(inputId, btn) {
            var input = document.getElementById(inputId);
            var eyeOpen = '<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"></path><circle cx="12" cy="12" r="3"></circle></svg>';
            var eyeClosed = '<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19m-6.72-1.07a3 3 0 1 1-4.24-4.24"></path><line x1="1" y1="1" x2="23" y2="23"></line></svg>';
            
            if (input.type === 'password') {
                input.type = 'text';
                btn.innerHTML = eyeClosed;
            } else {
                input.type = 'password';
                btn.innerHTML = eyeOpen;
            }
        }
    </script>
</head>
<body>
    <header>
        <a href="${pageContext.request.contextPath}/" class="logo">Nirami</a>
    </header>

    <div class="auth-container">
        <div class="glass-card">
            <h2>Bienvenido de Nuevo</h2>
            
            <% String error = (String) request.getAttribute("error"); %>
            <% if (error != null) { %>
                <div class="alert alert-error"><%= error %></div>
            <% } %>

            <% String registrado = request.getParameter("registrado"); %>
            <% if ("true".equals(registrado)) { %>
                <div class="alert alert-success">Registro exitoso. Por favor, inicia sesión.</div>
            <% } %>

            <form action="${pageContext.request.contextPath}/login" method="post">
                <div class="form-group">
                    <label for="correo">Correo Electrónico</label>
                    <input type="email" id="correo" name="correo" class="form-control" required placeholder="correo@ejemplo.com">
                </div>
                <div class="form-group">
                    <label for="contrasena">Contraseña</label>
                    <div style="position: relative;">
                        <input type="password" id="contrasena" name="contrasena" class="form-control" required placeholder="••••••••" style="padding-right: 40px;">
                        <button type="button" onclick="togglePassword('contrasena', this)" style="position: absolute; right: 10px; top: 50%; transform: translateY(-50%); background: none; border: none; cursor: pointer; color: #666; padding: 0; display: flex; align-items: center; justify-content: center;">
                            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"></path><circle cx="12" cy="12" r="3"></circle></svg>
                        </button>
                    </div>
                </div>
                <button type="submit" class="btn btn-primary btn-block">Ingresar</button>
            </form>
            <p class="text-center mt-3">
                ¿No tienes cuenta? <a href="${pageContext.request.contextPath}/registro" style="color: var(--azul); font-weight: 500;">Regístrate aquí</a>
            </p>
        </div>
    </div>
</body>
</html>
