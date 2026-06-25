<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Registro - Nirami</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/styles.css">
    <script>
        function toggleVendedorFields() {
            var tipo = document.getElementById("tipoUsuario").value;
            var camposVendedor = document.getElementById("camposVendedor");
            var inputCuenta = document.getElementById("cuentaBancaria");
            if (tipo === "VENDEDOR") {
                camposVendedor.style.display = "block";
                inputCuenta.required = true;
            } else {
                camposVendedor.style.display = "none";
                inputCuenta.required = false;
            }
        }

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

        function validarFormulario(event) {
            var p1 = document.getElementById('contrasena').value;
            var p2 = document.getElementById('confirmar_contrasena').value;
            if (p1 !== p2) {
                event.preventDefault();
                alert('Las contraseñas no coinciden. Por favor, verifica.');
            }
        }
    </script>
</head>
<body>
    <header>
        <a href="${pageContext.request.contextPath}/" class="logo">Nirami</a>
    </header>

    <div class="auth-container" style="padding: 2rem 0;">
        <div class="glass-card" style="max-width: 500px;">
            <h2>Crear Cuenta</h2>
            
            <% String error = (String) request.getAttribute("error"); %>
            <% if (error != null) { %>
                <div class="alert alert-error"><%= error %></div>
            <% } %>

            <form action="${pageContext.request.contextPath}/registro" method="post" onsubmit="validarFormulario(event)">
                <div class="form-group">
                    <label for="nombre">Nombre Completo</label>
                    <input type="text" id="nombre" name="nombre" class="form-control" required pattern=".{3,}" title="El nombre debe tener al menos 3 caracteres">
                </div>
                <div class="form-group">
                    <label for="correo">Correo Electrónico</label>
                    <input type="email" id="correo" name="correo" class="form-control" required title="Debe ser un correo electrónico válido con @">
                </div>
                <div class="form-group">
                    <label for="telefono">Teléfono (Opcional)</label>
                    <input type="text" id="telefono" name="telefono" class="form-control" pattern="\d{10}" title="Debe ser exactamente de 10 números" placeholder="Ej. 3001234567">
                </div>
                <div class="form-group">
                    <label for="contrasena">Contraseña</label>
                    <div style="position: relative;">
                        <input type="password" id="contrasena" name="contrasena" class="form-control" required pattern="(?=.*\d)(?=.*[a-z])(?=.*[A-Z]).{8,}" title="Mínimo 8 caracteres, al menos una mayúscula, una minúscula y un número" style="padding-right: 40px;">
                        <button type="button" onclick="togglePassword('contrasena', this)" style="position: absolute; right: 10px; top: 50%; transform: translateY(-50%); background: none; border: none; cursor: pointer; color: #666; padding: 0; display: flex; align-items: center; justify-content: center;">
                            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"></path><circle cx="12" cy="12" r="3"></circle></svg>
                        </button>
                    </div>
                </div>
                <div class="form-group">
                    <label for="confirmar_contrasena">Confirmar Contraseña</label>
                    <div style="position: relative;">
                        <input type="password" id="confirmar_contrasena" name="confirmar_contrasena" class="form-control" required style="padding-right: 40px;">
                        <button type="button" onclick="togglePassword('confirmar_contrasena', this)" style="position: absolute; right: 10px; top: 50%; transform: translateY(-50%); background: none; border: none; cursor: pointer; color: #666; padding: 0; display: flex; align-items: center; justify-content: center;">
                            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"></path><circle cx="12" cy="12" r="3"></circle></svg>
                        </button>
                    </div>
                </div>
                <div class="form-group">
                    <label for="tipoUsuario">¿Cómo usarás Nirami?</label>
                    <select id="tipoUsuario" name="tipoUsuario" class="form-control" onchange="toggleVendedorFields()" required>
                        <option value="CLIENTE">Quiero comprar artesanías</option>
                        <option value="VENDEDOR">Quiero vender mis artesanías</option>
                    </select>
                </div>
                
                <div id="camposVendedor" style="display: none;">
                    <div class="form-group">
                        <label for="cuentaBancaria">Cuenta Bancaria (Para recibir pagos)</label>
                        <input type="text" id="cuentaBancaria" name="cuentaBancaria" class="form-control" placeholder="Ej. 1234567890" pattern="\d{10}" title="Debe ser exactamente de 10 números">
                    </div>
                </div>

                <button type="submit" class="btn btn-primary btn-block">Registrarse</button>
            </form>
            <p class="text-center mt-3">
                ¿Ya tienes cuenta? <a href="${pageContext.request.contextPath}/login" style="color: var(--azul); font-weight: 500;">Inicia sesión</a>
            </p>
        </div>
    </div>
</body>
</html>
