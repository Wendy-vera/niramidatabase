<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Compra Exitosa - Nirami</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/styles.css">
    <style>
        .success-card {
            background: var(--blanco-puro);
            padding: 3rem;
            border-radius: 12px;
            box-shadow: 0 4px 15px rgba(0,0,0,0.05);
            text-align: center;
            max-width: 600px;
            margin: 4rem auto;
        }
        .icon {
            font-size: 5rem;
            color: var(--turquesa);
            margin-bottom: 1rem;
        }
    </style>
</head>
<body>
    <header>
        <a href="${pageContext.request.contextPath}/" class="logo">Nirami</a>
    </header>

    <main class="container">
        <div class="success-card">
            <div class="icon">✅</div>
            <h1 style="color: var(--oscuro); margin-bottom: 1rem;">¡Gracias por tu compra!</h1>
            <p style="color: #555; font-size: 1.1rem; margin-bottom: 2rem;">
                Tu orden ha sido procesada exitosamente. Los artesanos han sido notificados y comenzarán a preparar tu pedido pronto.
            </p>
            <a href="${pageContext.request.contextPath}/catalogo" class="btn btn-primary">Seguir Explorando</a>
        </div>
    </main>
</body>
</html>
